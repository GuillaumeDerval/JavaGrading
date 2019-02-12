import be.ac.ucl.info.javagrading.GradingListener;
import be.ac.ucl.info.javagrading.TestSecurityManager;
import org.junit.runner.JUnitCore;


public class RunTests {
    public static void main(String args[]) {
        System.setSecurityManager(new TestSecurityManager());
        JUnitCore runner = new JUnitCore();
        runner.addListener(new GradingListener());
        runner.run(NormalTests.class);
    }
}