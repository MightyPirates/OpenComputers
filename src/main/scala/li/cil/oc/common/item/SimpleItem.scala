package li.cil.oc.common.item

import java.util
import java.util.Random

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
import net.minecraft.util.BlockPos
import net.minecraft.util.WeightedRandomChestContent
import net.minecraft.world.World
import net.minecraftforge.common.ChestGenHooks
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly
import org.lwjgl.input

class SimpleItem extends Item {
  setCreativeTab(CreativeTab)

  // TODO remove
  //  iconString = Settings.resourceDomain + ":" + getClass.getSimpleName

  def createItemStack(amount: Int = 1) = new ItemStack(this, amount)

  override def isBookEnchantable(stack: ItemStack, book: ItemStack) = false

  override def getChestGenBase(chest: ChestGenHooks, rnd: Random, original: WeightedRandomChestContent) = original


  override def doesSneakBypassUse(world: World, pos: BlockPos, player: EntityPlayer) = {
    world.getTileEntity(pos) match {
      case drive: tileentity.DiskDrive => true
      case _ => super.doesSneakBypassUse(world, pos, player)
    }
  }

  @SideOnly(Side.CLIENT)
  override def addInformation(stack: ItemStack, player: EntityPlayer, tooltip: util.List[_], advanced: Boolean): Unit = {
    val tt = tooltip.asInstanceOf[util.List[String]]
    tt.addAll(Tooltip.get(getClass.getSimpleName))

    if (ItemCosts.hasCosts(stack)) {
      if (KeyBindings.showMaterialCosts) {
        ItemCosts.addTooltip(stack, tt)
      }
      else {
        tt.add(Localization.localizeImmediately(
          Settings.namespace + "tooltip.MaterialCosts",
          input.Keyboard.getKeyName(KeyBindings.materialCosts.getKeyCode)))
      }
    }
    if (stack.hasTagCompound && stack.getTagCompound.hasKey(Settings.namespace + "data")) {
      val data = stack.getTagCompound.getCompoundTag(Settings.namespace + "data")
      if (data.hasKey("node") && data.getCompoundTag("node").hasKey("address")) {
        tt.add("§8" + data.getCompoundTag("node").getString("address").substring(0, 13) + "...§7")
      }
    }
  }
}
