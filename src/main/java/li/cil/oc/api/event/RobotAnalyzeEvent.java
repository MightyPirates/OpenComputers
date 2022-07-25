package li.cil.oc.api.event;

import li.cil.oc.api.internal.Agent;
import net.minecraft.entity.player.PlayerEntity;

/**
 * Fired when an analyzer is used on a robot.
 * <p/>
 * Use this to echo additional information for custom components.
 */
public class RobotAnalyzeEvent extends RobotEvent {
    /**
     * The player that used the analyzer.
     */
    public final PlayerEntity player;

    public RobotAnalyzeEvent(Agent agent, PlayerEntity player) {
        super(agent);
        this.player = player;
    }
}
