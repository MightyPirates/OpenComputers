package li.cil.oc.common.item

import li.cil.oc.Constants
import li.cil.oc.Settings
import li.cil.oc.util.BlockPosition
import li.cil.oc.util.Color
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.client.resources.model.ModelBakery
import net.minecraft.client.resources.model.ModelResourceLocation
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly

class FloppyDisk(val parent: Delegator) extends traits.Delegate with CustomModel with traits.FileSystemLike {
  // Necessary for anonymous subclasses used for loot disks.
  override def unlocalizedName = "FloppyDisk"

  val kiloBytes = Settings.get.floppySize

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

  override def doesSneakBypassUse(position: BlockPosition, player: EntityPlayer): Boolean = true
}
