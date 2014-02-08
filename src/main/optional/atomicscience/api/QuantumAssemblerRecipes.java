package atomicscience.api;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

public class QuantumAssemblerRecipes
{
	public static final List<ItemStack> RECIPES = new ArrayList<ItemStack>();

	static
	{
		for (Item item : Item.itemsList)
		{
			if (item != null)
			{
				if (item.itemID > 256)
				{
					ItemStack itemStack = new ItemStack(item);
					addRecipe(itemStack);
				}
			}
		}

		String[] oresToAdd = new String[] { "ingotSteel", "ingotRefinedIron", "ingotUranium", "ingotSilver", "ingotBronze", "ingotTin", "ingotSteeCopper" };

		for (String ore : oresToAdd)
		{
			for (ItemStack itemStack : OreDictionary.getOres(ore))
			{
				addRecipe(itemStack);
			}
		}
	}

	public static boolean hasItemStack(ItemStack itemStack)
	{
		for (ItemStack output : RECIPES)
		{
			if (output.isItemEqual(itemStack))
				return true;
		}
		return false;
	}

	public static void addRecipe(ItemStack itemStack)
	{
		if (itemStack != null)
		{
			if (itemStack.isStackable())
			{
				RECIPES.add(itemStack);
			}
		}
	}
}
