import com.github.guillaumederval.javagrading.Grade;
import com.github.guillaumederval.javagrading.GradeClass;
import com.github.guillaumederval.javagrading.GradingRunnerWithParametersFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GradingRunnerWithParametersFactory.class)
@GradeClass(totalValue = 100)
public class ParametersTests {
    @Parameterized.Parameters
    public static Collection numbers() {
        return Arrays.asList(new Object[][] {
                { 1 },
                { 2 },
                { 3 },
                { 4 },
                { 5 }
        });
    }

    int param;
    public ParametersTests(int param) {
        this.param = param;
    }

    @Test
    @Grade(value = 1)
    public void mytest() throws Exception {
        if(param % 2 != 0)
            throw new Exception("not even");
    }
}
