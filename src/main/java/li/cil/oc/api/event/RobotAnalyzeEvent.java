package li.cil.oc.api.event;

import li.cil.oc.api.internal.Agent;
import net.minecraft.entity.player.EntityPlayer;

/**
 * Fired when an analyzer is used on a robot.
 * <p/>
 * Use this to echo additional information for custom components.
 */
public class RobotAnalyzeEvent extends RobotEvent {
    /**
     * The player that used the analyzer.
     */
    public final EntityPlayer player;

    public RobotAnalyzeEvent(Agent agent, EntityPlayer player) {
        super(agent);
        this.player = player;
    }
}
