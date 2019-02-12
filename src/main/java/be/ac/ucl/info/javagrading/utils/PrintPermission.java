package be.ac.ucl.info.javagrading.utils;

import java.security.Permission;

public class PrintPermission extends Permission {
    public PrintPermission() {
        super("print");
    }

    @Override
    public boolean implies(Permission permission) {
        return equals(permission);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof PrintPermission;
    }

    @Override
    public int hashCode() {
        return 568929722;
    }

    @Override
    public String getActions() {
        return null;
    }

    public static final PrintPermission instance = new PrintPermission();
}
