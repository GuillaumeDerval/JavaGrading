# JavaGrading: grading java made simple

Simply grade student assignments made in Java or anything that runs on the JVM (Scala/Kotlin/Jython/...).

```java
@Test
@Grade(value = 5, cpuTimeout=1000)
@GradeFeedback("Are you sure your code is in O(n) ?", onTimeout=true)
@GradeFeedback("Sorry, something is wrong with your algorithm", onFail=true)
void yourtest() {
    //a test for the student's code
}
```

Features:
- CPU timeouts on the code
- Jails student code
    - No I/O, including stdout/err
    - No thread creating by the student, ...
    - Most things involving syscalls are forbidden
    - specific permissions can be added on specifics tests if needed
- Text/RST reporting
- Custom feedback, both from outside the test (onFail, onTimeout, ...) but also from inside (see below).

We use this library at [UCLouvain](https://www.uclouvain.be) in the following courses:
- _Data Structures and Algorithms_ (LSINF1121)
- _Computer Science 2_ (LEPL1402)
- _Constraint Programming_ (LING2365)

This library is best used with an autograder, such as [INGInious](https://github.com/UCL-INGI/INGInious).

## Example

Add the `@Grade` annotation on your JUnit test like this:
```java
@RunWith(GradingRunner.class)
public class MyTests {
    @Test
    @Grade(value = 5)
    void mytest1() {
        //this works
        something();
    }
    
    @Test
    @Grade(value = 3)
    @GradeFeedback("You forgot to consider this particular case [...]", onFail=true)
    void mytest2() {
        //this doesn't
        somethingelse();
    }
}
```
Note that we demonstrate here the usage of the `@GradeFeedback` annotation, that allows to give feedback to the students.

You can then run the tests using this small boilerplate:
```java
public class RunTests {
    public static void main(String args[]) {
        JUnitCore runner = new JUnitCore();
        runner.addListener(new GradingListener(false));
        runner.run(MyTests.class);
    }
}
```

This will print the following on the standard output:
```
--- GRADE ---
- class MyTests 8/8
	mytest1(StdTests) SUCCESS 5/5
	ignored(StdTests) FAILED 0/3
	    You forgot to consider this particular case [...]
TOTAL 5/8
TOTAL WITHOUT IGNORED 5/8
--- END GRADE ---
```

## Documentation & installation

Everything needed is located inside the files:
- [Grade.java](https://github.com/GuillaumeDerval/JavaGrading/blob/master/src/main/java/com/github/guillaumederval/javagrading/Grade.java): annotation for grading a test
- [GradeFeedback.java](https://github.com/GuillaumeDerval/JavaGrading/blob/master/src/main/java/com/github/guillaumederval/javagrading/GradeFeedback.java): annotation to add feedback when a test fails/succeeds/timeouts/is ignored
- [GradeClass.java](https://github.com/GuillaumeDerval/JavaGrading/blob/master/src/main/java/com/github/guillaumederval/javagrading/GradeClass.java): annotation to grade all tests from a class, and to give an overall score
- [CustomGradingResult.java](https://github.com/GuillaumeDerval/JavaGrading/blob/master/src/main/java/com/github/guillaumederval/javagrading/CustomGradingResult.java): exception to be thrown by the "teacher" to give custom score/feedback, if needed

To add it as a dependency of your project, you can add this to your pom.xml in maven:
```xml
<dependency>
  <groupId>com.github.guillaumederval</groupId>
  <artifactId>JavaGrading</artifactId>
  <version>0.5.1</version>
</dependency>
```

If you are not using maven, [search.maven](https://search.maven.org/artifact/com.github.guillaumederval/JavaGrading/0.5.1/jar) probably has the line of code you need.


## Advanced examples

### Cpu timeout
It is (strongly) advised when using an autograder (did I already say that [INGInious](https://github.com/UCL-INGI/INGInious) is a very nice one?)
to put a maximum time to run a test:
```java
@Test
@Grade(value = 5, cpuTimeout=1000)
void yourtest() {
    //a test for the student's code
}
```

If the test runs for more than 1000 milliseconds, it will receive a TIMEOUT error and receive a grade of 0/5.

Note that if you allow the student (via the addition of some permission) to create new threads, the time taken in the new
threads won't be taken into account!

It is also possible to add a wall-clock-time timeout, via JUnit:
```java
@Test(timeout=3000) //kills the test after 3000ms in real, wall-clock time
@Grade(value = 5)
void yourtest() {
    //a test for the student's code
}
```

**By default, setting a CPU timeout also sets a wall-clock timeout at three times the cpu timeout.**
If you want to override that, set a different value to `@Test(timeout=XXX)`.

### Ignored tests
Ignored tests are supported:
```java
@Test
@Grade(value = 5)
void yourtest() {
    Assume.assumeFalse(true); //JUnit function to indicate that the test should be ignored
}
```

### Custom feedback (outside the test)
Use the `@GradeFeedback` annotation to give feedback about specific type of errors
```java
@Test
@Grade(value = 5)
@GradeFeedback("Congrats!", onSuccess=True)
@GradeFeedback("Something is wrong", onFail=True)
@GradeFeedback("Too slow!", onTimeout=True)
@GradeFeedback("We chose to ignore this test", onIgnore=True)
void yourtest() {
    //
}
```

### Custom grade and feedback (inside the test)
Throw the exception `CustomGradingResult` to give a custom grading from inside the text.

In order to avoid that students throw this exception, this feature is disabled by default. You must activate it by 
setting `@Grade(custom=true)` and protect yourself your code against evil students that may throw the exception themselves.

```java
@Test
@Grade(value = 2, cpuTimeout=1000, custom=true)
void yourtest() {
    try {
        //code of the student here
    }
    catch (CustomGradingResult e) {
        throw new CustomGradingResult(TestStatus.FAILED, 0, "Well tried, but we are protected against that");
    }
    
    if(something) {
        throw new CustomGradingResult(TestStatus.FAILED, 1, "Sadly, you are not *completely* right.");
    }
    else if(somethingelse) {
        throw new CustomGradingResult(TestStatus.FAILED, 1.5, "Still not there!");
    }
    else if(somethingentirelydifferent) {
        throw new CustomGradingResult(TestStatus.TIMEOUT, 1.75, "A bit too slow, I'm afraid");
    }
    else if(otherthing) {
        throw new CustomGradingResult(TestStatus.SUCCESS, 2.5, "Good! Take these 0.5 bonus points with you");
    }
    
    //by default, if you throw nothing, it's SUCCESS with the maximum grade
}
```

### RST output
When using an autograder (I may already have told you that [INGInious](https://github.com/UCL-INGI/INGInious) is very nice)
you might want to output something nice (i.e. not text) for the students. JavaGrading can output a nice 
RestructuredText table:

```java
public class RunTests {
    public static void main(String args[]) {
        JUnitCore runner = new JUnitCore();
        runner.addListener(new GradingListener(true)); //notice the *true* here 
        runner.run(MyTests.class);
    }
}
```

![Screenshot of the RST output](https://raw.githubusercontent.com/GuillaumeDerval/JavaGrading/master/rst_screenshot.png "Screenshot of the RST output")

### @GradeClass
The `@GradeClass` annotation allows setting a default grade for all test (avoiding to put @Grade everywhere)
and also to give an overall max grade for the whole class. See next example for... an example.

### Parameterized tests
JUnit's parameterized tests are also supported:

```java
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
```

Output:
```
- class ParametersTests 40/100
	mytest[0](ParametersTests) FAILED 0/20
	mytest[1](ParametersTests) SUCCESS 20/20
	mytest[2](ParametersTests) FAILED 0/20
	mytest[3](ParametersTests) SUCCESS 20/20
	mytest[4](ParametersTests) FAILED 0/20
```

### Multiple test classes

If you have multiple test classes, simply update the main function like this:

```java
public class RunTests {
    public static void main(String args[]) {
        JUnitCore runner = new JUnitCore();
        runner.addListener(new GradingListener(false));
        runner.run(MyTests.class, MyTests2.class, MyOtherTests.class /*, ... */);
    }
}
```
