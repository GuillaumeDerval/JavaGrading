package com.github.guillaumederval.javagrading;

import org.junit.Test;
import org.junit.internal.runners.statements.FailOnTimeout;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;

import java.util.concurrent.TimeUnit;

/**
 * Custom runner that handles CPU timeouts and stdout/err.
 */
public class GradingRunner extends BlockJUnit4ClassRunner {
    public GradingRunner(Class<?> klass) throws InitializationError {
        super(klass);
    }

    @Override
    protected Statement methodInvoker(FrameworkMethod method, Object test) {
        return GradingRunnerUtils.methodInvoker(method, super.methodInvoker(method, test));
    }

    @Override
    protected Statement methodBlock(FrameworkMethod method) {
        return GradingRunnerUtils.methodBlock(method, super.methodBlock(method));
    }

    @Override
    protected Statement withPotentialTimeout(FrameworkMethod method,
                                             Object test, Statement next) {
        return GradingRunnerUtils.withPotentialTimeout(method, test, next);
    }
}
