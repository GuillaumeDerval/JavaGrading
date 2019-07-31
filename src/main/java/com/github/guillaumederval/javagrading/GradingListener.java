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
import java.util.StringJoiner;
import java.util.function.Consumer;
import java.util.function.Function;

class Format {
    static DecimalFormat df = new DecimalFormat("0.##");

    static String format(double d) {
        return df.format(d);
    }

    static String replace(String orig, String toFind, String replaceBy) {
        String[] lines = orig.split("\n");
        for(int i = 0; i < lines.length; i++) {
            int foundIdx = lines[i].indexOf(toFind);
            if(foundIdx != -1) {
                lines[i] = prefix(replaceBy, lines[i].substring(0, foundIdx));
            }
        }
        StringJoiner sj = new StringJoiner("\n");
        for(String s: lines) sj.add(s);
        return sj.toString();
    }

    static String prefix(String orig, String prefix) {
        String[] lines = orig.split("\n");
        StringJoiner sj = new StringJoiner("\n");
        for(String s: lines) sj.add(prefix + s);
        return sj.toString();
    }

    static String csvEscape(String orig) {
        orig = orig.replaceAll("\"", "\"\"");
        return "\"" + orig + "\"";
    }

    static String statusToIcon(TestStatus status) {
        switch (status) {
            case IGNORED:
                return "❓ Ignored";
            case FAILED:
                return "❌ **Failed**";
            case SUCCESS:
                return "✅️ Success";
            case TIMEOUT:
                return "\uD83D\uDD51 **Timeout**";
        }
        return "Unknown status";
    }
}

class GradedTest {
    public final double grade;
    public final TestStatus status;
    public final Description desc;
    public final Failure possibleFailure;

    public GradedTest(Description desc, double grade, TestStatus status, Failure possibleFailure) {
        this.grade = grade;
        this.status = status;
        this.desc = desc;
        this.possibleFailure = possibleFailure;
    }

    private String toStringText() {
        StringBuilder out = new StringBuilder(desc.getDisplayName()).append(" ")
                .append(status).append(" ")
                .append(Format.format(grade));
        processGradeFeedbacks((s) -> {
            out.append("\n").append(Format.prefix(s, "\t"));
        });
        return out.toString();
    }

    private String toRSTCSVTableLine() {
        StringBuilder out = new StringBuilder(Format.csvEscape("**→** " + desc.getDisplayName())).append(",")
                .append(Format.statusToIcon(status)).append(",")
                .append(Format.format(grade));

        ArrayList<String> fts = new ArrayList<>();
        processGradeFeedbacks(fts::add);
        if(fts.size() != 0)
        out.append(",").append(Format.csvEscape(String.join("\n\n", fts)));

        return out.toString();
    }

    @Override
    public String toString() {
        if(Config.outputAsRST)
            return toRSTCSVTableLine();
        else
            return toStringText();
    }

    private void processGradeFeedbacks(Consumer<String> op) {
        //If there is exactly one @GradeFeedback, feedback is not null, and feedbacks is null.
        //If there are more than one @GradeFeedback, feedback is null, and feedbacks is not null.
        GradeFeedback feedback = desc.getAnnotation(GradeFeedback.class);
        GradeFeedbacks feedbacks = desc.getAnnotation(GradeFeedbacks.class);
        if(feedback != null)
            if(shouldDisplayFeedback(feedback))
                op.accept(formatFeedback(feedback.message()));
        if(feedbacks != null) {
            for(GradeFeedback f: feedbacks.value()) {
                if(shouldDisplayFeedback(f))
                    op.accept(formatFeedback(f.message()));
            }
        }
    }

    private boolean shouldDisplayFeedback(GradeFeedback f) {
        boolean show = !f.onFail() && !f.onIgnore() && !f.onSuccess() && !f.onTimeout() && (status == TestStatus.FAILED || status == TestStatus.TIMEOUT);
        show |= f.onSuccess() && status == TestStatus.SUCCESS;
        show |= f.onFail() && status == TestStatus.FAILED;
        show |= f.onTimeout() && status == TestStatus.TIMEOUT;
        show |= f.onIgnore() && status == TestStatus.IGNORED;
        return show;
    }

    private String formatFeedback(String feedback) {
        if(possibleFailure != null)
            return Format.replace(Format.replace(feedback,"$trace", possibleFailure.getTrace()),"$exception", possibleFailure.getException().toString());
        return feedback;
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

    public void add(Description desc, double grade, TestStatus status, Failure possibleFailure) {
        if(!grades.containsKey(desc))
            grades.put(desc, new GradedTest(desc, grade, status, possibleFailure));
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



    private void printStatusText() {
        System.out.println("- " + cls.toString() + " " + Format.format(getGrade(true)) + "/" + Format.format(getMax(true)));

        ArrayList<GradedTest> gcl = new ArrayList<>(grades.values());
        Collections.sort(gcl, new NaturalOrderComparator());

        for(GradedTest t: gcl) {
            System.out.println(Format.prefix(t.toString(), "\t\t"));
        }
    }

    private void printStatusRST() {
        StringBuilder out = new StringBuilder("    ")
                .append(Format.csvEscape("**" + cls.toString() + "**")).append(",")
                .append(",**")
                .append(Format.format(getGrade(true)))
                .append("/")
                .append(Format.format(getMax(true)))
                .append("**");
        System.out.println(out.toString());

        ArrayList<GradedTest> gcl = new ArrayList<>(grades.values());
        Collections.sort(gcl, new NaturalOrderComparator());

        for(GradedTest t: gcl) {
            System.out.println(Format.prefix(t.toString(), "    "));
        }
    }

    public void printStatus() {
        if(Config.outputAsRST)
            printStatusRST();
        else
            printStatusText();
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

    private void addTestResult(Description description, TestStatus status, Failure possibleFailure) {
        if(!shouldBeGraded(description))
            return;

        GradedClass gc = getGradedClassObj(description.getTestClass());

        double value = gc.defaultValue;

        Grade g = description.getAnnotation(Grade.class);
        if(g != null)
            value = g.value();

        if(value == -1.0)
            return;

        gc.add(description, value, status, possibleFailure);
    }

    public void testRunStarted(Description description) throws Exception {
        classes = new HashMap<>();

    }

    public void testRunFinished(Result result) throws Exception {
        System.out.print("--- GRADE ---" +"\n\r"); // yields better and simpler output format to inginious
        double grade = 0;
        double gradeWithoutIgnored = 0;
        double max = 0;
        double maxWithoutIgnored = 0;
        ArrayList<GradedClass> gcl = new ArrayList<>(classes.values());
        Collections.sort(gcl, new NaturalOrderComparator());

        if(Config.outputAsRST) {
            System.out.println(".. csv-table::\n" +
                    "    :header: \"Test\", \"Status\", \"Grade\", \"Comment\"\n" +
                    "    :widths: auto\n"+
                    "    ");
        }

        for(GradedClass c: gcl) {
            if(c.getMax(true) != 0) {
                c.printStatus();
                grade += c.getGrade(true);
                max += c.getMax(true);
                gradeWithoutIgnored += c.getGrade(false);
                maxWithoutIgnored += c.getMax(false);
            }
        }

        if(Config.outputAsRST) {
            System.out.println("    \"**TOTAL**\",,**"+Format.format(grade)+"/"+Format.format(max)+"**");
            System.out.println("    \"**TOTAL WITHOUT IGNORED**\",,**"+Format.format(gradeWithoutIgnored)+"/"+Format.format(maxWithoutIgnored)+"**");
            System.out.println();
        }
        System.out.print("TOTAL "+Format.format(grade)+"/"+Format.format(max) + "\n \r");
        System.out.print("TOTAL WITHOUT IGNORED "+Format.format(gradeWithoutIgnored)+"/"+Format.format(maxWithoutIgnored) + "\n \r");
        System.out.println("--- END GRADE ---");
    }

    public void testFinished(Description description) throws Exception {
        addTestResult(description, TestStatus.SUCCESS, null);
    }

    public void testFailure(Failure failure) throws Exception {
        if(failure.getException() instanceof TestTimedOutException)
            addTestResult(failure.getDescription(), TestStatus.TIMEOUT, failure);
        else
            addTestResult(failure.getDescription(), TestStatus.FAILED, failure);
    }

    public void testAssumptionFailure(Failure failure) {
        addTestResult(failure.getDescription(), TestStatus.IGNORED, null);
    }

    public void testIgnored(Description description) throws Exception {
        addTestResult(description, TestStatus.IGNORED, null);
    }
}
