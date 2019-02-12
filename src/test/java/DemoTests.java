import be.ac.ucl.info.javagrading.Grade;
import be.ac.ucl.info.javagrading.GradingRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

@RunWith(GradingRunner.class)
public class DemoTests {
    @Test(timeout = 3000, expected = SecurityException.class)
    @Grade(value = 5.0, cputimeout = 1000)
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