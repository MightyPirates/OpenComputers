package li.cil.oc.common.item

import li.cil.oc.Constants
import li.cil.oc.Settings
import li.cil.oc.util.Color
import net.minecraft.client.renderer.model.ModelBakery
import net.minecraft.client.renderer.model.ModelResourceLocation
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.util.ResourceLocation
import net.minecraft.util.math.BlockPos
import net.minecraft.world.IBlockReader
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import net.minecraftforge.client.model.ModelLoader

class FloppyDisk(val parent: Delegator) extends traits.Delegate with CustomModel with traits.FileSystemLike {
  // Necessary for anonymous subclasses used for loot disks.
  override def unlocalizedName = "floppydisk"

  val kiloBytes = Settings.get.floppySize

  @OnlyIn(Dist.CLIENT)
  private def modelLocationFromDyeName(name: String) = {
    new ModelResourceLocation(Settings.resourceDomain + ":" + Constants.ItemName.Floppy + "_" + name, "inventory")
  }

  @OnlyIn(Dist.CLIENT)
  override def getModelLocation(stack: ItemStack): ModelResourceLocation = {
    val dyeIndex =
      if (stack.hasTag && stack.getTag.contains(Settings.namespace + "color"))
        stack.getTag.getInt(Settings.namespace + "color")
      else
        8
    modelLocationFromDyeName(Color.dyes(dyeIndex max 0 min 15))
  }

  @OnlyIn(Dist.CLIENT)
  override def registerModelLocations(): Unit = {
    for (dyeName <- Color.dyes) {
      val location = modelLocationFromDyeName(dyeName)
      ModelLoader.addSpecialModel(location)
    }
  }

  override def doesSneakBypassUse(world: IBlockReader, pos: BlockPos, player: PlayerEntity): Boolean = true
}
