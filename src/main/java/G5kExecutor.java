import action.ActionLauncher;
import action.Boot;
import action.Migrate;
import action.Shutdown;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import net.minidev.json.parser.ParseException;
import org.btrplace.json.JSONConverterException;
import org.btrplace.json.plan.ReconfigurationPlanConverter;
import org.btrplace.plan.ReconfigurationPlan;
import org.btrplace.plan.event.Action;
import org.btrplace.plan.event.BootNode;
import org.btrplace.plan.event.MigrateVM;
import org.btrplace.plan.event.ShutdownNode;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import plan.PlanScheduler;

import java.io.*;
import java.util.*;
import java.util.zip.GZIPInputStream;

/**
 * Created by vins on 17/02/15.
 */
public class G5kExecutor {

    // Define options list
    @Option(name = "-t", aliases = "--timeout", usage = "Set a timeout (in sec)")
    private int timeout = 0; //5min by default
    @Option(required = true, name = "-i", aliases = "--input-json", usage = "the json reconfiguration plan to read (can be a .gz)")
    private String planFileName;
    @Option(required = true, name = "-o", aliases = "--output-dir", usage = "Output to this directory")
    private String dst;

    public static void main(String[] args) throws IOException {
        new G5kExecutor().parseArgs(args);
    }

    public void parseArgs(String[] args) {

        // Parse the cmdline arguments
        CmdLineParser cmdParser = new CmdLineParser(this);
        cmdParser.setUsageWidth(80);
        try {
            cmdParser.parseArgument(args);
            if (timeout < 0)
                throw new CmdLineException("Timeout can not be < 0 !");
        } catch (CmdLineException e) {
            System.err.println(e.getMessage());
            System.err.println("g5kExecutor [-t n_sec] -i file_name -o dir_name");
            cmdParser.printUsage(System.err);
            System.err.println();
            return;
        }

        // Retrieve the plan
        ReconfigurationPlan plan = loadPlan(planFileName);

        // Get actions
        Set<Action> actionsSet = plan.getActions();
        if (actionsSet.isEmpty()) {
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
        actions.sort((action, action2) -> {
            int result = action.getStart() - action2.getStart();
            if (result == 0) {
                result = action.getEnd() - action2.getEnd();
            }
            return result;
        });

        // Create an ActionLauncher for each Action
        Map<Action, ActionLauncher> actionsMap = new HashMap<>();
        for (Action a : actions) {
            actionsMap.put(a, createLauncher(a));
        }

        // Schedule all actions
        PlanScheduler executor = new PlanScheduler(plan, actionsMap);
        executor.start();

        System.exit(0);
    }

    private ReconfigurationPlan loadPlan(String fileName) {

        // Read the input JSON file
        JSONParser parser = new JSONParser(JSONParser.DEFAULT_PERMISSIVE_MODE);
        Object obj = null;
        try {
            // Check for gzip extension
            if (fileName.endsWith(".gz")) {
                obj = parser.parse(new InputStreamReader(new GZIPInputStream(new FileInputStream(fileName))));
            } else {
                obj = parser.parse(new FileReader(fileName));
            }
        } catch (ParseException e) {
            System.err.println("Error during XML file parsing: " + e.toString());
            System.exit(1);
        } catch (FileNotFoundException e) {
            System.err.println("File '"+fileName+"' not found (" + e.toString() + ")");
            System.exit(1);
        } catch (IOException e) {
            System.err.println("IO error while loading plan: " + e.toString());
            System.exit(1);
        }
        JSONObject o = (JSONObject) obj;

        ReconfigurationPlanConverter planConverter = new ReconfigurationPlanConverter();
        try {
            return planConverter.fromJSON(o);
        } catch (JSONConverterException e) {
            System.err.println("Error while converting plan: " + e.toString());
            e.printStackTrace();
            System.exit(1);
        }

        return null;
    }

    private ActionLauncher createLauncher(Action a) {
        if (a instanceof MigrateVM) {
            return new Migrate(((MigrateVM) a).getVM(),
                            ((MigrateVM) a).getSourceNode(),
                            ((MigrateVM) a).getDestinationNode(),
                            ((MigrateVM) a).getBandwidth()
            );
        }
        if (a instanceof ShutdownNode) {
            return new Shutdown(((ShutdownNode) a).getNode());
        }
        if (a instanceof BootNode) {
            return new Boot(((BootNode) a).getNode());
        }
        return null;
    }
}

