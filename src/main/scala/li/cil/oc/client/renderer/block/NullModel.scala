package li.cil.oc.client.renderer.block

import net.minecraft.block.state.IBlockState
import net.minecraft.item.ItemStack
import net.minecraftforge.client.model.ISmartItemModel

object NullModel extends SmartBlockModelBase with ISmartItemModel {
  override def handleBlockState(state: IBlockState) = new Model()

  override def handleItemState(stack: ItemStack) = new Model()

  class Model extends SmartBlockModelBase

}
