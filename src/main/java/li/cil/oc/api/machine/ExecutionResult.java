package li.cil.oc.api.machine;

/**
 * Used by the Machine to determine the result of a call to
 * {@link Architecture#runThreaded(boolean)}.
 * <p/>
 * Do not implement this interface, only use the predefined internal classes.
 */
public abstract class ExecutionResult {
    /**
     * Indicates the machine may sleep for the specified number of ticks. This
     * is merely considered a suggestion. If signals are in the queue or are
     * pushed to the queue while sleeping, the sleep will be interrupted and
     * {@link Architecture#runThreaded(boolean)} will be called so that the next
     * signal is pushed.
     */
    public static final class Sleep extends ExecutionResult {
        /**
         * The number of ticks to sleep.
         */
        public final int ticks;

        public Sleep(int ticks) {
            this.ticks = ticks;
        }
    }

    /**
     * Indicates tha the computer should shutdown or reboot.
     */
    public static final class Shutdown extends ExecutionResult {
        /**
         * Whether to reboot. If false the computer will stop.
         */
        public final boolean reboot;

        public Shutdown(boolean reboot) {
            this.reboot = reboot;
        }
    }

    /**
     * Indicates that a synchronized call should be performed. The architecture
     * is expected to be in a state that allows the next call to be to
     * {@link Architecture#runSynchronized()} instead of
     * {@link Architecture#runThreaded(boolean)}. This is used to perform calls
     * from the server's main thread, to avoid threading issues when interacting
     * with other objects in the world.
     */
    public static final class SynchronizedCall extends ExecutionResult {
    }

    /**
     * Indicates that an error occurred and the computer should crash.
     */
    public static final class Error extends ExecutionResult {
        /**
         * The error message.
         */
        public final String message;

        public Error(String message) {
            this.message = message;
        }
    }
}
