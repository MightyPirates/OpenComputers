package li.cil.oc.api.event;

import li.cil.oc.api.internal.Agent;

/**
 * This event is fired when a robot tries to place a block and has no point of
 * reference, i.e. the place would have to be placed in "thin air". Per default
 * this fails (because players can't do this, either).
 * <p/>
 * This is primarily intended for the 'Angel Upgrade', but it might be useful
 * for other upgrades, too.
 */
public class RobotPlaceInAirEvent extends RobotEvent {
    private boolean isAllowed = false;

    public RobotPlaceInAirEvent(Agent agent) {
        super(agent);
    }

    /**
     * Whether the placement is allowed. Defaults to <tt>false</tt>.
     */
    public boolean isAllowed() {
        return isAllowed;
    }

    /**
     * Set whether the placement is allowed, can be used to allow robots to
     * place blocks in thin air.
     */
    public void setAllowed(boolean value) {
        this.isAllowed = value;
    }
}
