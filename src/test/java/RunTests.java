import com.github.guillaumederval.javagrading.GradingListener;
import org.junit.runner.JUnitCore;


public class RunTests {
    public static void main(String args[]) {
        JUnitCore runner = new JUnitCore();
        runner.addListener(new GradingListener(false));
        runner.run(StdTests.class, ParametersTests.class, PermissionTest.class);
    }
}