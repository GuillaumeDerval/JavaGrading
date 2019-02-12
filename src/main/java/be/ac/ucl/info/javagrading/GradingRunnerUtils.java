package be.ac.ucl.info.javagrading;

import be.ac.ucl.info.javagrading.Grade;
import be.ac.ucl.info.javagrading.TestSecurityManager;
import be.ac.ucl.info.javagrading.utils.PrintPermission;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;
import org.junit.runners.model.TestTimedOutException;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.security.*;
import java.security.cert.Certificate;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * Functions common to GradingRunners.
 *
 * Does all the hard work about stdout/stderr and cpu timeouts.
 */
class GradingRunnerUtils {
    public static Statement methodInvoker(FrameworkMethod method, Statement base) {
        return cpu(method, jail(method, base));
    }

    public static Statement methodBlock(FrameworkMethod method, Statement base) {
        return base;
    }

    /**
     * Add a test that verifies that a given test do not take too much cpu time
     */
    private static Statement cpu(final FrameworkMethod method, Statement base) {
        final Grade g = method.getAnnotation(Grade.class);

        if(g != null && g.cputimeout() > 0) {
            return new Statement() {
                @Override
                public void evaluate() throws Throwable {
                    ThreadMXBean thread = ManagementFactory.getThreadMXBean();
                    long start = thread.getCurrentThreadCpuTime();
                    base.evaluate();
                    long end = thread.getCurrentThreadCpuTime();
                    if(g.debug())
                        System.out.println("Function "+ method.toString()+ " took " + ((end-start)/1000000L) + "ms");
                    if(end-start > g.cputimeout()*1000000L)
                        throw new TestTimedOutException(g.cputimeout(), MILLISECONDS);
                }
            };
        }
        else
            return base;
    }

    private static Statement jail(FrameworkMethod method, final Statement base) {
        checkSecurity();

        final Grade g = method.getAnnotation(Grade.class);

        PermissionCollection coll = new Permissions();
        if(g.debug())
            coll.add(PrintPermission.instance);

        ProtectionDomain pd = new ProtectionDomain(new CodeSource(null, (Certificate[]) null), coll);

        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
                    @Override
                    public Object run() throws Exception {
                        try {
                            base.evaluate();
                        } catch (Throwable throwable) {
                            throw (Exception)throwable; //bad. I know.
                        }
                        return null;
                    }
                }, new AccessControlContext(new ProtectionDomain[]{pd}));
            }
        };
    }

    private static void checkSecurity() {
        if(!(System.getSecurityManager() instanceof TestSecurityManager)) {
            try {
                System.setSecurityManager(new TestSecurityManager());
            }
            catch (SecurityException e) {
                System.out.println("/!\\ WARNING: Cannot set a TestSecurityManager as the security manager. Tests may not be jailed properly.");
            }
        }
    }
}
