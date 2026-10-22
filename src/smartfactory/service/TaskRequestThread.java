package smartfactory.service;

import smartfactory.exception.ResourceUnavailableException;
import smartfactory.model.Allocation;
import smartfactory.model.Task;

/**
 * One production task asking for resources in its own thread.
 *
 * Unit 4 concept: multithreading. Several of these threads run at the same
 * time; AllocationService.allocateTask() is synchronized, so two threads can
 * never book the same machine or worker.
 */
public class TaskRequestThread extends Thread {

    private final AllocationService service;
    private final Task task;
    private volatile boolean successful;

    public TaskRequestThread(AllocationService service, Task task) {
        super("Thread-" + task.getTaskId());
        this.service = service;
        this.task = task;
    }

    @Override
    public void run() {
        System.out.println("  [" + getName() + "] requesting resources for " + task.getTaskId()
                + " (" + task.getPriority() + ", needs " + task.getRequirementsText() + ")");
        try {
            Allocation allocation = service.allocateTask(task, AllocationService.SIMULATED_CHECK_DELAY_MS);
            successful = true;
            System.out.println("  [" + getName() + "] SUCCESS -> " + allocation.getResourceSummary());
        } catch (ResourceUnavailableException e) {
            successful = false;
            System.out.println("  [" + getName() + "] FAILED  -> " + e.getMessage());
        }
    }

    public Task getTask() {
        return task;
    }

    public boolean isSuccessful() {
        return successful;
    }
}
