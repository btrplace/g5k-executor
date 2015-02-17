import org.junit.Test;

import java.io.IOException;

/**
 * Created by vkherbac on 17/02/15.
 */
public class ExecutorTest {

    @Test
    public void test() throws IOException {

        G5kExecutor.main(new String[]{
                "--repair",
                "--timeout", "500",
                "-i", "src/test/resources/4n-4v.json",
                "-o", "src/test/resources/"
        });
        System.err.flush();
        System.out.flush();
    }
}
