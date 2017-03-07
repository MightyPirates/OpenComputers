package li.cil.oc.common.item

import li.cil.oc.Constants
import li.cil.oc.Settings
import li.cil.oc.util.DyeUtils
import net.minecraft.client.renderer.block.model.ModelBakery
import net.minecraft.client.renderer.block.model.ModelResourceLocation
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.util.ResourceLocation
import net.minecraft.util.math.BlockPos
import net.minecraft.world.IBlockAccess
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
    modelLocationFromDyeName(DyeUtils.dyes(dyeIndex max 0 min 15))
  }

  @SideOnly(Side.CLIENT)
  override def registerModelLocations(): Unit = {
    for (dyeName <- DyeUtils.dyes) {
      val location = modelLocationFromDyeName(dyeName)
      ModelBakery.registerItemVariants(parent, new ResourceLocation(location.getResourceDomain + ":" + location.getResourcePath))
    }
  }

  override def doesSneakBypassUse(world: IBlockAccess, pos: BlockPos, player: EntityPlayer): Boolean = true
}
