package li.cil.oc.api.machine;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import java.lang.annotation.*;

/**
 * This interface abstracts away any language specific details for the Machine.
 * <p/>
 * This allows the introduction of other languages, e.g. computers that run
 * assembly or some other language interpreter. The two architectures included
 * in OpenComputers are the native Lua architecture (using native LuaC) and the
 * Java Lua architecture (using LuaJ).
 */
public interface Architecture {
    /**
     * Used to check if the machine is fully initialized. If this is false no
     * signals for detected components will be generated. Avoids duplicate
     * signals if <tt>component_added</tt> signals are generated in the
     * language's startup script, for already present components (see Lua's
     * init.lua script).
     * <p/>
     * This is also used to check whether limits on direct calls should be
     * enforced or not - this allows a quick boot phase in the language's
     * kernel logic before switching to business-as-usual.
     *
     * @return whether the machine is fully initialized.
     */
    boolean isInitialized();

    /**
     * This is called when the amount of memory in the machine may have changed.
     * This is usually triggered by the owner when its composition changes. For
     * example this is called from computer cases' onInventoryChanged method.
     * <p/>
     * The amount of memory should be computed from the list of components given.
     * The architecture should immediately apply the new memory size
     *
     * @param components the components to use for computing the total memory.
     * @return whether any memory is present at all.
     */
    boolean recomputeMemory(Iterable<ItemStack> components);

    /**
     * Called when a machine starts up. Used to (re-)initialize the underlying
     * architecture logic. For example, for Lua this creates a new Lua state.
     * <p/>
     * This also sets up any built-in APIs for the underlying language, such as
     * querying available memory, listing and interacting with components and so
     * on. If this returns <tt>false</tt> the machine fails to start.
     * <p/>
     * Note that the owning machine has not necessarily been connected to a
     * network when this is called, in case this is called from the machine's
     * load logic. Use {@link #onConnect()} for additional initialization that
     * depends on a node network (such as connecting a ROM file system).
     *
     * @return whether the architecture was initialized successfully.
     */
    boolean initialize();

    /**
     * Called when a machine stopped. Used to clean up any handles, memory and
     * so on. For example, for Lua this destroys the Lua state.
     */
    void close();

    /**
     * Performs a synchronized call initialized in a previous call to
     * {@link #runThreaded(boolean)}.
     * <p/>
     * This method is invoked from the main server thread, meaning it is safe
     * to interact with the world without having to perform manual
     * synchronization.
     * <p/>
     * This method is expected to leave the architecture in a state so it is
     * prepared to next be called with <tt>runThreaded(true)</tt>. For example,
     * the Lua architecture will leave the results of the synchronized call on
     * the stack so they can be further processed in the next call to
     * <tt>runThreaded</tt>.
     */
    void runSynchronized();

    /**
     * Continues execution of the machine. The first call may be used to
     * initialize the machine (e.g. for Lua we load the libraries in the first
     * call so that the computers boot faster). After that the architecture
     * <em>should</em> return <tt>true</tt> from {@link #isInitialized()}.
     * <p/>
     * The resumed state is either a return from a synchronized call, when a
     * synchronized call has been completed (via <tt>runSynchronized</tt>), or
     * a normal yield in all other cases (sleep, interrupt, boot, ...).
     * <p/>
     * This is expected to return within a very short time, usually. For example,
     * in Lua this returns as soon as the state yields, and returns at the latest
     * when the Settings.timeout is reached (in which case it forces the state
     * to crash).
     * <p/>
     * This is expected to consume a single signal if one is present and return.
     * If returning from a synchronized call this should consume no signal.
     *
     * @param isSynchronizedReturn whether the architecture is resumed from an
     *                             earlier synchronized call. In the case of
     *                             Lua this means the results of the call are
     *                             now on the stack, for example.
     * @return the result of the execution. Used to determine the new state.
     */
    ExecutionResult runThreaded(boolean isSynchronizedReturn);

    /**
     * Called when a new signal is queued in the hosting {@link Machine}.
     * <p/>
     * Depending on how you structure your architecture, you may not need this
     * callback. For example, the Lua architectures simply pull the next signal
     * from the queue whenever {@link #runThreaded} is called again. However,
     * if you'd like to react to signals in a more timely manner, you can
     * react to this <em>while</em> you are in a {@link #runThreaded} call,
     * which is what it is intended to be used for.
     * <p/>
     * Keep in mind that this may be called from any random thread, since
     * {@link Context#signal} does not require being called from a specific
     * thread.
     */
    void onSignal();

    /**
     * Called when the owning machine was connected to the component network.
     * <p/>
     * This can be useful for connecting custom file systems (read only memory)
     * in case {@link #initialize()} was called from the machine's load logic
     * (where it was not yet connected to the network).
     */
    void onConnect();

    /**
     * Restores the state of this architecture as previously saved in
     * {@link #save(NBTTagCompound)}. The architecture should be in the same
     * state it was when it was saved after this, so it can be resumed from
     * whatever state the owning machine was in when it was saved.
     *
     * @param nbt the tag compound to save to.
     */
    void load(NBTTagCompound nbt);

    /**
     * Saves the architecture for later restoration, e.g. across games or chunk
     * unloads. Used to persist a machine's execution state. For native Lua this
     * uses the Eris library to persist the main coroutine, for example.
     * <p/>
     * Note that the tag compound is shared with the Machine.
     *
     * @param nbt the tag compound to save to.
     */
    void save(NBTTagCompound nbt);

    /**
     * Architectures can be annotated with this to provide a nice display name.
     * <p/>
     * This is used when the name of an architecture has to be displayed to the
     * user, such as when cycling architectures on a CPU.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @interface Name {
        String value();
    }

    /**
     * Architectures flagged with this annotation can potentially run without
     * any additional memory installed in the computer.
     * <p/>
     * Use this to allow assembly of devices such as microcontrollers without
     * any memory being installed in them while your architecture is being
     * used by the CPU being installed. Note to actually make the machine
     * start up you only need to always return <tt>true</tt> from
     * {@link #recomputeMemory}.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @Inherited
    @interface NoMemoryRequirements {
    }
}
