package plan.memory_buddies;

import action.ActionLauncher;
import org.btrplace.plan.event.Action;
import org.btrplace.plan.event.BootNode;
import org.btrplace.plan.event.MigrateVM;
import org.btrplace.plan.event.ShutdownNode;
import plan.Scheduler;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Created by vkherbac on 18/02/15.
 */
public class MemoryBuddiesScheduler implements Scheduler {
    
    private Map<Action, ActionLauncher> actionsMap, bootMap, shutdownMap, migrationMap;
    private String scriptsDir = null;
    private int para = 0;
    private boolean fixedOrder = false;

    public MemoryBuddiesScheduler(Map<Action, ActionLauncher> actionsMap, int para, boolean fixedOrder, String scriptsDir) {
        this(actionsMap, para, fixedOrder);
        this.scriptsDir = scriptsDir;
    }
    
    public MemoryBuddiesScheduler(Map<Action, ActionLauncher> actionsMap, int para, boolean fixedOrder) {
        this.para = para;
        this.fixedOrder = fixedOrder;
        this.actionsMap = actionsMap;
        
        // Create a map with migrations only
        migrationMap = new HashMap<>();
        for (Action a : actionsMap.keySet()) if (a instanceof MigrateVM) migrationMap.put(a, actionsMap.get(a));

        bootMap = new HashMap<>();
        for (Action a : actionsMap.keySet()) if (a instanceof BootNode) bootMap.put(a, actionsMap.get(a));

        shutdownMap = new HashMap<>();
        for (Action a : actionsMap.keySet()) if (a instanceof ShutdownNode) shutdownMap.put(a, actionsMap.get(a));
    }

    public Map<Action, ActionDuration> start() {

        Map<Future<ActionDuration>, Action> actionStates = new HashMap<>();
        Map<Action, ActionDuration> durations = new HashMap<>();

        ExecutorService service = null;
        
        // Boot all nodes in parallel
        if (!bootMap.isEmpty()) {
            service = Executors.newFixedThreadPool(bootMap.size());
            for (Action b : bootMap.keySet()) {
                ActionLauncher l = bootMap.get(b);
                if (scriptsDir != null) l.setScriptsDir(scriptsDir);
                actionStates.put(service.submit(l), b);
            }
            service.shutdown();
            while (!service.isTerminated()) {
                // Wait for any termination
                synchronized (lock) {
                    try {
                        lock.wait(500);
                    } catch (InterruptedException e) {
                        System.err.println("Interrupted Exception on main thread");
                        System.exit(1);
                    }
                }
            }
        }

        // Schedule all migrations
        if (!migrationMap.isEmpty()) {
            List<Action> migrations = new ArrayList<Action>(migrationMap.keySet());

            // Select the migrations randomly
            if (!fixedOrder) { Collections.shuffle(migrations); }
            
            service = Executors.newFixedThreadPool(para);
            for (Action m : migrations) {
                ActionLauncher l = migrationMap.get(m);
                if (scriptsDir != null) l.setScriptsDir(scriptsDir);
                actionStates.put(service.submit(l), m);
            }
            service.shutdown();
            while (!service.isTerminated()) {
                // Wait for any termination
                synchronized (lock) {
                    try {
                        lock.wait(500);
                    } catch (InterruptedException e) {
                        System.err.println("Interrupted Exception on main thread");
                        System.exit(1);
                    }
                }
            }
        }
        
        // Shutdown all nodes in parallel
        if (!shutdownMap.isEmpty()) {
            service = Executors.newFixedThreadPool(shutdownMap.size());
            for (Action s : shutdownMap.keySet()) {
                ActionLauncher l = shutdownMap.get(s);
                if (scriptsDir != null) l.setScriptsDir(scriptsDir);
                actionStates.put(service.submit(l), s);
            }
            service.shutdown();
            while (!service.isTerminated()) {
                // Wait for any termination
                synchronized (lock) {
                    try {
                        lock.wait(500);
                    } catch (InterruptedException e) {
                        System.err.println("Interrupted Exception on main thread");
                        System.exit(1);
                    }
                }
            }
        }
        
        // Get the date of finished actions
        for (Iterator<Future<ActionDuration>> it = actionStates.keySet().iterator(); it.hasNext(); ) {
            Future<ActionDuration> f = it.next();

            if (f.isDone()) {
                // Get the returned Date
                try {
                    durations.put(actionStates.get(f), f.get());
                } catch (InterruptedException e) {
                    System.err.println("Interrupted Exception during action: " +
                            actionsMap.get((actionStates.get(f))).toString());
                    System.exit(1);
                } catch (ExecutionException e) {
                    System.err.println("Execution Exception for action: " +
                            actionsMap.get((actionStates.get(f))).toString());
                    System.exit(1);
                }
                it.remove();
            }
        }
        
        return durations;
    }
}
