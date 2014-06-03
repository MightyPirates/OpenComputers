package thaumcraft.api.research;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public interface IScanEventHandler {
	ScanResult scanPhenomena(ItemStack stack, World world, EntityPlayer player);
}
