package be.ac.ucl.info.javagrading;

import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

import java.io.PrintStream;
import java.util.HashMap;

class GradedTest {
    public final double grade;
    public final TestStatus status;
    public final Description desc;

    public GradedTest(Description desc, double grade, TestStatus status) {
        this.grade = grade;
        this.status = status;
        this.desc = desc;
    }
}

class GradedClass {
    public final Class cls;
    public final double totalValue;
    public final double defaultValue;
    public final boolean allCorrect;
    public final HashMap<Description, GradedTest> grades;

    public GradedClass(Class cls, double totalValue, double defaultValue, boolean allCorrect) {
        this.cls = cls;
        this.totalValue = totalValue;
        this.defaultValue = defaultValue;
        this.allCorrect = allCorrect;
        this.grades = new HashMap<>();
    }

    public void add(Description desc, double grade, TestStatus status) {
        if(!grades.containsKey(desc))
            grades.put(desc, new GradedTest(desc, grade, status));
    }

    public double getMax() {
        if(totalValue != -1.0)
            return totalValue;
        else {
            double m = 0;
            for(GradedTest t: grades.values()) {
                m += t.grade;
            }
            return m;
        }
    }

    public double getGrade() {
        double g = 0;
        double m = 0;
        for(GradedTest t: grades.values()) {
            if(t.status == TestStatus.SUCCESS)
                g += t.grade;
            m += t.grade;
        }
        if(allCorrect) {
            if(m != g)
                g = 0;
        }
        if(totalValue != -1.0)
            g = g * totalValue / m;
        return g;
    }

    public void printStatus() {
        System.out.println("- " + cls.toString() + " " + getGrade() + "/" + getMax());
        for(GradedTest t: grades.values()) {
            System.out.println("\t\t" + t.desc.getDisplayName() + " " + t.status + " " + t.grade);
        }
    }
}

public class GradingListener extends RunListener {

    private HashMap<Class, GradedClass> classes;
    private PrintStream oStdout;
    private PrintStream oStderr;

    private GradedClass getGradedClassObj(Class cls) {
        if(classes.containsKey(cls))
            return classes.get(cls);

        // Check for annotations
        double totalValue = -1;
        double defaultValue = -1;
        boolean allCorrect = false;

        GradeClass gc = (GradeClass) cls.getAnnotation(GradeClass.class);
        if(gc != null) {
            totalValue = gc.totalValue();
            defaultValue = gc.defaultValue();
            allCorrect = gc.allCorrect();
        }

        GradedClass gco = new GradedClass(cls, totalValue, defaultValue, allCorrect);
        classes.put(cls, gco);
        return gco;
    }

    private void addTestResult(Description description, TestStatus status) {
        GradedClass gc = getGradedClassObj(description.getTestClass());

        double value = gc.defaultValue;

        Grade g = description.getAnnotation(Grade.class);
        if(g != null)
            value = g.value();

        if(value == -1.0)
            return;

        gc.add(description, value, status);
    }

    public void testRunStarted(Description description) throws Exception {
        classes = new HashMap<>();
        oStdout = System.out;
        oStderr = System.err;
        System.setOut(new PrintStream(new SinkStream()));
        System.setErr(new PrintStream(new SinkStream()));
    }

    public void testRunFinished(Result result) throws Exception {
        System.out.flush();
        System.err.flush();
        System.setOut(oStdout);
        System.setErr(oStderr);
        System.out.println("--- GRADE ---");
        double grade = 0;
        double max = 0;
        for(GradedClass c: classes.values()) {
            if(c.getMax() != 0) {
                c.printStatus();
                grade += c.getGrade();
                max += c.getMax();
            }
        }
        System.out.println("TOTAL "+grade+"/"+max);
        System.out.println("--- END GRADE ---");
    }

    public void testStarted(Description description) throws Exception {
    }

    public void testFinished(Description description) throws Exception {
        addTestResult(description, TestStatus.SUCCESS);
    }

    public void testFailure(Failure failure) throws Exception {
        addTestResult(failure.getDescription(), TestStatus.FAILED);
    }

    public void testAssumptionFailure(Failure failure) {
        addTestResult(failure.getDescription(), TestStatus.IGNORED);
    }

    public void testIgnored(Description description) throws Exception {
        addTestResult(description, TestStatus.IGNORED);
    }
}
