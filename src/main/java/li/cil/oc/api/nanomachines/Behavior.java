package li.cil.oc.api.nanomachines;

/**
 * Implemented by single behaviors.
 * <p/>
 * If you need a reference to the player this behavior applies to (which you'll
 * probably usually want to have), pass it along from {@link BehaviorProvider#createBehaviors}.
 */
public interface Behavior {
    /**
     * A short name / description of this behavior.
     * <p/>
     * You can <em>not</em> use commas (<tt>,</tt>) or double quotes (<tt>"</tt>)
     * in the returned string. If you do, they'll automatically be replaced with
     * underscores.
     * <p/>
     * This is entirely optional and may even return <tt>null</tt>. It is made
     * accessible via the controller's wireless protocol, to allow better
     * automating reconfigurations / determining input mappings. In some cases
     * you may not wish to make this possible, in those cases return <tt>null</tt>
     * or a random string.
     * <p/>
     * Again, you can return whatever you like here, it's not used in mod internal
     * logic, but only provided to ingame devices as a hint to make configuring
     * nanomachines a little easier.
     *
     * @return the name to provide for this behavior, if any.
     */
    String getNameHint();

    /**
     * Called when this behavior becomes active because all its required inputs
     * are now satisfied.
     * <p/>
     * Use this to initialize permanent effects.
     */
    void onEnable();

    /**
     * Called when this behavior becomes inactive.
     * <p/>
     * Use this to remove permanent effects.
     *
     * @param reason the reason the behavior is being disabled.
     */
    void onDisable(DisableReason reason);

    /**
     * Called each tick while this behavior is active.
     */
    void update();
}
