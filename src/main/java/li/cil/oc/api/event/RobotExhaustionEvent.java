package li.cil.oc.api.event;

import li.cil.oc.api.machine.Robot;

/**
 * Fired when a robot performed an action that would cause exhaustion for a
 * player. Used for the experience upgrade, for example.
 */
public class RobotExhaustionEvent extends RobotEvent {
    /**
     * The amount of exhaustion that was generated.
     */
    public final double exhaustion;

    public RobotExhaustionEvent(Robot robot, double exhaustion) {
        super(robot);
        this.exhaustion = exhaustion;
    }
}
