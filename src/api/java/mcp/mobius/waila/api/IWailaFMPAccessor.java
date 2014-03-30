package mcp.mobius.waila.api;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeDirection;

/* The Accessor is used to get some basic data out of the game without having to request
 * direct access to the game engine.
 * It will also return things that are unmodified by the overriding systems (like getWailaStack).
 */

public interface IWailaFMPAccessor {
	World        		 getWorld();
	EntityPlayer 		 getPlayer();
	TileEntity           getTileEntity();
	MovingObjectPosition getPosition();
	NBTTagCompound       getNBTData();
	NBTTagCompound       getFullNBTData();
	int                  getNBTInteger(NBTTagCompound tag, String keyname);
	double               getPartialFrame();
	Vec3                 getRenderingPosition();
	String               getID();
}
