package smartfactory.main;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import smartfactory.exception.InvalidInputException;
import smartfactory.model.Machine;
import smartfactory.model.Material;
import smartfactory.model.Priority;
import smartfactory.model.Resource;
import smartfactory.model.Task;
import smartfactory.model.TaskStatus;
import smartfactory.model.Worker;
import smartfactory.service.AllocationService;
import smartfactory.service.ReportGenerator;
import smartfactory.service.ResourceManager;
import smartfactory.util.FileHandler;
import smartfactory.util.InputHelper;

/**
 * Smart Factory Resource Allocation System - console application.
 * Entry point: shows the menu and calls the service classes.
 */
public class FactoryApp {

    private static final String LINE = repeat('-', 79);
    private static final String WIDE_LINE = repeat('-', 108);

    private final ResourceManager resourceManager = new ResourceManager();
    private final AllocationService allocationService = new AllocationService(resourceManager);
    private final ReportGenerator reportGenerator = new ReportGenerator(resourceManager, allocationService);
    private final InputHelper input = new InputHelper(new Scanner(System.in));

    private static String repeat(char c, int times) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < times; i++) {
            sb.append(c);
        }
        return sb.toString();
    }

    public static void main(String[] args) {
        new FactoryApp().start();
    }

    private void start() {
        System.out.println("===============================================================");
        System.out.println("        SMART FACTORY RESOURCE ALLOCATION SYSTEM");
        System.out.println("   Group 4 - Object Oriented Techniques using Java (CCSE0355)");
        System.out.println("===============================================================");
        System.out.println("Data folder: " + FileHandler.getDataFolderPath());
        loadData(false);

        boolean running = true;
        while (running) {
            printMenu();
            int choice = input.readInt("Enter your choice: ", 0, 10);
            System.out.println();
            try {
                switch (choice) {
                    case 1:  addResource();          break;
                    case 2:  viewResources();        break;
                    case 3:  addTask();              break;
                    case 4:  viewTasks();            break;
                    case 5:  runAllocation();        break;
                    case 6:  simulateSimultaneous(); break;
                    case 7:  completeTask();         break;
                    case 8:  saveData();             break;
                    case 9:  loadData(true);         break;
                    case 10: reportGenerator.printReport(); break;
                    case 0:  running = !confirmExit(); break;
                    default: System.out.println("Invalid choice.");
                }
            } catch (RuntimeException e) {
                // safety net: an unexpected error should never close the program
                System.out.println("Unexpected error: " + e);
            }
        }
        System.out.println("Thank you for using the Smart Factory Resource Allocation System.");
    }

    private void printMenu() {
        System.out.println();
        System.out.println("======================= MAIN MENU =======================");
        System.out.println(" 1. Add Resource (Machine / Worker / Material)");
        System.out.println(" 2. View Resources");
        System.out.println(" 3. Add Production Task");
        System.out.println(" 4. View Tasks and Pending Queue");
        System.out.println(" 5. Run Priority-Based Allocation");
        System.out.println(" 6. Simulate Simultaneous Requests (Multithreading)");
        System.out.println(" 7. Complete Task / Release Resources");
        System.out.println(" 8. Save Data to File");
        System.out.println(" 9. Load Data from File");
        System.out.println("10. Allocation Report");
        System.out.println(" 0. Exit");
        System.out.println("=========================================================");
    }

    // =====================================================================
    // 1. Add resource
    // =====================================================================
    private void addResource() {
        System.out.println("--- Add Resource ---");
        System.out.println("1. Machine   2. Worker   3. Material");
        int kind = input.readInt("Choose resource type: ", 1, 3);

        String id = input.readText("Resource ID (e.g. M105 / W05 / MT04): ");
        String name = input.readText("Name: ");
        try {
            Resource resource;
            if (kind == 1) {
                String type = input.readText("Machine type (e.g. CNC, WELDING, ASSEMBLY): ");
                int capacity = input.readInt("Capacity (units per hour, 1-100000): ", 1, 100000);
                resource = new Machine(id, name, type, capacity);
            } else if (kind == 2) {
                String role = input.readText("Role (e.g. OPERATOR, TECHNICIAN, WELDER): ");
                int skill = input.readInt("Skill level (1-5): ", 1, Worker.MAX_SKILL);
                resource = new Worker(id, name, role, skill);
            } else {
                String type = input.readText("Material type (e.g. STEEL, COPPER): ");
                int qty = input.readInt("Quantity in stock (0-1000000): ", 0, 1000000);
                resource = new Material(id, name, type, qty);
            }
            resourceManager.addResource(resource);
            System.out.println("Resource added: " + resource.getId() + " (" + resource.getCategory() + ")");
        } catch (InvalidInputException e) {
            System.out.println("Error: " + e.getMessage() + " Resource was not added.");
        }
    }

    // =====================================================================
    // 2. View resources
    // =====================================================================
    private void viewResources() {
        if (resourceManager.isEmpty()) {
            System.out.println("No resources yet. Use option 1 or load data (option 9).");
            return;
        }
        printResourceGroup("MACHINES", Machine.CATEGORY);
        printResourceGroup("WORKERS", Worker.CATEGORY);
        printResourceGroup("MATERIALS", Material.CATEGORY);
    }

    private void printResourceGroup(String title, String category) {
        List<Resource> list = resourceManager.getByCategory(category);
        System.out.println(title + " (" + list.size() + ")");
        System.out.println(LINE);
        System.out.printf("%-6s %-22s %-11s %-18s %s%n", "ID", "Name", "Type/Role", "Details", "Status");
        System.out.println(LINE);
        for (Resource r : list) {
            System.out.println(r); // toString() of Machine / Worker / Material
        }
        System.out.println();
    }

    // =====================================================================
    // 3. Add task
    // =====================================================================
    private void addTask() {
        System.out.println("--- Add Production Task ---");
        System.out.println("Machine types available : " + typesOrNone(Machine.CATEGORY));
        System.out.println("Material types available: " + typesOrNone(Material.CATEGORY));

        String id = input.readText("Task ID (e.g. T7): ");
        String name = input.readText("Task name: ");
        Priority priority = readPriority();
        int duration = input.readInt("Duration in hours (1-100): ", 1, 100);
        String machineType = input.readLine("Machine type needed (press Enter or '-' if none): ");
        int skill = input.readInt("Minimum worker skill 1-5 (0 if no worker needed): ", 0, Worker.MAX_SKILL);
        String materialType = input.readLine("Material type needed (press Enter or '-' if none): ");
        int qty = 0;
        if (!materialType.isEmpty() && !materialType.equals("-")) {
            qty = input.readInt("Material quantity needed (1-1000000): ", 1, 1000000);
        }

        try {
            Task task = new Task(id, name, priority, duration, machineType, skill, materialType, qty);
            allocationService.addTask(task);
            System.out.println("Task added to the pending queue: " + task.getTaskId()
                    + " (" + task.getPriority() + ", needs " + task.getRequirementsText() + ")");
            for (String warning : allocationService.checkFeasibility(task)) {
                System.out.println("  Warning: " + warning + " The task will wait until this is available.");
            }
        } catch (InvalidInputException e) {
            System.out.println("Error: " + e.getMessage() + " Task was not added.");
        }
    }

    private Priority readPriority() {
        while (true) {
            try {
                return Priority.fromString(input.readText("Priority (H = HIGH, M = MEDIUM, L = LOW): "));
            } catch (InvalidInputException e) {
                System.out.println("  " + e.getMessage());
            }
        }
    }

    private String typesOrNone(String category) {
        return resourceManager.getTypes(category).isEmpty() ? "(none)" : String.join(", ", resourceManager.getTypes(category));
    }

    // =====================================================================
    // 4. View tasks
    // =====================================================================
    private void viewTasks() {
        if (allocationService.getAllTasks().isEmpty()) {
            System.out.println("No tasks yet. Use option 3 or load data (option 9).");
            return;
        }
        System.out.println("ALL TASKS");
        System.out.println(WIDE_LINE);
        System.out.printf("%-5s %-22s %-7s %4s  %-35s %-10s %s%n", "ID", "Name", "Prio", "Time",
                "Needs (machine | worker | material)", "Status", "Assigned");
        System.out.println(WIDE_LINE);
        for (Task t : allocationService.getAllTasks()) {
            System.out.println(t);
        }

        System.out.println();
        List<Task> queue = allocationService.getPendingInOrder();
        System.out.println("PENDING QUEUE (order in which tasks will be served): "
                + (queue.isEmpty() ? "empty" : ""));
        int pos = 1;
        for (Task t : queue) {
            System.out.printf("  %d. %-5s %-7s %s%n", pos++, t.getTaskId(), t.getPriority(), t.getName());
        }
    }

    // =====================================================================
    // 5. Run allocation
    // =====================================================================
    private void runAllocation() {
        if (allocationService.getPendingInOrder().isEmpty()) {
            System.out.println("No pending tasks to allocate.");
            return;
        }
        System.out.println("Running priority-based allocation (HIGH -> MEDIUM -> LOW)...");
        int allocated = allocationService.runAllocation();
        int waiting = allocationService.getPendingInOrder().size();
        System.out.println();
        System.out.println("Allocated: " + allocated + "   Still waiting: " + waiting);
        if (waiting > 0) {
            System.out.println("Tip: complete running tasks (option 7) to free resources, then run allocation again.");
        }
    }

    // =====================================================================
    // 6. Multithreading demo
    // =====================================================================
    private void simulateSimultaneous() {
        List<Task> pending = allocationService.getPendingInOrder();
        if (pending.size() < 2) {
            System.out.println("At least 2 pending tasks are needed for this demo. Add tasks (option 3) or load data (option 9).");
            return;
        }
        System.out.println("--- Simultaneous Requests (each task runs in its own thread) ---");
        System.out.println("Pending tasks:");
        for (Task t : pending) {
            System.out.printf("  %-5s %-7s needs %s%n", t.getTaskId(), t.getPriority(), t.getRequirementsText());
        }
        System.out.println("Tip: choose tasks that need the SAME machine type to see synchronization at work.");
        String line = input.readText("Enter task IDs separated by space or comma (e.g. T1 T5 T6): ");

        List<String> ids = new ArrayList<>();
        for (String part : Arrays.asList(line.split("[,\\s]+"))) {
            if (!part.isEmpty()) {
                ids.add(part);
            }
        }
        System.out.println();
        try {
            allocationService.simulateSimultaneousRequests(ids);
        } catch (InvalidInputException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    // =====================================================================
    // 7. Complete task
    // =====================================================================
    private void completeTask() {
        List<Task> running = allocationService.getTasksByStatus(TaskStatus.ALLOCATED);
        if (running.isEmpty()) {
            System.out.println("No allocated tasks to complete. Run allocation first (option 5).");
            return;
        }
        System.out.println("Allocated (running) tasks:");
        for (Task t : running) {
            System.out.printf("  %-5s %-22s using %s%n", t.getTaskId(), t.getName(), t.getAssignedText());
        }
        String id = input.readText("Enter task ID to mark as completed: ");
        try {
            List<String> freed = allocationService.completeTask(id);
            System.out.println("Task " + id.trim().toUpperCase() + " completed. Released: "
                    + (freed.isEmpty() ? "nothing (material is consumed)" : String.join(", ", freed)));
            if (!allocationService.getPendingInOrder().isEmpty()
                    && input.readYesNo("There are waiting tasks. Run allocation now?")) {
                System.out.println();
                runAllocation();
            }
        } catch (InvalidInputException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    // =====================================================================
    // 8 / 9. Save and load
    // =====================================================================
    private void saveData() {
        try {
            FileHandler.saveResources(resourceManager.getAllResources());
            FileHandler.saveTasks(allocationService.getAllTasks());
            System.out.println("Saved " + resourceManager.getAllResources().size() + " resources and "
                    + allocationService.getAllTasks().size() + " tasks to " + FileHandler.getDataFolderPath());
        } catch (IOException e) {
            System.out.println("Error: could not save data - " + e.getMessage());
        }
    }

    private void loadData(boolean askFirst) {
        if (askFirst && !(resourceManager.isEmpty() && allocationService.getAllTasks().isEmpty())
                && !input.readYesNo("This will replace the current data with the saved files. Continue?")) {
            System.out.println("Load cancelled.");
            return;
        }
        try {
            List<Resource> resources = FileHandler.loadResources();
            List<Task> tasks = FileHandler.loadTasks();
            allocationService.replaceAllData(resources, tasks);
            System.out.println("Loaded " + resources.size() + " resources and " + tasks.size() + " tasks from file.");
        } catch (FileNotFoundException e) {
            System.out.println("No saved data found (" + e.getMessage() + "). Starting with empty data.");
        } catch (IOException e) {
            System.out.println("Error: could not read data files - " + e.getMessage());
        } catch (InvalidInputException e) {
            System.out.println("Error in data file: " + e.getMessage());
            System.out.println("Current data was not changed.");
        }
    }

    // =====================================================================
    // 0. Exit
    // =====================================================================
    /** Returns true if the program should really exit. */
    private boolean confirmExit() {
        if (input.readYesNo("Save data before exiting?")) {
            saveData();
        }
        return true;
    }
}
