package li.cil.oc.common.item

import net.minecraft.client.resources.model.ModelResourceLocation
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly

trait CustomModel {
  @SideOnly(Side.CLIENT)
  def getModelLocation(stack: ItemStack): ModelResourceLocation

  @SideOnly(Side.CLIENT)
  def registerModelLocations(): Unit
}
