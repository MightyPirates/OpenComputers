package mods.immibis.redlogic.api.wiring;

/**
 * Interface implemented by tile entities that receive redstone updates.
 * Insulated wire does not cause block updates when it changes state.
 * Tile entities that connect to insulated wire must implement this if they need notification of state changes.
 * Bare red alloy wire causes block updates and does not call onRedstoneInputChanged.
 */
public interface IRedstoneUpdatable {
	public void onRedstoneInputChanged();
}
