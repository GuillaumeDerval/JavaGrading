package be.ac.ucl.info.javagrading.utils;

import java.io.PrintStream;

/**
 * Print, or not, depending on the code having PrintPermission or not.
 */
public class PermissionPrintStream extends PrintStream {
    PermissionStream ts;
    public PermissionPrintStream(PrintStream s) {
        super(new PermissionStream(s));
        ts = (PermissionStream)out;
    }
}
