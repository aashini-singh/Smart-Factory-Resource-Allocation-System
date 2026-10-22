package smartfactory.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.PriorityQueue;

import smartfactory.exception.InvalidInputException;
import smartfactory.exception.ResourceUnavailableException;
import smartfactory.model.Allocation;
import smartfactory.model.Machine;
import smartfactory.model.Material;
import smartfactory.model.Resource;
import smartfactory.model.Task;
import smartfactory.model.TaskStatus;
import smartfactory.model.Worker;
import smartfactory.util.FileHandler;

/**
 * Core allocation logic of the system.
 *
 * - Pending tasks wait in a PriorityQueue (HIGH priority served first).
 * - allocateTask() is synchronized (Unit 4) so simultaneous requests from
 *   different threads can never take the same resource.
 * - Allocation is all-or-nothing: a task gets every resource it needs, or none.
 */
public class AllocationService {

    /** Pause used only in the thread simulation, to make threads overlap clearly. */
    public static final int SIMULATED_CHECK_DELAY_MS = 300;

    private final ResourceManager resourceManager;
    private final PriorityQueue<Task> pendingQueue = new PriorityQueue<>();
    private final HashMap<String, Task> tasks = new LinkedHashMap<>();
    private final List<Allocation> history = new ArrayList<>();
    private long nextSequenceNo = 1;

    public AllocationService(ResourceManager resourceManager) {
        this.resourceManager = resourceManager;
    }

    // =====================================================================
    // Tasks
    // =====================================================================

    public synchronized void addTask(Task task) throws InvalidInputException {
        if (tasks.containsKey(task.getTaskId())) {
            throw new InvalidInputException("A task with ID " + task.getTaskId() + " already exists.");
        }
        task.setSequenceNo(nextSequenceNo++);
        tasks.put(task.getTaskId(), task);
        if (task.getStatus() == TaskStatus.PENDING) {
            pendingQueue.add(task);
        }
    }

    public synchronized Task getTask(String taskId) throws InvalidInputException {
        if (taskId == null || taskId.trim().isEmpty()) {
            throw new InvalidInputException("Task ID cannot be empty.");
        }
        Task task = tasks.get(taskId.trim().toUpperCase());
        if (task == null) {
            throw new InvalidInputException("No task found with ID " + taskId.trim().toUpperCase() + ".");
        }
        return task;
    }

    public synchronized Collection<Task> getAllTasks() {
        return Collections.unmodifiableCollection(new ArrayList<>(tasks.values()));
    }

    public synchronized List<Task> getTasksByStatus(TaskStatus status) {
        List<Task> list = new ArrayList<>();
        for (Task t : tasks.values()) {
            if (t.getStatus() == status) {
                list.add(t);
            }
        }
        return list;
    }

    /** Pending tasks in the exact order they will be served. */
    public synchronized List<Task> getPendingInOrder() {
        // Iterating a PriorityQueue directly is NOT sorted, so we poll from a copy.
        PriorityQueue<Task> copy = new PriorityQueue<>(pendingQueue);
        List<Task> ordered = new ArrayList<>();
        while (!copy.isEmpty()) {
            ordered.add(copy.poll());
        }
        return ordered;
    }

    public synchronized List<Allocation> getHistory() {
        return new ArrayList<>(history);
    }

    /** Warnings for a new task whose needs can never be met with the current resources. */
    public List<String> checkFeasibility(Task task) {
        List<String> warnings = new ArrayList<>();
        if (task.needsMachine()
                && !resourceManager.getTypes(Machine.CATEGORY).contains(task.getRequiredMachineType())) {
            warnings.add("No machine of type " + task.getRequiredMachineType() + " exists in the factory yet.");
        }
        if (task.needsWorker()) {
            int maxSkill = 0;
            for (Resource r : resourceManager.getByCategory(Worker.CATEGORY)) {
                maxSkill = Math.max(maxSkill, ((Worker) r).getSkillLevel());
            }
            if (maxSkill < task.getRequiredSkillLevel()) {
                warnings.add("No worker has skill level " + task.getRequiredSkillLevel() + " or above.");
            }
        }
        if (task.needsMaterial()) {
            int bestStock = -1;
            for (Resource r : resourceManager.getByCategory(Material.CATEGORY)) {
                if (r.getType().equals(task.getRequiredMaterialType())) {
                    bestStock = Math.max(bestStock, ((Material) r).getQuantity());
                }
            }
            if (bestStock < 0) {
                warnings.add("No material of type " + task.getRequiredMaterialType() + " exists in stock.");
            } else if (bestStock < task.getRequiredMaterialQty()) {
                warnings.add("Not enough " + task.getRequiredMaterialType() + " in stock right now (max "
                        + bestStock + " units in one batch).");
            }
        }
        return warnings;
    }

    // =====================================================================
    // Allocation
    // =====================================================================

    public Allocation allocateTask(Task task) throws ResourceUnavailableException {
        return allocateTask(task, 0);
    }

    /**
     * Checks availability and books all resources for one task.
     * synchronized: only one thread at a time can be inside this method,
     * so "check if free" and "mark as busy" happen together without interference.
     */
    public synchronized Allocation allocateTask(Task task, int checkDelayMs) throws ResourceUnavailableException {
        if (task.getStatus() != TaskStatus.PENDING) {
            throw new ResourceUnavailableException("Task " + task.getTaskId() + " is already " + task.getStatus() + ".");
        }

        // Step 1: find the best free resource of each kind the task needs
        Resource machine = null;
        Resource worker = null;
        Resource material = null;
        List<String> missing = new ArrayList<>();

        if (task.needsMachine()) {
            machine = resourceManager.findBestResource(task, Machine.CATEGORY);
            if (machine == null) {
                missing.add("no free " + task.getRequiredMachineType() + " machine");
            }
        }
        if (task.needsWorker()) {
            worker = resourceManager.findBestResource(task, Worker.CATEGORY);
            if (worker == null) {
                missing.add("no free worker with skill >= " + task.getRequiredSkillLevel());
            }
        }
        if (task.needsMaterial()) {
            material = resourceManager.findBestResource(task, Material.CATEGORY);
            if (material == null) {
                missing.add("not enough " + task.getRequiredMaterialType() + " in stock");
            }
        }

        // Only for the thread demo: a pause between "check" and "book".
        // Without synchronized, another thread could grab the same machine during this pause.
        if (checkDelayMs > 0) {
            try {
                Thread.sleep(checkDelayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        if (!missing.isEmpty()) {
            throw new ResourceUnavailableException("Task " + task.getTaskId() + " must wait: " + String.join(", ", missing) + ".");
        }

        // Step 2: everything is free -> book all of them together (all-or-nothing)
        if (machine != null) {
            machine.allocate(task);
        }
        if (worker != null) {
            worker.allocate(task);
        }
        if (material != null) {
            material.allocate(task);
        }
        task.markAllocated(idOf(machine), idOf(worker), idOf(material));

        Allocation allocation = new Allocation(task, Thread.currentThread().getName());
        history.add(allocation);
        FileHandler.appendLog("ALLOCATED " + task.getTaskId() + " (" + task.getPriority() + ") -> "
                + allocation.getResourceSummary() + " | by " + Thread.currentThread().getName());
        return allocation;
    }

    private static String idOf(Resource resource) {
        return resource == null ? Resource.NONE : resource.getId();
    }

    /**
     * Serves all pending tasks in priority order. Tasks that cannot be served
     * now stay in the queue for the next run. Returns how many were allocated.
     */
    public synchronized int runAllocation() {
        List<Task> stillWaiting = new ArrayList<>();
        int allocated = 0;
        int step = 1;

        while (!pendingQueue.isEmpty()) {
            Task task = pendingQueue.poll(); // highest priority first
            try {
                Allocation a = allocateTask(task);
                allocated++;
                System.out.printf("  %d. [ALLOCATED] %-5s %-7s -> %s%n", step, task.getTaskId(), task.getPriority(), a.getResourceSummary());
            } catch (ResourceUnavailableException e) {
                stillWaiting.add(task);
                System.out.printf("  %d. [WAITING]   %-5s %-7s -> %s%n", step, task.getTaskId(), task.getPriority(), e.getMessage());
            }
            step++;
        }
        pendingQueue.addAll(stillWaiting);
        return allocated;
    }

    /**
     * Unit 4 demo: the selected pending tasks request resources at the same time,
     * each in its own thread. Tasks that fail go back to the pending queue.
     */
    public void simulateSimultaneousRequests(List<String> taskIds) throws InvalidInputException {
        List<Task> selected = new ArrayList<>();
        synchronized (this) {
            for (String id : taskIds) {
                Task task = getTask(id);
                if (task.getStatus() != TaskStatus.PENDING) {
                    throw new InvalidInputException("Task " + task.getTaskId() + " is not PENDING (it is " + task.getStatus() + ").");
                }
                if (!selected.contains(task)) {
                    selected.add(task);
                }
            }
            if (selected.size() < 2) {
                throw new InvalidInputException("Select at least 2 different pending tasks.");
            }
            pendingQueue.removeAll(selected); // threads will handle these tasks now
        }

        System.out.println("Starting " + selected.size() + " threads at the same time...");
        System.out.println("(allocateTask() is synchronized, so only one thread can check and book resources at a time)");

        List<TaskRequestThread> threads = new ArrayList<>();
        for (Task task : selected) {
            threads.add(new TaskRequestThread(this, task));
        }
        for (TaskRequestThread t : threads) {
            t.start(); // all threads start almost together
        }
        for (TaskRequestThread t : threads) {
            try {
                t.join(); // main thread waits until every request is finished
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        int success = 0;
        synchronized (this) {
            for (TaskRequestThread t : threads) {
                if (t.isSuccessful()) {
                    success++;
                } else if (t.getTask().getStatus() == TaskStatus.PENDING) {
                    pendingQueue.add(t.getTask()); // failed -> back to waiting queue
                }
            }
        }
        System.out.println();
        System.out.println("  Result: " + success + " of " + threads.size() + " simultaneous requests allocated, "
                + (threads.size() - success) + " moved back to the pending queue.");
    }

    // =====================================================================
    // Completion
    // =====================================================================

    /** Marks a task completed and frees its machine and worker. Returns the IDs released. */
    public synchronized List<String> completeTask(String taskId) throws InvalidInputException {
        Task task = getTask(taskId);
        if (task.getStatus() != TaskStatus.ALLOCATED) {
            throw new InvalidInputException("Only ALLOCATED tasks can be completed. Task "
                    + task.getTaskId() + " is " + task.getStatus() + ".");
        }
        List<String> freed = new ArrayList<>();
        for (String resourceId : task.getAssignedResourceIds()) {
            Resource r = resourceManager.getResource(resourceId);
            r.release(task); // polymorphism: machine/worker become free, material stays consumed
            if (!(r instanceof Material)) {
                freed.add(r.getId());
            }
        }
        task.markCompleted();
        FileHandler.appendLog("COMPLETED " + task.getTaskId() + " | released " + (freed.isEmpty() ? "-" : String.join(", ", freed)));
        return freed;
    }

    // =====================================================================
    // Loading saved data
    // =====================================================================

    /**
     * Replaces all resources and tasks with data loaded from files.
     * Everything is checked first, so on any error the current data is not touched.
     */
    public synchronized void replaceAllData(List<Resource> newResources, List<Task> newTasks) throws InvalidInputException {
        HashMap<String, Resource> resMap = new LinkedHashMap<>();
        for (Resource r : newResources) {
            if (resMap.containsKey(r.getId())) {
                throw new InvalidInputException("Duplicate resource ID " + r.getId() + " in file.");
            }
            resMap.put(r.getId(), r);
        }
        HashMap<String, Task> taskMap = new LinkedHashMap<>();
        for (Task t : newTasks) {
            if (taskMap.containsKey(t.getTaskId())) {
                throw new InvalidInputException("Duplicate task ID " + t.getTaskId() + " in file.");
            }
            taskMap.put(t.getTaskId(), t);
        }

        // Every ALLOCATED task must point to resources that are really held by it
        for (Task t : newTasks) {
            if (t.getStatus() != TaskStatus.ALLOCATED) {
                continue;
            }
            checkHolder(resMap, t, t.getAssignedMachineId(), Machine.CATEGORY);
            checkHolder(resMap, t, t.getAssignedWorkerId(), Worker.CATEGORY);
            if (t.needsMaterial()) {
                Resource m = resMap.get(t.getAssignedMaterialId());
                if (!(m instanceof Material)) {
                    throw new InvalidInputException("Task " + t.getTaskId() + " uses material "
                            + t.getAssignedMaterialId() + " which does not exist.");
                }
            }
        }
        // Every busy machine/worker must belong to an ALLOCATED task that uses it
        for (Resource r : newResources) {
            if (r instanceof Material || r.isAvailable()) {
                continue;
            }
            Task holder = taskMap.get(r.getAllocatedTo());
            boolean ok = holder != null && holder.getStatus() == TaskStatus.ALLOCATED
                    && (r.getId().equals(holder.getAssignedMachineId()) || r.getId().equals(holder.getAssignedWorkerId()));
            if (!ok) {
                throw new InvalidInputException("Resource " + r.getId() + " is busy with task "
                        + r.getAllocatedTo() + ", but that task is not allocated to it.");
            }
        }

        // All checks passed -> replace current data
        resourceManager.replaceAll(newResources);
        tasks.clear();
        pendingQueue.clear();
        history.clear();
        nextSequenceNo = 1;
        for (Task t : newTasks) {
            addTask(t);
        }
    }

    private static void checkHolder(HashMap<String, Resource> resMap, Task task, String resourceId, String category)
            throws InvalidInputException {
        if (resourceId.equals(Resource.NONE)) {
            return;
        }
        Resource r = resMap.get(resourceId);
        if (r == null || !r.getCategory().equals(category)) {
            throw new InvalidInputException("Task " + task.getTaskId() + " uses " + category.toLowerCase()
                    + " " + resourceId + " which does not exist.");
        }
        if (r.isAvailable() || !r.getAllocatedTo().equals(task.getTaskId())) {
            throw new InvalidInputException("Task " + task.getTaskId() + " uses " + resourceId
                    + ", but the resources file does not show it as busy with this task.");
        }
    }
}
