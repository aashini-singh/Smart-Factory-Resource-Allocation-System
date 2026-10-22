package smartfactory.model;

import java.util.ArrayList;
import java.util.List;

import smartfactory.exception.InvalidInputException;
import smartfactory.util.Validator;

/**
 * A production task that needs some resources: a machine of a given type,
 * a worker with a minimum skill and/or some quantity of a material.
 *
 * Implements Comparable so that a PriorityQueue serves HIGH priority first,
 * and tasks of the same priority in the order they were added (first come, first served).
 */
public class Task implements Comparable<Task> {

    private static final String NONE = Validator.NONE;

    private final String taskId;
    private final String name;
    private final Priority priority;
    private final int durationHours;

    private final String requiredMachineType;   // "-" = no machine needed
    private final int requiredSkillLevel;       // 0   = no worker needed
    private final String requiredMaterialType;  // "-" = no material needed
    private final int requiredMaterialQty;

    private TaskStatus status = TaskStatus.PENDING;
    private String assignedMachineId = NONE;
    private String assignedWorkerId = NONE;
    private String assignedMaterialId = NONE;

    private long sequenceNo; // order in which the task entered the system

    public Task(String taskId, String name, Priority priority, int durationHours,
                String requiredMachineType, int requiredSkillLevel,
                String requiredMaterialType, int requiredMaterialQty) throws InvalidInputException {

        this.taskId = Validator.requireText(taskId, "Task ID").toUpperCase();
        this.name = Validator.requireText(name, "Task name");
        if (priority == null) {
            throw new InvalidInputException("Priority is required.");
        }
        this.priority = priority;
        this.durationHours = Validator.requireRange(durationHours, 1, 100, "Duration (hours)");
        this.requiredMachineType = Validator.optionalText(requiredMachineType, "Machine type").toUpperCase();
        this.requiredSkillLevel = Validator.requireRange(requiredSkillLevel, 0, Worker.MAX_SKILL, "Worker skill level");
        this.requiredMaterialType = Validator.optionalText(requiredMaterialType, "Material type").toUpperCase();
        this.requiredMaterialQty = needsMaterial()
                ? Validator.requireRange(requiredMaterialQty, 1, 1000000, "Material quantity")
                : 0;

        if (!needsMachine() && !needsWorker() && !needsMaterial()) {
            throw new InvalidInputException("A task must need at least one resource (machine, worker or material).");
        }
    }

    // ---------------- requirement checks ----------------

    public final boolean needsMachine() {
        return !requiredMachineType.equals(NONE);
    }

    public final boolean needsWorker() {
        return requiredSkillLevel > 0;
    }

    public final boolean needsMaterial() {
        return !requiredMaterialType.equals(NONE);
    }

    // ---------------- state changes ----------------

    public void markAllocated(String machineId, String workerId, String materialId) {
        this.assignedMachineId = machineId;
        this.assignedWorkerId = workerId;
        this.assignedMaterialId = materialId;
        this.status = TaskStatus.ALLOCATED;
    }

    public void markCompleted() {
        this.status = TaskStatus.COMPLETED;
    }

    /** Used only while loading saved data from the tasks file. */
    public void restoreState(TaskStatus savedStatus, String machineId, String workerId, String materialId)
            throws InvalidInputException {
        String m = normalise(machineId);
        String w = normalise(workerId);
        String mat = normalise(materialId);

        if (savedStatus == TaskStatus.PENDING && !(m.equals(NONE) && w.equals(NONE) && mat.equals(NONE))) {
            throw new InvalidInputException("Task " + taskId + " is PENDING but has assigned resources.");
        }
        if (savedStatus != TaskStatus.PENDING
                && (needsMachine() == m.equals(NONE)
                    || needsWorker() == w.equals(NONE)
                    || needsMaterial() == mat.equals(NONE))) {
            throw new InvalidInputException("Task " + taskId + " is " + savedStatus
                    + " but its assigned resources do not match its requirements.");
        }
        this.status = savedStatus;
        this.assignedMachineId = m;
        this.assignedWorkerId = w;
        this.assignedMaterialId = mat;
    }

    private static String normalise(String id) {
        return (id == null || id.trim().isEmpty()) ? NONE : id.trim().toUpperCase();
    }

    // ---------------- getters ----------------

    public String getTaskId() {
        return taskId;
    }

    public String getName() {
        return name;
    }

    public Priority getPriority() {
        return priority;
    }

    public int getDurationHours() {
        return durationHours;
    }

    public String getRequiredMachineType() {
        return requiredMachineType;
    }

    public int getRequiredSkillLevel() {
        return requiredSkillLevel;
    }

    public String getRequiredMaterialType() {
        return requiredMaterialType;
    }

    public int getRequiredMaterialQty() {
        return requiredMaterialQty;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public String getAssignedMachineId() {
        return assignedMachineId;
    }

    public String getAssignedWorkerId() {
        return assignedWorkerId;
    }

    public String getAssignedMaterialId() {
        return assignedMaterialId;
    }

    public long getSequenceNo() {
        return sequenceNo;
    }

    public void setSequenceNo(long sequenceNo) {
        this.sequenceNo = sequenceNo;
    }

    /** IDs of all resources given to this task (used when releasing them). */
    public List<String> getAssignedResourceIds() {
        List<String> ids = new ArrayList<>();
        if (!assignedMachineId.equals(NONE)) {
            ids.add(assignedMachineId);
        }
        if (!assignedWorkerId.equals(NONE)) {
            ids.add(assignedWorkerId);
        }
        if (!assignedMaterialId.equals(NONE)) {
            ids.add(assignedMaterialId);
        }
        return ids;
    }

    // ---------------- display / file ----------------

    /** Always "machine | worker skill | material", with "-" for anything not needed. */
    public String getRequirementsText() {
        String machine = needsMachine() ? requiredMachineType : NONE;
        String worker = needsWorker() ? "skill>=" + requiredSkillLevel : NONE;
        String material = needsMaterial() ? requiredMaterialQty + " " + requiredMaterialType : NONE;
        return machine + " | " + worker + " | " + material;
    }

    public String getAssignedText() {
        List<String> parts = new ArrayList<>();
        if (!assignedMachineId.equals(NONE)) {
            parts.add(assignedMachineId);
        }
        if (!assignedWorkerId.equals(NONE)) {
            parts.add(assignedWorkerId);
        }
        if (!assignedMaterialId.equals(NONE)) {
            parts.add(assignedMaterialId + " x" + requiredMaterialQty);
        }
        return parts.isEmpty() ? NONE : String.join(", ", parts);
    }

    public String toFileLine() {
        return String.join(",", taskId, name, priority.name(), String.valueOf(durationHours),
                requiredMachineType, String.valueOf(requiredSkillLevel),
                requiredMaterialType, String.valueOf(requiredMaterialQty),
                status.name(), assignedMachineId, assignedWorkerId, assignedMaterialId);
    }

    /** Higher priority first; for equal priority the task added earlier comes first. */
    @Override
    public int compareTo(Task other) {
        int byPriority = Integer.compare(other.priority.getLevel(), this.priority.getLevel());
        if (byPriority != 0) {
            return byPriority;
        }
        return Long.compare(this.sequenceNo, other.sequenceNo);
    }

    @Override
    public String toString() {
        return String.format("%-5s %-22s %-7s %3dh  %-35s %-10s %s",
                taskId, name, priority, durationHours, getRequirementsText(), status, getAssignedText());
    }
}
