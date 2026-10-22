package smartfactory.model;

import smartfactory.exception.InvalidInputException;
import smartfactory.exception.ResourceUnavailableException;
import smartfactory.util.Validator;

/**
 * A factory worker with a role (OPERATOR, TECHNICIAN ...) and a skill level 1-5.
 * A worker can work on only one task at a time.
 */
public class Worker extends Resource {

    public static final String CATEGORY = "WORKER";
    public static final int MAX_SKILL = 5;

    private final int skillLevel;

    public Worker(String id, String name, String role, int skillLevel) throws InvalidInputException {
        super(id, name, role);
        this.skillLevel = Validator.requireRange(skillLevel, 1, MAX_SKILL, "Skill level");
    }

    public int getSkillLevel() {
        return skillLevel;
    }

    @Override
    public String getCategory() {
        return CATEGORY;
    }

    @Override
    public String getDetails() {
        return "Skill " + skillLevel + "/" + MAX_SKILL;
    }

    /** Free worker whose skill is at least the skill the task needs. */
    @Override
    public boolean canFulfil(Task task) {
        return task.needsWorker()
                && available
                && skillLevel >= task.getRequiredSkillLevel();
    }

    /**
     * Polymorphism: a worker with skill exactly equal to the requirement gets score 0
     * (best). Highly skilled workers get a higher score, so they stay free for
     * tasks that really need them.
     */
    @Override
    public int getSuitabilityScore(Task task) {
        return skillLevel - task.getRequiredSkillLevel();
    }

    @Override
    public void allocate(Task task) throws ResourceUnavailableException {
        if (!canFulfil(task)) {
            throw new ResourceUnavailableException("Worker " + getId() + " cannot be allocated to task " + task.getTaskId() + ".");
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
        return buildFileLine(skillLevel);
    }
}
