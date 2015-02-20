import org.junit.Test;

import java.io.IOException;

/**
 * Created by vkherbac on 17/02/15.
 */
public class ExecutorTest {

    @Test
    public void intraNodeTest() throws IOException {

        G5kExecutor.main(new String[]{
                "-s", "src/main/bin/scripts/",
                "-i", "src/test/resources/micro_intra-node.json",
                "-o", "src/test/resources/micro_intra-node.csv"
        });
        System.err.flush();
        System.out.flush();
    }

    @Test
    public void interNodeTest() throws IOException {

        G5kExecutor.main(new String[]{
                "-s", "src/main/bin/scripts/",
                "-i", "src/test/resources/micro_inter-node.json",
                "-o", "src/test/resources/micro_inter-node.csv"
        });
        System.err.flush();
        System.out.flush();
    }
}
