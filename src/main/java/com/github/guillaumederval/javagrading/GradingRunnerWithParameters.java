package com.github.guillaumederval.javagrading;

import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;
import org.junit.runners.parameterized.BlockJUnit4ClassRunnerWithParameters;
import org.junit.runners.parameterized.TestWithParameters;

class GradingRunnerWithParameters extends BlockJUnit4ClassRunnerWithParameters {

    public GradingRunnerWithParameters(TestWithParameters test) throws InitializationError {
        super(test);
    }

    @Override
    protected Statement methodInvoker(FrameworkMethod method, Object test) {
        return GradingRunnerUtils.methodInvoker(method, super.methodInvoker(method, test));
    }

    @Override
    protected Statement methodBlock(FrameworkMethod method) {
        return GradingRunnerUtils.methodBlock(method, super.methodBlock(method));
    }
}
