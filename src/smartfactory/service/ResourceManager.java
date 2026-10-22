package smartfactory.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import smartfactory.exception.InvalidInputException;
import smartfactory.model.Resource;
import smartfactory.model.Task;

/**
 * Stores all factory resources and finds suitable ones for a task.
 *
 * Unit 2 concept: Java collections. A HashMap gives quick lookup by resource ID
 * (LinkedHashMap is a HashMap that also remembers insertion order for display).
 */
public class ResourceManager {

    private final HashMap<String, Resource> resources = new LinkedHashMap<>();

    public void addResource(Resource resource) throws InvalidInputException {
        if (resources.containsKey(resource.getId())) {
            throw new InvalidInputException("A resource with ID " + resource.getId() + " already exists.");
        }
        resources.put(resource.getId(), resource);
    }

    public Resource getResource(String id) throws InvalidInputException {
        if (id == null || id.trim().isEmpty()) {
            throw new InvalidInputException("Resource ID cannot be empty.");
        }
        Resource resource = resources.get(id.trim().toUpperCase());
        if (resource == null) {
            throw new InvalidInputException("No resource found with ID " + id.trim().toUpperCase() + ".");
        }
        return resource;
    }

    public Collection<Resource> getAllResources() {
        return Collections.unmodifiableCollection(resources.values());
    }

    public List<Resource> getByCategory(String category) {
        List<Resource> list = new ArrayList<>();
        for (Resource r : resources.values()) {
            if (r.getCategory().equals(category)) {
                list.add(r);
            }
        }
        return list;
    }

    /** Distinct types in a category, e.g. all machine types: CNC, WELDING ... */
    public Set<String> getTypes(String category) {
        Set<String> types = new LinkedHashSet<>();
        for (Resource r : getByCategory(category)) {
            types.add(r.getType());
        }
        return types;
    }

    /**
     * Finds the best free resource of a category for a task.
     * Polymorphism: canFulfil() and getSuitabilityScore() run the Machine,
     * Worker or Material version depending on the actual object.
     * Returns null if nothing suitable is free.
     */
    public Resource findBestResource(Task task, String category) {
        Resource best = null;
        for (Resource r : resources.values()) {
            if (r.getCategory().equals(category) && r.canFulfil(task)) {
                if (best == null || r.getSuitabilityScore(task) < best.getSuitabilityScore(task)) {
                    best = r;
                }
            }
        }
        return best;
    }

    public int count(String category) {
        return getByCategory(category).size();
    }

    public int countAvailable(String category) {
        int free = 0;
        for (Resource r : getByCategory(category)) {
            if (r.isAvailable()) {
                free++;
            }
        }
        return free;
    }

    public boolean isEmpty() {
        return resources.isEmpty();
    }

    /** Replaces all resources (used when loading from file). Checks duplicates first. */
    public void replaceAll(List<Resource> newResources) throws InvalidInputException {
        HashMap<String, Resource> temp = new LinkedHashMap<>();
        for (Resource r : newResources) {
            if (temp.containsKey(r.getId())) {
                throw new InvalidInputException("Duplicate resource ID " + r.getId() + ".");
            }
            temp.put(r.getId(), r);
        }
        resources.clear();
        resources.putAll(temp);
    }
}
