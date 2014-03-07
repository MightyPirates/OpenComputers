package mods.immibis.redlogic.api.wiring;

/**
 * Implemented by tile entities that need to be notified when a connected bundled cable changes state.
 */
public interface IBundledUpdatable {
	public void onBundledInputChanged();
}
