package li.cil.oc.common.item

import java.util

import li.cil.oc.Constants
import li.cil.oc.Settings
import li.cil.oc.util.Color
import net.minecraft.client.resources.model.ModelBakery
import net.minecraft.client.resources.model.ModelResourceLocation
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly

class FloppyDisk(val parent: Delegator) extends traits.Delegate with CustomModel {
  // Necessary for anonymous subclasses used for loot disks.
  override def unlocalizedName = "FloppyDisk"

  override protected def tooltipName = None

  override def tooltipLines(stack: ItemStack, player: EntityPlayer, tooltip: util.List[String], advanced: Boolean) = {
    if (stack.hasTagCompound && stack.getTagCompound.hasKey(Settings.namespace + "data")) {
      val nbt = stack.getTagCompound.getCompoundTag(Settings.namespace + "data")
      if (nbt.hasKey(Settings.namespace + "fs.label")) {
        tooltip.add(nbt.getString(Settings.namespace + "fs.label"))
      }
    }
    super.tooltipLines(stack, player, tooltip, advanced)
  }

  @SideOnly(Side.CLIENT)
  private def modelLocationFromDyeName(name: String) = {
    new ModelResourceLocation(Settings.resourceDomain + ":" + Constants.ItemName.Floppy + "_" + name, "inventory")
  }

  @SideOnly(Side.CLIENT)
  override def getModelLocation(stack: ItemStack): ModelResourceLocation = {
    val dyeIndex =
      if (stack.hasTagCompound && stack.getTagCompound.hasKey(Settings.namespace + "color"))
        stack.getTagCompound.getInteger(Settings.namespace + "color")
      else
        8
    modelLocationFromDyeName(Color.dyes(dyeIndex max 0 min 15))
  }

  @SideOnly(Side.CLIENT)
  override def registerModelLocations(): Unit = {
    for (dyeName <- Color.dyes) {
      val location = modelLocationFromDyeName(dyeName)
      ModelBakery.addVariantName(parent, location.getResourceDomain + ":" + location.getResourcePath)
    }
  }
}
