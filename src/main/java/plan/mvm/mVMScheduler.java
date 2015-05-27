package plan.mvm;

import action.ActionLauncher;
import org.btrplace.plan.ReconfigurationPlan;
import org.btrplace.plan.event.Action;
import plan.Scheduler;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Created by vkherbac on 18/02/15.
 */
public class mVMScheduler implements Scheduler {

    
    private Map<Action, ActionLauncher> actionsMap;
    private PlanMonitor rpm;
    private String scriptsDir = null;

    public mVMScheduler(Map<Action, ActionLauncher> actionsMap, ReconfigurationPlan plan, String scriptsDir) {
        this(actionsMap, plan);
        this.scriptsDir = scriptsDir;
    }
    
    public mVMScheduler(Map<Action, ActionLauncher> actionsMap, ReconfigurationPlan plan) {
        this.actionsMap = actionsMap;
        rpm = new PlanMonitor(plan);
    }

    public Map<Action, ActionDuration> start() {

        Map<Future<Date>, Action> actionStates = new HashMap<>();
        Map<Action, ActionDuration> durations = new HashMap<>();

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
                    if (scriptsDir!=null) l.setScriptsDir(scriptsDir);
                    actionStates.put(service.submit(l), a);
                    durations.put(a, new ActionDuration(new Date(), null));
                }
                service.shutdown();
            }

            // Wait for any termination
            synchronized (lock) {
                try {
                    lock.wait(500);
                } catch (InterruptedException e) {
                    System.err.println("Interrupted Exception on main thread");
                    System.exit(1);
                }
            }

            // Check for finished actions and prepare the new unlocked actions
            for (Iterator<Future<Date>> it = actionStates.keySet().iterator(); it.hasNext(); ) {
                Future<Date> f = it.next();

                if (f.isDone()) {
                    // Get the returned Date
                    try {
                        durations.get(actionStates.get(f)).setEnd(f.get());
                    } catch (InterruptedException e) {
                        System.err.println("Interrupted Exception during action: " +
                                actionsMap.get((actionStates.get(f))).toString());
                        System.exit(1);
                    } catch (ExecutionException e) {
                        System.err.println("Execution Exception for action: " +
                                actionsMap.get((actionStates.get(f))).toString());
                        System.exit(1);
                    }
                    Set<Action> s = rpm.commit(actionStates.get(f));
                    if (s == null) {
                        System.err.println("Unable to commit action: " +
                                actionsMap.get((actionStates.get(f))).toString());
                        System.exit(1);
                    }
                    newFeasible.addAll(s);
                    it.remove();
                }
            }
            feasible = newFeasible;
        }
        
        return durations;
    }
}