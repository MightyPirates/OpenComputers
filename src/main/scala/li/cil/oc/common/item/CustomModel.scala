package li.cil.oc.common.item

import net.minecraft.client.renderer.model.ModelResourceLocation
import net.minecraft.item.ItemStack
import net.minecraftforge.client.event.ModelBakeEvent
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn

trait CustomModel {
  @OnlyIn(Dist.CLIENT)
  def getModelLocation(stack: ItemStack): ModelResourceLocation

  @OnlyIn(Dist.CLIENT)
  def registerModelLocations(): Unit = {}

  @OnlyIn(Dist.CLIENT)
  def bakeModels(bakeEvent: ModelBakeEvent): Unit = {}
}
