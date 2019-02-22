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

    class GradeResult {
        public double successGrade;
        public double ignoredGrade;
        public double failedGrade;

        public GradeResult() {
            successGrade = 0;
            ignoredGrade = 0;
            failedGrade = 0;
        }

        public double getMax() { return successGrade + ignoredGrade + failedGrade; }
        public double getNotIgnoredRatio() { return (successGrade + failedGrade) / getMax(); }
    }

    public double getMax(boolean includingIgnored) {
        GradeResult gradeResult = getGradeResult();

        double realMax = gradeResult.getMax();

        if(realMax == 0.0)
            return 0.0;

        double destRealMax = totalValue;
        if(destRealMax == -1.0)
            destRealMax = realMax;

        if(includingIgnored)
            return destRealMax;

        double notIgnoredRatio = gradeResult.getNotIgnoredRatio();
        return destRealMax*notIgnoredRatio;
    }

    public double getGrade(boolean includingIgnored) {
        GradeResult gradeResult = getGradeResult();

        double realMax = gradeResult.getMax();

        if(realMax == 0.0)
            return 0.0;

        if(allCorrect) {
            if(includingIgnored && gradeResult.successGrade != realMax)
                return 0.0;
            if(!includingIgnored && gradeResult.successGrade - (realMax - gradeResult.ignoredGrade) < 1e-5) //because floating point magic.
                return 0.0;
        }

        double destRealMax = totalValue;
        if(destRealMax == -1.0)
            destRealMax = realMax;

        return gradeResult.successGrade * destRealMax / realMax;
    }

    protected GradeResult getGradeResult() {
        GradeResult r = new GradeResult();
        for(GradedTest t: grades.values()) {
            if (t.status == TestStatus.SUCCESS)
                r.successGrade += t.grade;
            else if(t.status == TestStatus.IGNORED)
                r.ignoredGrade += t.grade;
            else
                r.failedGrade += t.grade;
        }

        return r;
    }



    public void printStatus() {
        System.out.println("- " + cls.toString() + " " + Format.format(getGrade(true)) + "/" + Format.format(getMax(true)));

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

    private boolean shouldBeGraded(Description desc) {
        return desc.getAnnotation(Grade.class) != null || desc.getTestClass().getAnnotation(GradeClass.class) != null;
    }

    private void addTestResult(Description description, TestStatus status) {
        if(!shouldBeGraded(description))
            return;

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
        double gradeWithoutIgnored = 0;
        double max = 0;
        double maxWithoutIgnored = 0;
        ArrayList<GradedClass> gcl = new ArrayList<>(classes.values());
        Collections.sort(gcl, new NaturalOrderComparator());
        for(GradedClass c: gcl) {
            if(c.getMax(true) != 0) {
                c.printStatus();
                grade += c.getGrade(true);
                max += c.getMax(true);
                gradeWithoutIgnored += c.getGrade(false);
                maxWithoutIgnored += c.getMax(false);
            }
        }
        System.out.println("TOTAL "+Format.format(grade)+"/"+Format.format(max));
        System.out.println("TOTAL WITHOUT IGNORED "+Format.format(gradeWithoutIgnored)+"/"+Format.format(maxWithoutIgnored));
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
