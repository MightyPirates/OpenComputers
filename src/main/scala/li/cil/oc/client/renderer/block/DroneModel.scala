package li.cil.oc.client.renderer.block

import li.cil.oc.client.Textures
import net.minecraft.block.state.IBlockState
import net.minecraft.client.renderer.block.model.BakedQuad
import net.minecraft.item.ItemStack
import net.minecraft.util.Vec3
import net.minecraftforge.client.model.ISmartItemModel

import scala.collection.convert.WrapAsJava.bufferAsJavaList
import scala.collection.mutable

object DroneModel extends SmartBlockModelBase with ISmartItemModel {
  override def handleBlockState(state: IBlockState) = new NullModel.Model()

  override def handleItemState(stack: ItemStack) = new ItemModel(stack)

  protected def droneTexture = Textures.getSprite(Textures.Item.DroneItem)

  protected def Boxes = Array(
    makeBox(new Vec3(1f / 16f, 7f / 16f, 1f / 16f), new Vec3(7f / 16f, 8f / 16f, 7f / 16f)),
    makeBox(new Vec3(1f / 16f, 7f / 16f, 9f / 16f), new Vec3(7f / 16f, 8f / 16f, 15f / 16f)),
    makeBox(new Vec3(9f / 16f, 7f / 16f, 1f / 16f), new Vec3(15f / 16f, 8f / 16f, 7f / 16f)),
    makeBox(new Vec3(9f / 16f, 7f / 16f, 9f / 16f), new Vec3(15f / 16f, 8f / 16f, 15f / 16f)),
    rotateBox(makeBox(new Vec3(6f / 16f, 6f / 16f, 6f / 16f), new Vec3(10f / 16f, 9f / 16f, 10f / 16f)), 45)
  )

  class ItemModel(val stack: ItemStack) extends SmartBlockModelBase {
    override def getGeneralQuads = {
      val faces = mutable.ArrayBuffer.empty[BakedQuad]

      faces ++= Boxes.flatMap(box => bakeQuads(box, Array.fill(6)(droneTexture), None))

      bufferAsJavaList(faces)
    }
  }

}
