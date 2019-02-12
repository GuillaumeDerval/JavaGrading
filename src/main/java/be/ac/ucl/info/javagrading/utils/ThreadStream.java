package be.ac.ucl.info.javagrading.utils;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.HashSet;

import static java.lang.System.getSecurityManager;

public class ThreadStream extends OutputStream {
    PrintStream parent;
    boolean warned;

    public ThreadStream(PrintStream parent) {
        this.parent = parent;
        warned = false;
    }

    @Override
    public void write(int b) throws IOException {
        if(!check())
            return;
        parent.write(b);
    }

    @Override
    public void write(byte b[]) throws IOException {
        if(!check())
            return;
        parent.write(b);
    }

    @Override
    public void write(byte b[], int off, int len) throws IOException {
        if(!check())
            return;
        parent.write(b, off, len);
    }

    @Override
    public void flush() throws IOException {
        if(!check())
            return;
        parent.flush();
    }

    @Override
    public void close() throws IOException {
        if(!check())
            return;
        parent.close();
    }

    private boolean check() {
        SecurityManager sm = getSecurityManager();
        if (sm != null) {
            try {
                sm.checkPermission(PrintPermission.instance);
            }
            catch (SecurityException e) {
                if(!warned) {
                    warned = true;
                    parent.println("WARNING:");
                    parent.println("You use print/println/write on System.out or System.err.");
                    parent.println("It won't work here and slows down your code a lot. Consider removing/commenting these calls.");
                    parent.println();
                }
                return false;
            }
        }
        return true;
    }
}