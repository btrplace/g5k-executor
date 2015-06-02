import org.testng.annotations.Test;

import java.io.IOException;

/**
 * Created by vkherbac on 17/02/15.
 */
public class ExecutorTest {

    @Test
    public void intraNodeTest() throws IOException {

        G5kExecutor.main(new String[]{
                "-d", "src/main/bin/scripts_test/",
                "-buddies", "-p", "2",
                "-i", "src/test/resources/micro_intra-node.json",
                "-o", "src/test/resources/micro_intra-node.csv"
        });
        System.err.flush();
        System.out.flush();
    }

    @Test
    public void interNodeTest() throws IOException {

        G5kExecutor.main(new String[]{
                "-d", "src/main/bin/scripts_test/",
                "-buddies", "-p", "2",
                "-i", "src/test/resources/micro_inter-node.json",
                "-o", "src/test/resources/micro_inter-node.csv"
        });
        System.err.flush();
        System.out.flush();
    }

    @Test
    public void intraNodeVanillaFullMemTest() throws IOException {

        G5kExecutor.main(new String[]{
                "-d", "src/main/bin/scripts_test/",
                "-mvm",
                "-i", "src/test/resources/micro_intra_vanilla-realMem.json",
                "-o", "src/test/resources/micro_intra_vanilla-realMem.csv"
        });
        System.err.flush();
        System.out.flush();
    }

    @Test
    public void interNodeVanillaFullMemTest() throws IOException {

        G5kExecutor.main(new String[]{
                "-d", "src/main/bin/scripts_test/",
                "-mvm",
                "-i", "src/test/resources/micro_inter_vanilla-realMem.json",
                "-o", "src/test/resources/micro_inter_vanilla-realMem.csv"
        });
        System.err.flush();
        System.out.flush();
    }

    @Test
    public void intraNodeVanillaRealMemTest() throws IOException {

        G5kExecutor.main(new String[]{
                "-d", "src/main/bin/scripts_test/",
                "-mvm",
                "-i", "src/test/resources/micro_intra_vanilla-fullMem.json",
                "-o", "src/test/resources/micro_intra_vanilla-fullMem.csv"
        });
        System.err.flush();
        System.out.flush();
    }

    @Test
    public void interNodeVanillaRealMemTest() throws IOException {

        G5kExecutor.main(new String[]{
                "-d", "src/main/bin/scripts_test/",
                "-mvm",
                "-i", "src/test/resources/micro_inter_vanilla-fullMem.json",
                "-o", "src/test/resources/micro_inter_vanilla-fullMem.csv"
        });
        System.err.flush();
        System.out.flush();
    }

    @Test
    public void soccTest() throws IOException {

        G5kExecutor.main(new String[]{
                "-d", "src/main/bin/scripts_test/",
                "-mvm",
                "-i", "src/test/resources/socc.json",
                "-o", "src/test/resources/socc.csv"
        });
        System.err.flush();
        System.out.flush();
    }
}
