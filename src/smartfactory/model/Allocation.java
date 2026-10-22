package smartfactory.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Record of one successful allocation: which task got which resources,
 * when, and which thread did the allocation.
 */
public class Allocation {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

    private final String taskId;
    private final Priority priority;
    private final String machineId;
    private final String workerId;
    private final String materialId;
    private final int materialQty;
    private final String time;
    private final String handledBy;

    /** Must be created after task.markAllocated(...) so the assigned IDs are filled. */
    public Allocation(Task task, String handledBy) {
        this.taskId = task.getTaskId();
        this.priority = task.getPriority();
        this.machineId = task.getAssignedMachineId();
        this.workerId = task.getAssignedWorkerId();
        this.materialId = task.getAssignedMaterialId();
        this.materialQty = task.getRequiredMaterialQty();
        this.time = LocalDateTime.now().format(TIME_FORMAT);
        this.handledBy = handledBy;
    }

    public String getTaskId() {
        return taskId;
    }

    public Priority getPriority() {
        return priority;
    }

    public String getTime() {
        return time;
    }

    /** Example: "Machine M101, Worker W01, 100 x MT01" */
    public String getResourceSummary() {
        List<String> parts = new ArrayList<>();
        if (!machineId.equals(Resource.NONE)) {
            parts.add("Machine " + machineId);
        }
        if (!workerId.equals(Resource.NONE)) {
            parts.add("Worker " + workerId);
        }
        if (!materialId.equals(Resource.NONE)) {
            parts.add(materialQty + " x " + materialId);
        }
        return String.join(", ", parts);
    }

    @Override
    public String toString() {
        return time + " | " + taskId + " (" + priority + ") -> " + getResourceSummary() + " | by " + handledBy;
    }
}
