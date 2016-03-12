package li.cil.oc.common.recipe

import net.minecraft.inventory.InventoryCrafting
import net.minecraft.item.crafting.IRecipe

trait ContainerItemAwareRecipe extends IRecipe {
  override def getRemainingItems(inv: InventoryCrafting) =
    (0 until inv.getSizeInventory).
      map(inv.getStackInSlot).
      map(net.minecraftforge.common.ForgeHooks.getContainerItem).
      filter(_ != null).
      toArray
}
