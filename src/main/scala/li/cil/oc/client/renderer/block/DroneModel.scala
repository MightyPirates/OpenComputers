package li.cil.oc.client.renderer.block

import java.util
import java.util.Collections

import li.cil.oc.client.Textures
import net.minecraft.block.state.IBlockState
import net.minecraft.client.renderer.block.model.BakedQuad
import net.minecraft.client.renderer.block.model.IBakedModel
import net.minecraft.client.renderer.block.model.ItemOverrideList
import net.minecraft.entity.EntityLivingBase
import net.minecraft.item.ItemStack
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.Vec3d
import net.minecraft.world.World

import scala.collection.convert.WrapAsJava.bufferAsJavaList
import scala.collection.mutable

object DroneModel extends SmartBlockModelBase {
  override def getOverrides: ItemOverrideList = ItemOverride

  override def getQuads(state: IBlockState, side: EnumFacing, rand: Long): util.List[BakedQuad] = {
    val faces = mutable.ArrayBuffer.empty[BakedQuad]

    faces ++= Boxes.flatMap(box => bakeQuads(box, Array.fill(6)(droneTexture), None))

    bufferAsJavaList(faces)
  }

  protected def droneTexture = Textures.getSprite(Textures.Item.DroneItem)

  protected def Boxes = Array(
    makeBox(new Vec3d(1f / 16f, 7f / 16f, 1f / 16f), new Vec3d(7f / 16f, 8f / 16f, 7f / 16f)),
    makeBox(new Vec3d(1f / 16f, 7f / 16f, 9f / 16f), new Vec3d(7f / 16f, 8f / 16f, 15f / 16f)),
    makeBox(new Vec3d(9f / 16f, 7f / 16f, 1f / 16f), new Vec3d(15f / 16f, 8f / 16f, 7f / 16f)),
    makeBox(new Vec3d(9f / 16f, 7f / 16f, 9f / 16f), new Vec3d(15f / 16f, 8f / 16f, 15f / 16f)),
    rotateBox(makeBox(new Vec3d(6f / 16f, 6f / 16f, 6f / 16f), new Vec3d(10f / 16f, 9f / 16f, 10f / 16f)), 45)
  )

  object ItemOverride extends ItemOverrideList(Collections.emptyList()) {
    override def handleItemState(originalModel: IBakedModel, stack: ItemStack, world: World, entity: EntityLivingBase): IBakedModel = DroneModel
  }

}
