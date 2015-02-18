package plan;

import action.ActionLauncher;
import org.btrplace.plan.ReconfigurationPlan;
import org.btrplace.plan.event.Action;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Created by vkherbac on 18/02/15.
 */
public class PlanScheduler {

    public static final class Lock {}
    private final Object lock = new Lock();

    private Map<Action, ActionLauncher> actionsMap;
    private PlanMonitor rpm;

    public PlanScheduler(ReconfigurationPlan plan, Map<Action, ActionLauncher> actionsMap) {
        this.actionsMap = actionsMap;
        rpm = new PlanMonitor(plan);
    }

    public void start() {

        Map<Future<Integer>, Action> actionStates = new HashMap<>();

        // Start actions
        int nbCommitted = 0;

        // Get all feasible actions
        Set<Action> feasible = new HashSet<>();
        for (Action a : actionsMap.keySet()) {
            if (!rpm.isBlocked(a)) {
                feasible.add(a);
            }
        }

        // Loop while there are remaining actions
        while (rpm.getNbCommitted() < actionsMap.size()) {

            Set<Action> newFeasible = new HashSet<>();

            // Start all feasible actions
            if (!feasible.isEmpty()) {

                ExecutorService service = Executors.newFixedThreadPool(feasible.size());
                for (Action a : feasible) {
                    ActionLauncher l = actionsMap.get(a);
                    l.setSync((Lock) lock);
                    actionStates.put(service.submit(l), a);
                }
                service.shutdown();
            }

            // Wait for any termination
            synchronized (lock) {
                try {
                    lock.wait(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            // Check for finished actions and prepare the new unlocked actions
            for (Iterator<Future<Integer>> it = actionStates.keySet().iterator(); it.hasNext(); ) {
                Future<Integer> f = it.next();

                if (f.isDone()) {
                    Set<Action> s = rpm.commit(actionStates.get(f));
                    // TODO: retry ?
                    if (s == null) {
                        break;
                    }
                    newFeasible.addAll(s);
                    it.remove();
                }
            }

            feasible = newFeasible;
        }
    }
}
