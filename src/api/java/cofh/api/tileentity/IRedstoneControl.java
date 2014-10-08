package cofh.api.tileentity;

/**
 * Implement this interface on Tile Entities which have Redstone Control functionality. This means that a tile can be set to ignore redstone entirely, or
 * respond to a low or high redstone state.
 * 
 * @author King Lemming
 * 
 */
public interface IRedstoneControl extends IRedstoneCache {

	public static enum ControlMode {
		DISABLED(true), LOW(false), HIGH(true);

		private final boolean state;

		private ControlMode(boolean state) {

			this.state = state;
		}

		public boolean isDisabled() {

			return this == DISABLED;
		}

		public boolean isLow() {

			return this == LOW;
		}

		public boolean isHigh() {

			return this == HIGH;
		}

		public boolean getState() {

			return state;
		}

		public static ControlMode stepForward(ControlMode curControl) {

			return curControl == DISABLED ? LOW : curControl == HIGH ? DISABLED : HIGH;
		}

		public static ControlMode stepBackward(ControlMode curControl) {

			return curControl == DISABLED ? HIGH : curControl == HIGH ? LOW : DISABLED;
		}
	}

	void setControl(ControlMode control);

	ControlMode getControl();

}
