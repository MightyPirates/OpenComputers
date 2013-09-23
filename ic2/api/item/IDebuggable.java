package ic2.api.item;



/**
 * Allows a tile entity to output a debug message when the debugItem is used on it.
 * Suggestions by Myrathi
 */
public abstract interface IDebuggable {
    /**
     * Checks if the tile entity is in a state that can be debugged.
     *
     * @return True if the tile entity can be debugged
     */
    public abstract boolean isDebuggable();

    /**
     * Gets the debug text for the tile entity.
     *
     * @return The text that the debugItem should show
     */
    public abstract String getDebugText();
}
