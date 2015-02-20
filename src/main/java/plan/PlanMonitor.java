package plan;

import org.btrplace.model.Model;
import org.btrplace.plan.Dependency;
import org.btrplace.plan.ReconfigurationPlan;
import org.btrplace.plan.ReconfigurationPlanMonitor;
import org.btrplace.plan.event.Action;
import org.btrplace.plan.event.MigrateVM;

import java.util.*;

/**
 * Created by vkherbac on 18/02/15.
 */
public class PlanMonitor implements ReconfigurationPlanMonitor {

    private ReconfigurationPlan plan;

    private Model curModel;

    private final Map<Action, Set<Dependency>> pre;

    private final Map<Action, Dependency> dependencies;

    private final Object lock;

    private int nbCommitted;

    /**
     * Make a new monitor.
     *
     * @param p the plan to execute
     */
    public PlanMonitor(ReconfigurationPlan p) {
        this.plan = p;

        pre = new HashMap<>();
        dependencies = new HashMap<>();
        lock = new Object();
        reset();
    }

    private void reset() {
        synchronized (lock) {
            curModel = plan.getOrigin().clone();
            pre.clear();
            nbCommitted = 0;
            for (Action a : plan.getActions()) {
                Set<Action> deps = plan.getDirectDependencies(a);
                deps.addAll(addMigrationsDependencies(a));
                if (deps.isEmpty()) {
                    this.dependencies.put(a, new Dependency(a, Collections.<Action>emptySet()));
                } else {
                    Dependency dep = new Dependency(a, deps);
                    this.dependencies.put(a, dep);
                    for (Action x : dep.getDependencies()) {
                        Set<Dependency> pres = pre.get(x);
                        if (pres == null) {
                            pres = new HashSet<>();
                            pre.put(x, pres);
                        }
                        pres.add(dep);
                    }
                }
            }
        }
    }

    private Collection<Action> addMigrationsDependencies(Action action) {

        List<Action> actions = new ArrayList<>();

        if (action instanceof MigrateVM) {
            for (Action a : plan.getActions()) {
                if (a instanceof MigrateVM) {
                    if (a.getEnd() <= action.getStart()) {
                        actions.add(a);
                    }
                }
            }
        }

        return actions;
    }

    @Override
    public Model getCurrentModel() {
        return curModel;
    }

    @Override
    public Set<Action> commit(Action a) {
        Set<Action> s = new HashSet<>();
        synchronized (lock) {
            boolean ret = a.apply(curModel);
            if (!ret) {
                return null;
            }
            nbCommitted++;
            //Browse all its dependencies for the action
            Set<Dependency> deps = pre.get(a);
            if (deps != null) {
                for (Dependency dep : deps) {
                    Set<Action> actions = dep.getDependencies();
                    actions.remove(a);
                    if (actions.isEmpty()) {
                        Action x = dep.getAction();
                        s.add(x);
                    }
                }
            }
        }
        return s;
    }

    @Override
    public int getNbCommitted() {
        synchronized (lock) {
            return nbCommitted;
        }
    }

    @Override
    public boolean isBlocked(Action a) {
        synchronized (lock) {
            return !dependencies.get(a).getDependencies().isEmpty();
        }
    }

    @Override
    public ReconfigurationPlan getReconfigurationPlan() {
        return plan;
    }
}