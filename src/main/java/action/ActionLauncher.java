package action;

import plan.PlanScheduler;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Created by vkherbac on 17/02/15.
 */
public abstract class ActionLauncher implements Callable<Integer> {

    protected String script;
    protected List<String> params;
    protected Map<String, String> vars;
    protected PlanScheduler.Lock lock;

    //public void execute() {
    @Override
    public Integer call() {

        Process p = null;

        try {
            ProcessBuilder pb;

            // Script parameters
            if (params == null || params.isEmpty()) {
                pb = new ProcessBuilder(script);
            }
            else {
                List<String> options = new ArrayList<String>();
                options.add("./" + script);
                options.addAll(params);
                pb = new ProcessBuilder(options);
            }

            // Environment vars
            if (vars != null && !vars.isEmpty()) {
                Map<String, String> env = pb.environment();
                for (String name : vars.keySet()) {
                    env.put(name, vars.get(name));
                }
            }

            pb.directory(new File("src/main/bin/"));

            // Redirect script outputs to java process
            //pb.redirectInput(ProcessBuilder.Redirect.INHERIT);
            pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
            pb.redirectError(ProcessBuilder.Redirect.INHERIT);

            // Launch the script
            p = pb.start();

            // Wait for termination
            p.waitFor();

            synchronized (lock) {
                lock.notify();
            }

        } catch (Exception e) {
            System.err.println("Error while executing script '" + params.get(0) + "'");
            //e.printStackTrace();
            System.exit(1);
        }

        return p.exitValue();
    }

    public void setSync(PlanScheduler.Lock lock) {
        this.lock = lock;
    }
}
