package be.ac.ucl.info.javagrading;

import org.junit.AssumptionViolatedException;
import org.junit.Rule;
import org.junit.internal.runners.statements.InvokeMethod;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runners.model.MultipleFailureException;
import org.junit.runners.model.Statement;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

class CustomWatcher implements TestRule {
    private final GradedTests parent;
    public CustomWatcher(GradedTests parent) {
        this.parent = parent;
    }

    @Override
    public Statement apply(Statement base, Description description) {
        return new Statement() {
            public void evaluate() throws Throwable {
                List<Throwable> errors = new ArrayList();

                try {
                    base.evaluate();
                    CustomWatcher.this.succeededQuietly(base, description, errors);
                } catch (AssumptionViolatedException var7) {
                    errors.add(var7);
                    CustomWatcher.this.skippedQuietly(base, var7, description, errors);
                } catch (Throwable var8) {
                    errors.add(var8);
                    CustomWatcher.this.failedQuietly(base, var8, description, errors);
                }

                MultipleFailureException.assertEmpty(errors);
            }
        };
    }

    private void succeededQuietly(Statement base, Description description, List<Throwable> errors) {
        try {
            this.succeeded(base, description);
        } catch (Throwable var4) {
            errors.add(var4);
        }

    }

    private void failedQuietly(Statement base, Throwable e, Description description, List<Throwable> errors) {
        try {
            this.failed(base, e, description);
        } catch (Throwable var5) {
            errors.add(var5);
        }
    }

    private void skippedQuietly(Statement base, AssumptionViolatedException e, Description description, List<Throwable> errors) {
        try {
            if (e instanceof org.junit.AssumptionViolatedException) {
                this.skipped(base, (org.junit.AssumptionViolatedException)e, description);
            } else {
                this.skipped(base, e, description);
            }
        } catch (Throwable var5) {
            errors.add(var5);
        }
    }

    protected void succeeded(Statement base, Description description) {
        System.out.println("In");
    }

    protected void failed(Statement base, Throwable e, Description description){
        Feedback annotation = description.getAnnotation(Feedback.class);
        if(annotation == null)
            return;

        InvokeMethod im = (InvokeMethod) base;

        try {
            Constructor<? extends Feedback.FeedbackClass<?>> c = annotation.use().getDeclaredConstructor();
            c.setAccessible(true);
            String out = c.newInstance().apply(im.target, TestStatus.FAILED);
            System.out.println(out);
        } catch (Exception e1) {
            e1.printStackTrace();
        }
    }

    protected void skipped(Statement base, org.junit.AssumptionViolatedException e, Description description) {
    }
}

public abstract class GradedTests {
    @Rule
    public final TestRule watchman = new CustomWatcher(this);
    public static HashMap<Description, String> feedbacks = new HashMap<>();
}
