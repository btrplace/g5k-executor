import action.*;
import org.btrplace.plan.DefaultReconfigurationPlanMonitor;
import org.btrplace.plan.ReconfigurationPlan;
import org.btrplace.plan.event.Action;
import org.btrplace.plan.event.BootNode;
import org.btrplace.plan.event.MigrateVM;
import org.btrplace.plan.event.ShutdownNode;

import java.util.*;

/**
 * Created by vkherbac on 17/02/15.
 */
public final class ExecLoop {

    public static void execute(ReconfigurationPlan plan) {

        // Get actions
        Set<Action> actionsSet = plan.getActions();
        if(actionsSet.isEmpty()) {
            System.err.println("The provided plan does not contains any action.");
            System.exit(1);
        }

        // From set to list
        List<Action> actions = new ArrayList<Action>();
        actions.addAll(actionsSet);

        // Check plan duration
        int duration = plan.getDuration();
        if (duration <= 0) {
            System.err.println("The plan duration is wrong.");
            System.exit(1);
        }

        // Sort the actions per start and end times
        actions.sort(new Comparator<Action>() {
            @Override
            public int compare(Action action, Action action2) {
                int result = action.getStart() - action2.getStart();
                if (result == 0) {
                    result = action.getEnd() - action2.getEnd();
                }
                return result;
            }
        });

        // Associate each Action with an ActionLauncher
        Map<Action, ActionLauncher> actionLauncherMap = new HashMap<Action, ActionLauncher>();
        for (Action a : actions) {
            if (a instanceof MigrateVM) {
                actionLauncherMap.put(a,
                        new Migrate(((MigrateVM) a).getVM(),
                            ((MigrateVM) a).getSourceNode(),
                            ((MigrateVM) a).getDestinationNode(),
                            ((MigrateVM) a).getBandwidth())
                );
            }
            if (a instanceof ShutdownNode) {
                actionLauncherMap.put(a, new Shutdown(((ShutdownNode) a).getNode()));
            }
            if (a instanceof BootNode) {
                actionLauncherMap.put(a, new Boot(((BootNode) a).getNode()));
            }
        }

        DefaultReconfigurationPlanMonitor planMonitor = new DefaultReconfigurationPlanMonitor(plan);

        while (planMonitor.getNbCommitted() < actions.size()) {

            for (Action a : actions) {
                if (!planMonitor.isBlocked(a)) {
                }
            }
        }
    }
}
