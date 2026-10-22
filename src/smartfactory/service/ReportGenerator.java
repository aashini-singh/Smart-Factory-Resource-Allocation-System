package smartfactory.service;

import java.util.List;

import smartfactory.model.Allocation;
import smartfactory.model.Machine;
import smartfactory.model.Material;
import smartfactory.model.Priority;
import smartfactory.model.Resource;
import smartfactory.model.Task;
import smartfactory.model.TaskStatus;
import smartfactory.model.Worker;

/**
 * Prints a summary of resource usage and task progress.
 */
public class ReportGenerator {

    private final ResourceManager resourceManager;
    private final AllocationService allocationService;

    public ReportGenerator(ResourceManager resourceManager, AllocationService allocationService) {
        this.resourceManager = resourceManager;
        this.allocationService = allocationService;
    }

    public void printReport() {
        System.out.println();
        System.out.println("================== ALLOCATION REPORT ==================");

        // ---- Resource utilisation ----
        System.out.println("Resource utilisation:");
        printUtilisation("Machines", Machine.CATEGORY);
        printUtilisation("Workers ", Worker.CATEGORY);
        System.out.println("  Material stock:");
        List<Resource> materials = resourceManager.getByCategory(Material.CATEGORY);
        if (materials.isEmpty()) {
            System.out.println("    (no materials)");
        }
        for (Resource r : materials) {
            Material m = (Material) r;
            System.out.printf("    %-6s %-22s %6d units  (%s)%n", m.getId(), m.getName(), m.getQuantity(), m.getStatusText());
        }

        // ---- Task progress ----
        int pending = allocationService.getTasksByStatus(TaskStatus.PENDING).size();
        int allocated = allocationService.getTasksByStatus(TaskStatus.ALLOCATED).size();
        int completed = allocationService.getTasksByStatus(TaskStatus.COMPLETED).size();
        System.out.println();
        System.out.println("Task progress:");
        System.out.println("  Pending: " + pending + "   Allocated: " + allocated + "   Completed: " + completed);

        // ---- Tasks grouped by priority using a JAGGED ARRAY (Unit 2) ----
        // Each row is one priority level; rows have different lengths
        // because each priority has a different number of tasks.
        Priority[] levels = Priority.values();
        String[][] tasksByPriority = new String[levels.length][];
        for (int i = 0; i < levels.length; i++) {
            int count = 0;
            for (Task t : allocationService.getAllTasks()) {
                if (t.getPriority() == levels[i]) {
                    count++;
                }
            }
            tasksByPriority[i] = new String[count]; // row length decided at runtime
            int j = 0;
            for (Task t : allocationService.getAllTasks()) {
                if (t.getPriority() == levels[i]) {
                    tasksByPriority[i][j++] = t.getTaskId() + "(" + t.getStatus().name().charAt(0) + ")";
                }
            }
        }
        System.out.println();
        System.out.println("Tasks by priority (jagged array, P=Pending A=Allocated C=Completed):");
        for (int i = 0; i < tasksByPriority.length; i++) {
            String row = tasksByPriority[i].length == 0 ? "-" : String.join(", ", tasksByPriority[i]);
            System.out.printf("  %-6s [%d]: %s%n", levels[i], tasksByPriority[i].length, row);
        }

        // ---- Allocation history ----
        List<Allocation> history = allocationService.getHistory();
        System.out.println();
        System.out.println("Allocation history (this session): " + history.size() + " allocation(s)");
        for (Allocation a : history) {
            System.out.println("  " + a);
        }
        System.out.println("=======================================================");
    }

    private void printUtilisation(String label, String category) {
        int total = resourceManager.count(category);
        int free = resourceManager.countAvailable(category);
        int busy = total - free;
        int percent = total == 0 ? 0 : (busy * 100) / total;
        System.out.printf("  %s: %d total, %d busy, %d free  -> utilisation %d%%%n", label, total, busy, free, percent);
    }
}
