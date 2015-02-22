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
    @Option(name = "-s", aliases = "--scripts-dir", usage = "Scripts location relative directory")
    private String scriptsDir;
    @Option(required = true, name = "-i", aliases = "--input-json", usage = "The json reconfiguration plan to read (can be a .gz)")
    private String planFileName;
    @Option(required = true, name = "-o", aliases = "--output-csv", usage = "Print actions durations to this file")
    private String outputFile;

    public static void main(String[] args) throws IOException {
        new G5kExecutor().parseArgs(args);
    }

    public void parseArgs(String[] args) {

        // Parse the cmdline arguments
        CmdLineParser cmdParser = new CmdLineParser(this);
        cmdParser.setUsageWidth(80);
        try {
            cmdParser.parseArgument(args);
        } catch (CmdLineException e) {
            System.err.println(e.getMessage());
            System.err.println("g5kExecutor [-s scripts_dir] -i json_file -o output_file");
            cmdParser.printUsage(System.err);
            System.err.println();
            System.exit(1);
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
        PlanScheduler executor = new PlanScheduler(plan, actionsMap, scriptsDir);
        Map<Action, PlanScheduler.actionDuration> durations = executor.start();

        if (durations == null || durations.isEmpty()) {
            System.err.println("Unable to retrieve effective durations");
            System.exit(1);
        }

        saveAsCSV(durations);

        // Exit
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

    private void saveAsCSV(Map<Action, PlanScheduler.actionDuration> durations) {

        List<Action> actions = new ArrayList<>(durations.keySet());

        char SEPARATOR = ';';

        BufferedWriter writer = null;

        try {
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFile), "utf-8"));

            // Sort the actions per start and end times
            actions.sort(new Comparator<Action>() {
                @Override
                public int compare(Action action, Action action2) {
                    long result = durations.get(action).getStart().getTime() - durations.get(action2).getStart().getTime();
                    if (result == 0) {
                        result = durations.get(action).getEnd().getTime() - durations.get(action2).getEnd().getTime();
                    }
                    return (int) result;
                }
            });

            // Write header
            writer.write("ACTION" + SEPARATOR + "START" + SEPARATOR + "END");

            // Write actions and timestamps
            for (Action a : actions) {
                writer.newLine();
                writer.append(a.toString()).append(SEPARATOR)
                      .append(String.valueOf(durations.get(a).getStart().getTime()/1000)).append(SEPARATOR)
                      .append(String.valueOf(durations.get(a).getEnd().getTime()/1000));
            }

            writer.flush();
        } catch (IOException ex) {
            System.err.println("IO error occurs when trying to write '" + outputFile + ": " + ex.toString());
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    System.err.println("Unable to close the file '" + outputFile + ": " + e.toString());
                }
            }
        }
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

