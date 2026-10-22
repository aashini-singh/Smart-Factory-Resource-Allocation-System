# Smart Factory Resource Allocation System

Console-based Java prototype - Group 4, Object Oriented Techniques using Java (CCSE0355), NIET Greater Noida.
SDG 9 - Industry, Innovation and Infrastructure.

The system stores factory resources (machines, workers, materials), accepts production tasks with a
priority, and allocates resources to tasks by availability and priority. Several tasks can request
resources at the same time (multithreading), and all data is saved to and loaded from text files.

Requirements: JDK 8 or newer. No external libraries.

## How to run

**Windows:** double-click `run.bat` (or run it from Command Prompt inside this folder).

**Linux / macOS:** `./run.sh`

**IntelliJ / Eclipse / VS Code:** open this folder as a project, mark `src` as the source folder and run
`smartfactory.main.FactoryApp`. Keep the working directory as the project folder, so the program finds
the `data` folder (the program prints the data folder path when it starts).

**Command line by hand:**
```
javac -d out -sourcepath src src/smartfactory/main/FactoryApp.java
java -cp out smartfactory.main.FactoryApp
```

## Project structure

```
src/smartfactory/
  model/       Allocatable (interface), Resource (abstract), Machine, Worker, Material,
               Task, Priority, TaskStatus, Allocation
  service/     ResourceManager, AllocationService, TaskRequestThread, ReportGenerator
  exception/   InvalidInputException, ResourceUnavailableException
  util/        FileHandler, InputHelper, Validator
  main/        FactoryApp (menu)
data/          resources.txt, tasks.txt (loaded at start), allocation_log.txt (created automatically)
sample-data/   original copy of the sample data
```

## Menu

```
 1. Add Resource (Machine / Worker / Material)
 2. View Resources
 3. Add Production Task
 4. View Tasks and Pending Queue
 5. Run Priority-Based Allocation
 6. Simulate Simultaneous Requests (Multithreading)
 7. Complete Task / Release Resources
 8. Save Data to File
 9. Load Data from File
10. Allocation Report
 0. Exit
```

## How allocation works

1. Pending tasks wait in a `PriorityQueue`: HIGH first, then MEDIUM, then LOW. Tasks of the same
   priority are served first-come, first-served (`Task.compareTo`).
2. For each task the system looks for a free machine of the required type, a free worker with at least
   the required skill, and a material with enough stock.
3. Allocation is all-or-nothing: if any one resource is missing, the task gets nothing and keeps waiting
   (with the reason shown).
4. Best-fit worker: among suitable workers, the one whose skill is closest to the requirement is chosen,
   so highly skilled workers stay free for tasks that need them (`Worker.getSuitabilityScore`).
5. Machines and workers are returned when a task is completed (option 7). Material is consumed.

## Demo for the review (about 5 minutes)

The sample data has 4 machines (2 of them CNC), 4 workers, 3 materials and 6 tasks.

1. **Option 2** - show the loaded resources (file handling).
2. **Option 4** - show the tasks and the pending queue order (HIGH tasks T1, T4 come first).
3. **Option 6**, type `T1 T5 T6` - three tasks that all need a CNC machine request at the same time in
   three threads. Only 2 CNC machines exist, so 2 succeed and 1 fails. The same machine is never given
   twice, because `allocateTask()` is `synchronized`.
4. **Option 5** - the remaining tasks are allocated in priority order; tasks that cannot get resources
   show the reason ("must wait: no free worker with skill >= 2").
5. **Option 7** - complete a task; its machine and worker are released and the waiting tasks can be
   allocated.
6. **Option 3** - try wrong input (letters for a number, priority "urgent", duplicate ID) to show
   exception handling; the program never crashes.
7. **Option 10** - report with utilisation, tasks grouped by priority (jagged array) and history.
8. **Option 8** - save, then open `data/allocation_log.txt` to show the log.

To reset the demo data, copy the two files from `sample-data` into `data`.

## Where each Java concept is used

| Concept | Unit | Where |
|---|---|---|
| Classes, objects, constructors | 1 | All model classes; objects created in `FactoryApp` and `FileHandler` |
| Encapsulation, access modifiers | 1 | Private fields with getters in `Resource`, `Task`; `protected` state in `Resource` |
| Abstraction | 1, 3 | `abstract class Resource` with abstract `getCategory()`, `getDetails()`, `toFileLine()` |
| Inheritance | 2 | `Machine`, `Worker`, `Material` extend `Resource` |
| Polymorphism (overriding) | 2 | `canFulfil()`, `allocate()`, `release()`, `getSuitabilityScore()`, `isAvailable()`, `toString()` |
| Arrays and jagged array | 2 | `ReportGenerator` - `String[][] tasksByPriority`, rows of different length |
| Collections | 2 | `HashMap` (resource/task lookup), `ArrayList`, `PriorityQueue` (task order), `LinkedHashSet` |
| Interface | 3 | `Allocatable`, implemented by every resource |
| Packages | 3 | `model`, `service`, `exception`, `util`, `main` |
| Exception handling, custom exceptions | 3 | `InvalidInputException`, `ResourceUnavailableException`, try-catch in `FactoryApp`, `InputHelper` |
| Multithreading | 4 | `TaskRequestThread extends Thread`, `start()`, `join()` in `AllocationService` |
| Synchronization | 4 | `synchronized allocateTask()`, `synchronized appendLog()` |
| File handling | 4 | `FileHandler` - `BufferedReader`, `PrintWriter`, try-with-resources, append mode for the log |

## Data file format

`data/resources.txt`
```
category,id,name,type,capacity|skill|quantity,available,allocatedTo
MACHINE,M101,CNC Lathe A,CNC,50,true,-
```

`data/tasks.txt`
```
taskId,name,priority,durationHours,machineType,minSkill,materialType,materialQty,status,assignedMachine,assignedWorker,assignedMaterial
T1,Gear Shaft Machining,HIGH,4,CNC,3,STEEL,100,PENDING,-,-,-
```

`-` means "not needed / none". Lines starting with `#` are ignored. When loading, the program checks that
the two files agree (a busy machine must belong to an allocated task). If a file has an error, the
line number is shown and the current data is not changed.
