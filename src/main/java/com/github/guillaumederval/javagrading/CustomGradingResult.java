package com.github.guillaumederval.javagrading;

import static java.lang.Double.NaN;

/**
 * Allow a test to return a custom feedback
 */
public class CustomGradingResult extends Exception {
    public final String feedback;
    public final TestStatus status;
    public final double grade;
    public final Exception origException;

    /**
     * @param status test status
     * @param grade the grade. must be NaN to avoid defining a custom grade. Always set to NaN when status == IGNORED
     * @param feedback a string describing the feedback, or null
     * @param origException original exception, or null
     */
    public CustomGradingResult(TestStatus status, double grade, String feedback, Exception origException) {
        this.feedback = feedback;
        this.status = status;
        if(status != TestStatus.IGNORED)
            this.grade = grade;
        else
            this.grade = NaN;
        this.origException = origException;
    }

    public CustomGradingResult(TestStatus status, double grade, String feedback) {
        this(status, grade, feedback, null);
    }

    public CustomGradingResult(TestStatus status, double grade) {
        this(status, grade, null, null);
    }

    public CustomGradingResult(TestStatus status, String feedback) {
        this(status, NaN, feedback,null);
    }

    public CustomGradingResult(TestStatus status, String feedback, Exception origException) {
        this(status, NaN, feedback, origException);
    }

    public CustomGradingResult(TestStatus status, double grade, Exception origException) {
        this(status, grade, null, origException);
    }

    public CustomGradingResult(TestStatus status, Exception origException) {
        this(status, NaN, null, origException);
    }

    public CustomGradingResult(TestStatus status) {
        this(status, NaN, null, null);
    }
}
