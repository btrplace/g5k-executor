package action;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by vkherbac on 17/02/15.
 */
public abstract class ActionLauncher {

    protected String script;
    protected List<String> params;
    protected Map<String, String> vars;

    public void execute() {
        try {
            ProcessBuilder pb;

            // Script parameters
            if (params == null || params.isEmpty()) {
                pb = new ProcessBuilder(script);
            }
            else {
                List<String> options = new ArrayList<String>();
                options.add(script);
                options.addAll(params);
                pb = new ProcessBuilder(options.toArray(new String[options.size()]));
            }

            // Environment vars
            if (params != null && !vars.isEmpty()) {
                Map<String, String> env = pb.environment();
                for (String name : vars.keySet()) {
                    env.put(name, vars.get(name));
                }
            }

            // Redirect script outputs to java process
            //pb.redirectInput(ProcessBuilder.Redirect.INHERIT);
            pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
            pb.redirectError(ProcessBuilder.Redirect.INHERIT);

            // Launch the script
            Process p = pb.start();

            // Wait for termination
            p.waitFor();

        } catch (Exception e) {
            System.err.println("Error while executing script '" + params.get(0) + "'");
            e.printStackTrace();
            //System.exit(1);
        }
    }
}
