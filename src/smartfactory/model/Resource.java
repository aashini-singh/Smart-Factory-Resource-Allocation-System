package smartfactory.model;

import smartfactory.exception.InvalidInputException;
import smartfactory.util.Validator;

/**
 * Common parent of every factory resource: Machine, Worker and Material.
 *
 * Unit 1: abstraction and encapsulation (private fields + getters).
 * Unit 2: inheritance - subclasses reuse the id / name / type handling.
 * Unit 3: abstract class that implements an interface (Allocatable).
 */
public abstract class Resource implements Allocatable {

    public static final String NONE = Validator.NONE;

    private final String id;
    private final String name;
    private final String type;

    // protected: subclasses change these when the resource is allocated / released
    protected boolean available;
    protected String allocatedTo;

    protected Resource(String id, String name, String type) throws InvalidInputException {
        this.id = Validator.requireText(id, "ID").toUpperCase();
        this.name = Validator.requireText(name, "Name");
        this.type = Validator.requireText(type, "Type").toUpperCase();
        this.available = true;
        this.allocatedTo = NONE;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    @Override
    public boolean isAvailable() {
        return available;
    }

    public String getAllocatedTo() {
        return allocatedTo;
    }

    /** MACHINE, WORKER or MATERIAL - decided by the subclass. */
    public abstract String getCategory();

    /** Capacity / skill / quantity text - decided by the subclass. */
    public abstract String getDetails();

    /** One line of the resources file - decided by the subclass. */
    public abstract String toFileLine();

    public String getStatusText() {
        return available ? "Available" : "Busy (" + allocatedTo + ")";
    }

    /**
     * Used to choose the best resource when several are suitable.
     * Lower score = better match. By default every suitable resource is equally good;
     * Worker overrides this (polymorphism) to prefer the least over-qualified worker.
     */
    public int getSuitabilityScore(Task task) {
        return 0;
    }

    /** Used only while loading saved data, to restore whether the resource was busy. */
    public void restoreState(boolean available, String allocatedTo) throws InvalidInputException {
        String holder = (allocatedTo == null || allocatedTo.trim().isEmpty())
                ? NONE : allocatedTo.trim().toUpperCase();
        if (available && !holder.equals(NONE)) {
            throw new InvalidInputException("Resource " + id + " is marked available but allocated to " + holder + ".");
        }
        if (!available && holder.equals(NONE)) {
            throw new InvalidInputException("Resource " + id + " is marked busy but not allocated to any task.");
        }
        this.available = available;
        this.allocatedTo = holder;
    }

    /** Builds the common part of a file line: category,id,name,type,value,available,allocatedTo */
    protected String buildFileLine(int value) {
        return String.join(",", getCategory(), id, name, type,
                String.valueOf(value), String.valueOf(isAvailable()), allocatedTo);
    }

    @Override
    public String toString() {
        return String.format("%-6s %-22s %-11s %-18s %s", id, name, type, getDetails(), getStatusText());
    }
}
