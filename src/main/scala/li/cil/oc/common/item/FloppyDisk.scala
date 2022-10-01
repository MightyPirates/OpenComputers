package li.cil.oc.common.item

import li.cil.oc.Constants
import li.cil.oc.Settings
import li.cil.oc.util.Color
import net.minecraft.client.renderer.model.ModelBakery
import net.minecraft.client.renderer.model.ModelResourceLocation
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.Item
import net.minecraft.item.Item.Properties
import net.minecraft.item.ItemStack
import net.minecraft.util.ResourceLocation
import net.minecraft.util.math.BlockPos
import net.minecraft.world.IWorldReader
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import net.minecraftforge.client.model.ModelLoader
import net.minecraftforge.common.extensions.IForgeItem

class FloppyDisk(props: Properties) extends Item(props) with IForgeItem with traits.SimpleItem with CustomModel with traits.FileSystemLike {
  // Necessary for anonymous subclasses used for loot disks.
  unlocalizedName = "floppydisk"

  val kiloBytes = Settings.get.floppySize

  @OnlyIn(Dist.CLIENT)
  private def modelLocationFromDyeName(name: String) = {
    new ModelResourceLocation(Settings.resourceDomain + ":" + Constants.ItemName.Floppy + "_" + name.toLowerCase, "inventory")
  }

  @OnlyIn(Dist.CLIENT)
  override def getModelLocation(stack: ItemStack): ModelResourceLocation = {
    val dyeIndex =
      if (stack.hasTag && stack.getTag.contains(Settings.namespace + "color"))
        stack.getTag.getInt(Settings.namespace + "color")
      else
        8
    modelLocationFromDyeName(Color.byId(dyeIndex max 0 min 15))
  }

  @OnlyIn(Dist.CLIENT)
  override def registerModelLocations(): Unit = {
    for (dyeName <- Color.dyes) {
      val location = modelLocationFromDyeName(dyeName)
      ModelLoader.addSpecialModel(location)
    }
  }

  override def doesSneakBypassUse(stack: ItemStack, world: IWorldReader, pos: BlockPos, player: PlayerEntity): Boolean = true
}
