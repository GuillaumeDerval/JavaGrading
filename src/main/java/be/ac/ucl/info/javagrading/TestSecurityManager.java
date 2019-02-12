package be.ac.ucl.info.javagrading;

import be.ac.ucl.info.javagrading.utils.ThreadPrintStream;
import be.ac.ucl.info.javagrading.utils.ThreadStream;

import java.io.PrintStream;
import java.security.*;

class TestPolicy extends Policy {
    @Override
    public boolean implies(ProtectionDomain domain, Permission permission) {
        return true;
    }
}

public class TestSecurityManager extends SecurityManager {

    private static ThreadGroup rootGroup;

    public TestSecurityManager() {
        System.setOut(new PrintStream(new ThreadStream(System.out)));
        System.setErr(new PrintStream(new ThreadStream(System.err)));
        Policy.setPolicy(new TestPolicy());
    }

    /**
     * Hackfix to forbid creating threads in the root group when you have no rights to create threads
     */
    @Override
    public ThreadGroup getThreadGroup() {
        if (rootGroup == null) {
            rootGroup = getRootGroup();
        }
        return rootGroup;
    }

    private static ThreadGroup getRootGroup() {
        ThreadGroup root = Thread.currentThread().getThreadGroup();
        while (root.getParent() != null) {
            root = root.getParent();
        }
        return root;
    }
}