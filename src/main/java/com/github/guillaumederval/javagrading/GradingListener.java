package com.github.guillaumederval.javagrading;

import com.github.guillaumederval.javagrading.utils.NaturalOrderComparator;
import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;
import org.junit.runners.model.TestTimedOutException;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

class Format {
    static DecimalFormat df = new DecimalFormat("0.##");

    static String format(double d) {
        return df.format(d);
    }
}

class GradedTest {
    public final double grade;
    public final TestStatus status;
    public final Description desc;

    public GradedTest(Description desc, double grade, TestStatus status) {
        this.grade = grade;
        this.status = status;
        this.desc = desc;
    }

    @Override
    public String toString() {
        return desc.getDisplayName() + " " + status + " " + Format.format(grade);
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
        System.out.println("- " + cls.toString() + " " + Format.format(getGrade()) + "/" + Format.format(getMax()));

        ArrayList<GradedTest> gcl = new ArrayList<>(grades.values());
        Collections.sort(gcl, new NaturalOrderComparator());

        for(GradedTest t: gcl) {
            System.out.println("\t\t" + t.toString());
        }
    }

    @Override
    public String toString() {
        return cls.toString();
    }
}

/**
 * Listener that outputs the grades.
 */
public class GradingListener extends RunListener {
    private HashMap<Class, GradedClass> classes;

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

    }

    public void testRunFinished(Result result) throws Exception {
        System.out.println("--- GRADE ---");
        double grade = 0;
        double max = 0;
        ArrayList<GradedClass> gcl = new ArrayList<>(classes.values());
        Collections.sort(gcl, new NaturalOrderComparator());
        for(GradedClass c: gcl) {
            if(c.getMax() != 0) {
                c.printStatus();
                grade += c.getGrade();
                max += c.getMax();
            }
        }
        System.out.println("TOTAL "+Format.format(grade)+"/"+Format.format(max));
        System.out.println("--- END GRADE ---");
    }

    public void testFinished(Description description) throws Exception {
        addTestResult(description, TestStatus.SUCCESS);
    }

    public void testFailure(Failure failure) throws Exception {
        if(failure.getException() instanceof TestTimedOutException)
            addTestResult(failure.getDescription(), TestStatus.TIMEOUT);
        else
            addTestResult(failure.getDescription(), TestStatus.FAILED);
    }

    public void testAssumptionFailure(Failure failure) {
        addTestResult(failure.getDescription(), TestStatus.IGNORED);
    }

    public void testIgnored(Description description) throws Exception {
        addTestResult(description, TestStatus.IGNORED);
    }
}
