package li.cil.oc.common.item

import net.minecraft.client.resources.model.ModelResourceLocation
import net.minecraft.item.ItemStack

trait CustomModel {
  def getModelLocation(stack: ItemStack): ModelResourceLocation

  def registerModelLocations(): Unit
}
