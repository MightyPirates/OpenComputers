package li.cil.oc.api.nanomachines;

/**
 * Enum with reasons why a nanomachine behavior was disabled.
 * <p/>
 * This allows some more context specific behavior in a more stable fashion.
 */
public enum DisableReason {
    /**
     * This covers things like players logging off or the controller being reset.
     */
    Default,

    /**
     * Input state changed, leading to a behavior being disabled.
     */
    InputChanged,

    /**
     * System has run out of energy and is powering down.
     */
    OutOfEnergy
}
