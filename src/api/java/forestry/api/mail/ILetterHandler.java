package forestry.api.mail;

import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public interface ILetterHandler {
	IPostalState handleLetter(World world, String recipient, ItemStack letterStack, boolean doLodge);
}
