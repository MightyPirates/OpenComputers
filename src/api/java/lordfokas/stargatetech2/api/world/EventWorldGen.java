package lordfokas.stargatetech2.api.world;

import net.minecraft.world.World;
import cpw.mods.fml.common.eventhandler.Event;
import cpw.mods.fml.common.eventhandler.Event.HasResult;

@HasResult
public class EventWorldGen extends Event {

	public final World world;
	public final int chunkX;
	public final int chunkZ;
	public final GenType type;
	
	public EventWorldGen(World world, int cX, int cZ, GenType type) {
		this.world = world;
		this.chunkX = cX;
		this.chunkZ = cZ;
		this.type = type;
	}
	
	public static enum GenType {
		STARGATE,
		LOOT_POD,
		VEIN_NAQUADAH;
	}

}
