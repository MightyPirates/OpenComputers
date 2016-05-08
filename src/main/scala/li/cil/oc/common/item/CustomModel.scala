package li.cil.oc.common.item

import net.minecraft.client.renderer.block.model.ModelResourceLocation
import net.minecraft.item.ItemStack
import net.minecraftforge.client.event.ModelBakeEvent
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly

trait CustomModel {
  @SideOnly(Side.CLIENT)
  def getModelLocation(stack: ItemStack): ModelResourceLocation

  @SideOnly(Side.CLIENT)
  def registerModelLocations(): Unit = {}

  @SideOnly(Side.CLIENT)
  def bakeModels(bakeEvent: ModelBakeEvent): Unit = {}
}
