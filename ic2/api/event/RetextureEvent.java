package ic2.api.event;

import net.minecraft.world.World;

import net.minecraftforge.event.Cancelable;
import net.minecraftforge.event.world.WorldEvent;

@Cancelable
public class RetextureEvent extends WorldEvent {
	// target block
	public final int x;
	public final int y;
	public final int z;
	public final int side;

	// referenced block (to grab the texture from)
	public final int referencedBlockId;
	public final int referencedMeta;
	public final int referencedSide;

	// set to true to confirm the operation
	public boolean applied = false;

	public RetextureEvent(World world, int x, int y, int z, int side, int referencedBlockId, int referencedMeta, int referencedSide) {
		super(world);

		this.x = x;
		this.y = y;
		this.z = z;
		this.side = side;
		this.referencedBlockId = referencedBlockId;
		this.referencedMeta = referencedMeta;
		this.referencedSide = referencedSide;
	}
}
