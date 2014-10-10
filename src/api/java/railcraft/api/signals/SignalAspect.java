package mods.railcraft.api.signals;

import java.util.Locale;

/**
 * Represents a Signal state.
 *
 * @author CovertJaguar <http://www.railcraft.info>
 */
public enum SignalAspect {

    /**
     * The All Clear.
     */
    GREEN(0),
    /**
     * Typically means pairing in progress.
     */
    BLINK_YELLOW(1),
    /**
     * Caution, cart heading away.
     */
    YELLOW(1),
    /**
     * Maintenance warning, the signal is malfunctioning.
     */
    BLINK_RED(2),
    /**
     * Stop!
     */
    RED(2),
    /**
     * Can't happen, really it can't (or shouldn't). Only used when rendering
     * blink states (for the texture offset).
     */
    OFF(3);
    private final int textureIndex;
    private static boolean blinkState;
    private static final int SIGNAL_BRIGHTNESS = 210;
    public static final SignalAspect[] VALUES = values();

    private SignalAspect(int textureIndex) {
        this.textureIndex = textureIndex;
    }

    /**
     * Returns the texture offset for this specific aspect.
     *
     * @return offset
     */
    public int getTextureIndex() {
        return textureIndex;
    }

    /**
     * Returns the texture brightness for this specific aspect.
     *
     * @return brightness
     */
    public int getTextureBrightness() {
        if (this == OFF) return -1;
        return SIGNAL_BRIGHTNESS;
    }

    /**
     * Returns true if the aspect is one of the blink states.
     *
     * @return true if blinks
     */
    public boolean isBlinkAspect() {
        if (this == BLINK_YELLOW || this == BLINK_RED)
            return true;
        return false;
    }

    /**
     * Returns true if the aspect should emit light. The return value varies for
     * Blink states.
     *
     * @return true if emitting light.
     */
    public boolean isLit() {
        if (this == OFF)
            return false;
        if (isBlinkAspect())
            return isBlinkOn();
        return true;
    }

    /**
     * Return true if the light is currently off.
     *
     * @return true if the light is currently off.
     */
    public static boolean isBlinkOn() {
        return blinkState;
    }

    /**
     * Don't call this, its used to change blink states by Railcraft.
     */
    public static void invertBlinkState() {
        blinkState = !blinkState;
    }

    /**
     * Takes an Ordinal and returns the corresponding SignalAspect.
     *
     * @param ordinal
     * @return the Signal Aspect with the specified Ordinal
     */
    public static SignalAspect fromOrdinal(int ordinal) {
        if (ordinal < 0 || ordinal >= VALUES.length)
            return SignalAspect.RED;
        return VALUES[ordinal];
    }

    /**
     * Tests two Aspects and determines which is more restrictive. The concept
     * of "most restrictive" refers to which aspect enforces the most
     * limitations of movement to a train.
     *
     * In Railcraft the primary use is in Signal Box logic.
     *
     * @param first
     * @param second
     * @return The most restrictive Aspect
     */
    public static SignalAspect mostRestrictive(SignalAspect first, SignalAspect second) {
        if (first == null && second == null)
            return RED;
        if (first == null)
            return second;
        if (second == null)
            return first;
        if (first.ordinal() > second.ordinal())
            return first;
        return second;
    }

    @Override
    public String toString() {
        String[] sa = name().split("_");
        String out = "";
        for (String s : sa) {
            out = out + s.substring(0, 1) + s.substring(1).toLowerCase(Locale.ENGLISH) + " ";
        }
        out = out.trim();
        return out;
    }

}
