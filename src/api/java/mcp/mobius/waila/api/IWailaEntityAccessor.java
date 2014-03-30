package mcp.mobius.waila.api;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

/* The Accessor is used to get some basic data out of the game without having to request
 * direct access to the game engine.
 * It will also return things that are unmodified by the overriding systems (like getWailaStack).
 */

public interface IWailaEntityAccessor {
	World        		 getWorld();
	EntityPlayer 		 getPlayer();
	Entity               getEntity();
	MovingObjectPosition getPosition();
	Vec3                 getRenderingPosition();
	NBTTagCompound       getNBTData();
	int                  getNBTInteger(NBTTagCompound tag, String keyname);
	double               getPartialFrame();
}
