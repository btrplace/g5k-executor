import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import net.minidev.json.parser.ParseException;
import org.btrplace.json.JSONConverterException;
import org.btrplace.json.plan.ReconfigurationPlanConverter;
import org.btrplace.plan.ReconfigurationPlan;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import java.io.*;
import java.util.zip.GZIPInputStream;

/**
 * Created by vins on 17/02/15.
 */
public class G5kExecutor {

    // Define options list
    @Option(name = "-r", aliases = "--repair", usage = "Enable the 'repair' feature")
    private boolean repair;
    @Option(name = "-m", aliases = "--optimize", usage = "Enable the 'optimize' feature")
    private boolean optimize;
    @Option(name = "-t", aliases = "--timeout", usage = "Set a timeout (in sec)")
    private int timeout = 0; //5min by default
    @Option(required = true, name = "-i", aliases = "--input-plan-json", usage = "the json reconfiguration plan to read (can be a .gz)")
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
            System.err.println("g5kExecutor [-r] [-m] [-t n_sec] -i file_name -o dir_name");
            cmdParser.printUsage(System.err);
            System.err.println();
            return;
        }

        ReconfigurationPlan plan = loadPlan(planFileName);

        ExecLoop.execute(plan);
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
}

