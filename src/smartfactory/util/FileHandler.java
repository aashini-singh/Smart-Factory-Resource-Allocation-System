package smartfactory.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import smartfactory.exception.InvalidInputException;
import smartfactory.model.Machine;
import smartfactory.model.Material;
import smartfactory.model.Priority;
import smartfactory.model.Resource;
import smartfactory.model.Task;
import smartfactory.model.TaskStatus;
import smartfactory.model.Worker;

/**
 * Saves and loads resources and tasks as simple comma-separated text files,
 * and keeps a log of every allocation.
 *
 * Unit 4 concept: file handling (FileReader/BufferedReader, FileWriter/PrintWriter,
 * try-with-resources).
 */
public final class FileHandler {

    public static final String DATA_DIR = "data";
    public static final String RESOURCES_FILE = DATA_DIR + File.separator + "resources.txt";
    public static final String TASKS_FILE = DATA_DIR + File.separator + "tasks.txt";
    public static final String LOG_FILE = DATA_DIR + File.separator + "allocation_log.txt";

    private static final DateTimeFormatter LOG_TIME = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
    private static boolean logWarningShown = false;

    private FileHandler() {
        // utility class - no objects
    }

    public static String getDataFolderPath() {
        return new File(DATA_DIR).getAbsolutePath();
    }

    private static void ensureDataDir() throws IOException {
        File dir = new File(DATA_DIR);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("Could not create folder " + dir.getAbsolutePath());
        }
    }

    // ---------------------------------------------------------------- save

    public static void saveResources(Collection<Resource> resources) throws IOException {
        ensureDataDir();
        try (PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(RESOURCES_FILE)))) {
            out.println("# category,id,name,type,capacity|skill|quantity,available,allocatedTo");
            for (Resource r : resources) {
                out.println(r.toFileLine());
            }
            if (out.checkError()) {
                throw new IOException("Error while writing " + RESOURCES_FILE);
            }
        }
    }

    public static void saveTasks(Collection<Task> tasks) throws IOException {
        ensureDataDir();
        try (PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(TASKS_FILE)))) {
            out.println("# taskId,name,priority,durationHours,machineType,minSkill,materialType,materialQty,"
                    + "status,assignedMachine,assignedWorker,assignedMaterial");
            for (Task t : tasks) {
                out.println(t.toFileLine());
            }
            if (out.checkError()) {
                throw new IOException("Error while writing " + TASKS_FILE);
            }
        }
    }

    // ---------------------------------------------------------------- load

    public static List<Resource> loadResources() throws IOException, InvalidInputException {
        List<Resource> list = new ArrayList<>();
        for (String[] row : readRows(RESOURCES_FILE, 7)) {
            list.add(parseResource(row));
        }
        return list;
    }

    public static List<Task> loadTasks() throws IOException, InvalidInputException {
        List<Task> list = new ArrayList<>();
        for (String[] row : readRows(TASKS_FILE, 12)) {
            list.add(parseTask(row));
        }
        return list;
    }

    /**
     * Reads a data file, skipping blank lines and lines starting with '#'.
     * The last element of every returned row is the line number (for error messages).
     */
    private static List<String[]> readRows(String fileName, int expectedFields) throws IOException, InvalidInputException {
        List<String[]> rows = new ArrayList<>();
        try (BufferedReader in = new BufferedReader(new FileReader(fileName))) {
            String line;
            int lineNo = 0;
            while ((line = in.readLine()) != null) {
                lineNo++;
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                String[] parts = line.split(",", -1);
                if (parts.length != expectedFields) {
                    throw new InvalidInputException(fileName + " line " + lineNo + ": expected "
                            + expectedFields + " values but found " + parts.length + ".");
                }
                String[] row = new String[expectedFields + 1];
                for (int i = 0; i < expectedFields; i++) {
                    row[i] = parts[i].trim();
                }
                row[expectedFields] = String.valueOf(lineNo);
                rows.add(row);
            }
        }
        return rows;
    }

    private static Resource parseResource(String[] p) throws InvalidInputException {
        String where = RESOURCES_FILE + " line " + p[7] + ": ";
        try {
            String category = p[0].toUpperCase();
            int value = Integer.parseInt(p[4]);
            boolean available = parseBoolean(p[5]);
            Resource r;
            switch (category) {
                case Machine.CATEGORY:
                    r = new Machine(p[1], p[2], p[3], value);
                    break;
                case Worker.CATEGORY:
                    r = new Worker(p[1], p[2], p[3], value);
                    break;
                case Material.CATEGORY:
                    r = new Material(p[1], p[2], p[3], value);
                    break;
                default:
                    throw new InvalidInputException("unknown category '" + p[0] + "'.");
            }
            r.restoreState(available, p[6]);
            return r;
        } catch (NumberFormatException e) {
            throw new InvalidInputException(where + "'" + p[4] + "' is not a whole number.");
        } catch (InvalidInputException e) {
            throw new InvalidInputException(where + e.getMessage());
        }
    }

    private static Task parseTask(String[] p) throws InvalidInputException {
        String where = TASKS_FILE + " line " + p[12] + ": ";
        try {
            Task t = new Task(p[0], p[1], Priority.fromString(p[2]), Integer.parseInt(p[3]),
                    p[4], Integer.parseInt(p[5]), p[6], Integer.parseInt(p[7]));
            t.restoreState(TaskStatus.fromString(p[8]), p[9], p[10], p[11]);
            return t;
        } catch (NumberFormatException e) {
            throw new InvalidInputException(where + "duration, skill and quantity must be whole numbers.");
        } catch (InvalidInputException e) {
            throw new InvalidInputException(where + e.getMessage());
        }
    }

    private static boolean parseBoolean(String text) throws InvalidInputException {
        if (text.equalsIgnoreCase("true")) {
            return true;
        }
        if (text.equalsIgnoreCase("false")) {
            return false;
        }
        throw new InvalidInputException("available must be true or false, found '" + text + "'.");
    }

    // ---------------------------------------------------------------- log

    /**
     * Adds one line to the allocation log. synchronized because several
     * threads may write to the log at the same time.
     */
    public static synchronized void appendLog(String message) {
        try {
            ensureDataDir();
            try (PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(LOG_FILE, true)))) {
                out.println(LocalDateTime.now().format(LOG_TIME) + " | " + message);
            }
        } catch (IOException e) {
            if (!logWarningShown) {
                System.out.println("  (Warning: could not write log file: " + e.getMessage() + ")");
                logWarningShown = true;
            }
        }
    }
}
