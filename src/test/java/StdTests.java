import com.github.guillaumederval.javagrading.Grade;
import com.github.guillaumederval.javagrading.GradingRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

@RunWith(GradingRunner.class)
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

    @Test(timeout = 3000)
    @Grade(value = 5.0)
    public void example() {
        System.out.println("test");
    }
}