import com.github.guillaumederval.javagrading.*;
import com.github.guillaumederval.javagrading.utils.PrintPermission;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Permissions;

@RunWith(GradingRunner.class)
@GradeClass(totalValue = 100)
public class PermissionTest {
    @Test()
    @Grade(value = 5.0, customPermissions = MyPerms1.class)
    public void allowPrint() {
        System.out.println("I was allowed to print!");
    }

    @Test()
    @Grade(value = 5.0, customPermissions = MyPerms2.class)
    public void allowThread() {
        Thread t = new Thread() {
            @Override
            public void run() {
                // nothing
            }
        };
        t.start();
    }

    /*
       NOTE: the class MUST be public AND static (if it is an inner class) for this to work.
       Namely, it must have an accessible constructor without args.
     */
    public static class MyPerms1 implements Grade.PermissionCollectionFactory {
        @Override
        public PermissionCollection get() {
            Permissions perms = new Permissions();
            perms.add(PrintPermission.instance);
            return perms;
        }
    }

    public static class MyPerms2 implements Grade.PermissionCollectionFactory {
        @Override
        public PermissionCollection get() {
            Permissions perms = new Permissions();
            perms.add(new RuntimePermission("modifyThreadGroup"));
            perms.add(new RuntimePermission(("modifyThread")));
            return perms;
        }
    }
}

