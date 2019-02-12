package be.ac.ucl.info.javagrading.utils;

import java.io.PrintStream;

/**
 * Print, or not, depending on the calling Thread.
 */
public class ThreadPrintStream extends PrintStream {
    ThreadStream ts;
    public ThreadPrintStream(PrintStream s) {
        super(new ThreadStream(s));
        ts = (ThreadStream)out;
    }
}
