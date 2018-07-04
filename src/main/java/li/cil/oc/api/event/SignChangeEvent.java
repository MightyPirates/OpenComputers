package li.cil.oc.api.event;

import net.minecraft.tileentity.TileEntitySign;
import net.minecraftforge.fml.common.eventhandler.Cancelable;
import net.minecraftforge.fml.common.eventhandler.Event;

/**
 * A bit more specific sign change event that holds information about new text of the sign. Used in the sign upgrade.
 */
public abstract class SignChangeEvent extends Event {
    public final TileEntitySign sign;
    public final String[] lines;

    private SignChangeEvent(TileEntitySign sign, String[] lines) {
        this.sign = sign;
        this.lines = lines;
    }

    @Cancelable
    public static class Pre extends SignChangeEvent {
        public Pre(TileEntitySign sign, String[] lines) {
            super(sign, lines);
        }
    }

    public static class Post extends SignChangeEvent {
        public Post(TileEntitySign sign, String[] lines) {
            super(sign, lines);
        }
    }
}
