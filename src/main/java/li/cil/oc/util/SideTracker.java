package li.cil.oc.util;

import net.minecraftforge.forgespi.Environment;

import java.util.Collections;
import java.util.Set;

public final class SideTracker {
    private static final Set<Thread> serverThreads = Collections.newSetFromMap(new java.util.WeakHashMap<Thread, Boolean>());

    public static void addServerThread() {
        serverThreads.add(Thread.currentThread());
    }

    public static boolean isServer() {
        return Environment.get().getDist().isDedicatedServer() || serverThreads.contains(Thread.currentThread());
    }

    public static boolean isClient() {
        return !isServer();
    }
}
