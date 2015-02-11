package li.cil.oc.client.renderer.block

import net.minecraft.block.state.IBlockState
import net.minecraft.client.renderer.block.model.BakedQuad
import net.minecraft.item.ItemStack
import net.minecraftforge.client.model.ISmartItemModel

import scala.collection.convert.WrapAsJava.bufferAsJavaList
import scala.collection.mutable

object RobotModel extends SmartBlockModelBase with ISmartItemModel {
  override def handleBlockState(state: IBlockState) = new NullModel.Model()

  override def handleItemState(stack: ItemStack) = new ItemModel(stack)

  class ItemModel(val stack: ItemStack) extends SmartBlockModelBase {
    override def getGeneralQuads = {
      val faces = mutable.ArrayBuffer.empty[BakedQuad]

      // TODO

      bufferAsJavaList(faces)
    }
  }

}
