package li.cil.oc.common.item.traits

import java.util

import li.cil.oc.CreativeTab
import li.cil.oc.Localization
import li.cil.oc.Settings
import li.cil.oc.client.KeyBindings
import li.cil.oc.common.tileentity
import li.cil.oc.util.ItemCosts
import li.cil.oc.util.Tooltip
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.util.math.BlockPos
import net.minecraft.world.IBlockAccess
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly

trait SimpleItem extends Item {
  setCreativeTab(CreativeTab)

  def createItemStack(amount: Int = 1) = new ItemStack(this, amount)

  override def isBookEnchantable(stack: ItemStack, book: ItemStack) = false

  override def doesSneakBypassUse(stack: ItemStack, world: IBlockAccess, pos: BlockPos, player: EntityPlayer) = {
    world.getTileEntity(pos) match {
      case drive: tileentity.TileEntityDiskDrive => true
      case _ => super.doesSneakBypassUse(stack, world, pos, player)
    }
  }

  @SideOnly(Side.CLIENT)
  override def addInformation(stack: ItemStack, player: EntityPlayer, tooltip: util.List[String], advanced: Boolean): Unit = {
    tooltip.addAll(Tooltip.get(getClass.getSimpleName))

    if (ItemCosts.hasCosts(stack)) {
      if (KeyBindings.showMaterialCosts) {
        ItemCosts.addTooltip(stack, tooltip)
      }
      else {
        tooltip.add(Localization.localizeImmediately(
          Settings.namespace + "tooltip.MaterialCosts",
          KeyBindings.getKeyBindingName(KeyBindings.materialCosts)))
      }
    }
    if (stack.hasTagCompound && stack.getTagCompound.hasKey(Settings.namespace + "data")) {
      val data = stack.getTagCompound.getCompoundTag(Settings.namespace + "data")
      if (data.hasKey("node") && data.getCompoundTag("node").hasKey("address")) {
        tooltip.add("§8" + data.getCompoundTag("node").getString("address").substring(0, 13) + "...§7")
      }
    }
  }
}
