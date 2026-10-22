package smartfactory.model;

import smartfactory.exception.InvalidInputException;
import smartfactory.exception.ResourceUnavailableException;
import smartfactory.util.Validator;

/**
 * Raw material kept in stock (steel, copper ...).
 * Unlike machines and workers, material is CONSUMED: allocating it reduces
 * the stock, and it is not returned when the task finishes.
 */
public class Material extends Resource {

    public static final String CATEGORY = "MATERIAL";

    private int quantity; // units in stock

    public Material(String id, String name, String materialType, int quantity) throws InvalidInputException {
        super(id, name, materialType);
        this.quantity = Validator.requireRange(quantity, 0, 1000000, "Quantity");
    }

    public int getQuantity() {
        return quantity;
    }

    @Override
    public String getCategory() {
        return CATEGORY;
    }

    @Override
    public String getDetails() {
        return "Stock " + quantity + " units";
    }

    /** Overridden: material is "available" while there is stock left. */
    @Override
    public boolean isAvailable() {
        return quantity > 0;
    }

    @Override
    public String getStatusText() {
        return quantity > 0 ? "In stock" : "Out of stock";
    }

    /** Same material type and enough stock for the task. */
    @Override
    public boolean canFulfil(Task task) {
        return task.needsMaterial()
                && getType().equals(task.getRequiredMaterialType())
                && quantity >= task.getRequiredMaterialQty();
    }

    @Override
    public void allocate(Task task) throws ResourceUnavailableException {
        if (!canFulfil(task)) {
            throw new ResourceUnavailableException("Material " + getId() + " does not have enough stock for task " + task.getTaskId() + ".");
        }
        quantity -= task.getRequiredMaterialQty();
    }

    /** Material is used up by the task, so nothing is returned to stock. */
    @Override
    public void release(Task task) {
        // nothing to do - consumed material does not come back
    }

    /** Material state is only its quantity, which is already loaded in the constructor. */
    @Override
    public void restoreState(boolean available, String allocatedTo) {
        // nothing to restore
    }

    @Override
    public String toFileLine() {
        return buildFileLine(quantity);
    }
}
