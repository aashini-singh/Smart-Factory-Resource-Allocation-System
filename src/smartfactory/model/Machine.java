package smartfactory.model;

import smartfactory.exception.InvalidInputException;
import smartfactory.exception.ResourceUnavailableException;
import smartfactory.util.Validator;

/**
 * A machine such as a CNC lathe or a welding robot.
 * A machine can work on only one task at a time.
 */
public class Machine extends Resource {

    public static final String CATEGORY = "MACHINE";

    private final int capacity; // units produced per hour

    public Machine(String id, String name, String machineType, int capacity) throws InvalidInputException {
        super(id, name, machineType);
        this.capacity = Validator.requireRange(capacity, 1, 100000, "Capacity");
    }

    public int getCapacity() {
        return capacity;
    }

    @Override
    public String getCategory() {
        return CATEGORY;
    }

    @Override
    public String getDetails() {
        return "Capacity " + capacity + "/hr";
    }

    /** Free machine of the same type that the task asks for. */
    @Override
    public boolean canFulfil(Task task) {
        return task.needsMachine()
                && available
                && getType().equals(task.getRequiredMachineType());
    }

    @Override
    public void allocate(Task task) throws ResourceUnavailableException {
        if (!canFulfil(task)) {
            throw new ResourceUnavailableException("Machine " + getId() + " cannot be allocated to task " + task.getTaskId() + ".");
        }
        available = false;
        allocatedTo = task.getTaskId();
    }

    @Override
    public void release(Task task) {
        if (!available && allocatedTo.equals(task.getTaskId())) {
            available = true;
            allocatedTo = NONE;
        }
    }

    @Override
    public String toFileLine() {
        return buildFileLine(capacity);
    }
}
