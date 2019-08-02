package com.github.guillaumederval.javagrading;

import com.github.guillaumederval.javagrading.utils.NaturalOrderComparator;
import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;
import org.junit.runners.model.TestTimedOutException;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;
import java.util.function.Consumer;

import static java.lang.Double.NaN;

class Format {
    private static DecimalFormat df = initDF();

    private static DecimalFormat initDF() {
        DecimalFormatSymbols otherSymbols = new DecimalFormatSymbols(Locale.ENGLISH);
        otherSymbols.setDecimalSeparator('.');
        otherSymbols.getGroupingSeparator();
        return new DecimalFormat("0.##", otherSymbols);
    }

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

/**
 * Listener that outputs the grades.
 */
public class GradingListener extends RunListener {
    private HashMap<Class, GradedClass> classes;
    private boolean outputAsRST; //if set to false, outputs as text instead.

    /**
     * Outputs to RST by default
     */
    public GradingListener() {
        super();
        outputAsRST = true;
    }

    /**
     * @param outputAsRST true to output RST, false to output text
     */
    public GradingListener(boolean outputAsRST) {
        super();
        this.outputAsRST = outputAsRST;
    }

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

    private void addTestResult(Description description, TestStatus status, double customGrade, Throwable possibleException, String customFeedback, boolean isCustom) {
        if(!shouldBeGraded(description))
            return;

        GradedClass gc = getGradedClassObj(description.getTestClass());

        double maxGrade = gc.defaultValue;

        Grade g = description.getAnnotation(Grade.class);

        if(isCustom && !g.custom()) {
            System.out.println("WARNING: Received a CustomGradingResult exception while not expecting one.");
            System.out.println("If you are trying to solve this exercise: sadly, there is a protection against this ;-)");
            System.out.println("If you are the exercise creator, you probably forgot to put custom=true inside @Grade.");

            status = TestStatus.FAILED;
            customGrade = NaN;
            possibleException = null;
        }

        if(g != null)
            maxGrade = g.value();

        if(maxGrade == -1.0)
            return;

        double grade = 0;
        if(!isCustom || Double.isNaN(customGrade)) {
            if (status == TestStatus.SUCCESS)
                grade = maxGrade;
        }
        else {
            grade = customGrade;
        }

        gc.add(description, grade, maxGrade, status, possibleException, customFeedback);
    }

    private void addTestResult(Description description, CustomGradingResult customGradingResult) {
        addTestResult(description, customGradingResult.status, customGradingResult.grade, customGradingResult.origException, customGradingResult.feedback, true);
    }

    private void addTestResult(Description description, TestStatus status, Failure possibleFailure) {
        addTestResult(description, status, NaN, possibleFailure == null ? null : possibleFailure.getException(), null,false);
    }

    public void testRunStarted(Description description) {
        classes = new HashMap<>();
    }

    public void testRunFinished(Result result) {
        System.out.println("--- GRADE ---");
        double grade = 0;
        double gradeWithoutIgnored = 0;
        double max = 0;
        double maxWithoutIgnored = 0;
        ArrayList<GradedClass> gcl = new ArrayList<>(classes.values());
        gcl.sort(new NaturalOrderComparator());

        if(outputAsRST) {
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

        if(outputAsRST) {
            System.out.println("    \"**TOTAL**\",,**"+Format.format(grade)+"/"+Format.format(max)+"**");
            System.out.println("    \"**TOTAL WITHOUT IGNORED**\",,**"+Format.format(gradeWithoutIgnored)+"/"+Format.format(maxWithoutIgnored)+"**");
            System.out.println();
        }
        System.out.println("TOTAL "+Format.format(grade)+"/"+Format.format(max));
        System.out.println("TOTAL WITHOUT IGNORED "+Format.format(gradeWithoutIgnored)+"/"+Format.format(maxWithoutIgnored));
        System.out.println("--- END GRADE ---");
    }

    public void testFinished(Description description) {
        addTestResult(description, TestStatus.SUCCESS, null);
    }

    public void testFailure(Failure failure) {
        if(failure.getException() instanceof TestTimedOutException)
            addTestResult(failure.getDescription(), TestStatus.TIMEOUT, failure);
        else if(failure.getException() instanceof CustomGradingResult)
            addTestResult(failure.getDescription(), (CustomGradingResult)failure.getException());
        else
            addTestResult(failure.getDescription(), TestStatus.FAILED, failure);
    }

    public void testAssumptionFailure(Failure failure) {
        addTestResult(failure.getDescription(), TestStatus.IGNORED, null);
    }

    public void testIgnored(Description description) {
        addTestResult(description, TestStatus.IGNORED, null);
    }






    class GradedTest {
        final double grade;
        final double maxGrade;
        final TestStatus status;
        private final Description desc;
        private final Throwable possibleException;
        private final String customFeedback;

        GradedTest(Description desc, double grade, double maxGrade, TestStatus status, Throwable possibleException, String customFeedback) {
            this.maxGrade = maxGrade;
            this.grade = grade;
            this.status = status;
            this.desc = desc;
            this.possibleException = possibleException;
            this.customFeedback = customFeedback;
        }

        private String toStringText(double ratio) {
            StringBuilder out = new StringBuilder(desc.getDisplayName()).append(" ")
                    .append(status).append(" ")
                    .append(Format.format(grade*ratio))
                    .append("/")
                    .append(Format.format(maxGrade*ratio));
            processGradeFeedbacks((s) -> out.append("\n").append(Format.prefix(s, "\t")));
            return out.toString();
        }

        private String toRSTCSVTableLine(double ratio) {
            StringBuilder out = new StringBuilder(Format.csvEscape("**→** " + desc.getDisplayName())).append(",")
                    .append(Format.statusToIcon(status)).append(",")
                    .append(Format.format(grade*ratio))
                    .append("/")
                    .append(Format.format(maxGrade*ratio));

            ArrayList<String> fts = new ArrayList<>();
            processGradeFeedbacks(fts::add);
            if(fts.size() != 0)
                out.append(",").append(Format.csvEscape(String.join("\n\n", fts)));

            return out.toString();
        }

        @Override
        public String toString() {
            return toString(1);
        }

        public String toString(double ratio) {
            if(outputAsRST)
                return toRSTCSVTableLine(ratio);
            else
                return toStringText(ratio);
        }

        private void processGradeFeedbacks(Consumer<String> op) {
            //If there is exactly one @GradeFeedback, feedback is not null, and feedbacks is null.
            //If there are more than one @GradeFeedback, feedback is null, and feedbacks is not null.

            if(customFeedback == null) {
                GradeFeedback feedback = desc.getAnnotation(GradeFeedback.class);
                GradeFeedbacks feedbacks = desc.getAnnotation(GradeFeedbacks.class);
                if (feedback != null)
                    if (shouldDisplayFeedback(feedback))
                        op.accept(formatFeedback(feedback.message()));
                if (feedbacks != null) {
                    for (GradeFeedback f : feedbacks.value()) {
                        if (shouldDisplayFeedback(f))
                            op.accept(formatFeedback(f.message()));
                    }
                }
            }
            else
                op.accept(customFeedback);
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
            if(possibleException != null) {
                StringWriter stringWriter = new StringWriter();
                PrintWriter writer = new PrintWriter(stringWriter);
                possibleException.printStackTrace(writer);
                String trace = stringWriter.toString();

                return Format.replace(Format.replace(feedback, "$trace", trace), "$exception", possibleException.toString());
            }
            return feedback;
        }
    }

    class GradedClass {
        private final Class cls;
        private final double totalValue;
        final double defaultValue;
        private final boolean allCorrect;
        private final HashMap<Description, GradedTest> grades;

        GradedClass(Class cls, double totalValue, double defaultValue, boolean allCorrect) {
            this.cls = cls;
            this.totalValue = totalValue;
            this.defaultValue = defaultValue;
            this.allCorrect = allCorrect;
            this.grades = new HashMap<>();
        }

        void add(Description desc, double grade, double maxGrade, TestStatus status, Throwable possibleException, String customFeedback) {
            if(!grades.containsKey(desc))
                grades.put(desc, new GradedTest(desc, grade, maxGrade, status, possibleException, customFeedback));
        }

        class GradeResult {
            double grade;
            double maxGrade;
            double maxGradeWithoutIgnored;
            boolean allIsSuccess;
            boolean allIsSuccessOrIgnore;

            GradeResult() {
                grade = 0;
                maxGrade = 0;
                maxGradeWithoutIgnored = 0;
                allIsSuccess = allIsSuccessOrIgnore = true;
            }
        }

        double getPonderationRatio() {
            GradeResult gradeResult = getGradeResult();

            double realMax = gradeResult.maxGrade;

            if(realMax == 0.0)
                return 0.0;

            double destRealMax = totalValue;
            if(destRealMax == -1.0)
                destRealMax = realMax;

            return destRealMax/realMax;
        }

        double getMax(boolean includingIgnored) {
            GradeResult gradeResult = getGradeResult();

            double realMax = gradeResult.maxGrade;

            if(realMax == 0.0)
                return 0.0;

            double destRealMax = totalValue;
            if(destRealMax == -1.0)
                destRealMax = realMax;

            if(includingIgnored)
                return destRealMax;

            double notIgnoredRatio = gradeResult.maxGradeWithoutIgnored/gradeResult.maxGrade;
            return destRealMax*notIgnoredRatio;
        }

        double getGrade(boolean includingIgnored) {
            GradeResult gradeResult = getGradeResult();

            double realMax = gradeResult.maxGrade;

            if(realMax == 0.0)
                return 0.0;

            if(allCorrect) {
                if(includingIgnored && !gradeResult.allIsSuccess)
                    return 0.0;
                if(!includingIgnored && !gradeResult.allIsSuccessOrIgnore)
                    return 0.0;
            }

            double destRealMax = totalValue;
            if(destRealMax == -1.0)
                destRealMax = realMax;

            return gradeResult.grade * destRealMax / realMax;
        }

        private GradeResult getGradeResult() {
            GradeResult r = new GradeResult();
            for(GradedTest t: grades.values()) {
                r.allIsSuccess &= t.status == TestStatus.SUCCESS;
                r.allIsSuccessOrIgnore &= t.status == TestStatus.SUCCESS || t.status == TestStatus.IGNORED;
                r.grade += t.grade;
                r.maxGrade += t.maxGrade;
                if(t.status != TestStatus.IGNORED)
                    r.maxGradeWithoutIgnored += t.maxGrade;
            }

            return r;
        }

        private void printStatusText() {
            System.out.println("- " + cls.toString() + " " + Format.format(getGrade(true)) + "/" + Format.format(getMax(true)));

            ArrayList<GradedTest> gcl = new ArrayList<>(grades.values());
            gcl.sort(new NaturalOrderComparator());

            for(GradedTest t: gcl) {
                System.out.println(Format.prefix(t.toString(getPonderationRatio()), "\t"));
            }
        }

        private void printStatusRST() {
            String out = "    " +
                    Format.csvEscape("**" + cls.toString() + "**") + "," +
                    ",**" +
                    Format.format(getGrade(true)) +
                    "/" +
                    Format.format(getMax(true)) +
                    "**";
            System.out.println(out);

            ArrayList<GradedTest> gcl = new ArrayList<>(grades.values());
            gcl.sort(new NaturalOrderComparator());

            for(GradedTest t: gcl) {
                System.out.println(Format.prefix(t.toString(getPonderationRatio()), "    "));
            }
        }

        void printStatus() {
            if(outputAsRST)
                printStatusRST();
            else
                printStatusText();
        }

        @Override
        public String toString() {
            return cls.toString();
        }
    }
}
