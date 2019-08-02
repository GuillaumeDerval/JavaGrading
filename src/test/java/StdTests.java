import com.github.guillaumederval.javagrading.*;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

@RunWith(GradingRunner.class)
@GradeClass(totalValue = 100)
public class StdTests {
    @Test(timeout = 3000, expected = SecurityException.class)
    @Grade(value = 5.0, cpuTimeout = 1000)
    public void attemptChangeIO() {
        PrintStream out = System.out;
        System.setOut(new PrintStream(new OutputStream() {
            @Override
            public void write(int b) throws IOException {

            }
        }));
        System.setOut(out);
    }

    @Test(timeout = 500)
    @Grade(value = 5.0)
    @GradeFeedback(message = "Timeout, sorry!", onTimeout = true)
    @GradeFeedback(message = "Failed, sorry!", onFail = true)
    public void shouldTimeout() throws Exception {
        System.out.println("test");
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    @Grade(value = 10)
    public void ignored() {
        Assume.assumeNotNull(null, null);
    }

    @Test
    @Grade(value = 5)
    public void shouldFail() throws CustomGradingResult {
        throw new CustomGradingResult(TestStatus.SUCCESS);
    }

    @Test
    @Grade(value = 5, custom = true)
    public void shouldSuccessWithHalfPointsAndComment() throws CustomGradingResult {
        throw new CustomGradingResult(TestStatus.SUCCESS, 2.5, "More or less half the points");
    }

    @Test
    @Grade(value = 5, custom = true)
    public void shouldSuccessWithAllPointsAndComment() throws CustomGradingResult {
        throw new CustomGradingResult(TestStatus.SUCCESS, "All the points");
    }

    @Test
    @Grade(value = 5, custom = true)
    public void shouldTimeoutWith0PointsAndComment() throws CustomGradingResult {
        Assume.assumeFalse(true);
        throw new CustomGradingResult(TestStatus.TIMEOUT, "Zero!");
    }
}