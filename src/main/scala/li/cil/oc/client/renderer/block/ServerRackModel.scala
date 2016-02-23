package li.cil.oc.client.renderer.block

import li.cil.oc.client.Textures
import li.cil.oc.common.block
import li.cil.oc.common.tileentity
import net.minecraft.block.state.IBlockState
import net.minecraft.client.renderer.block.model.BakedQuad
import net.minecraft.item.ItemStack
import net.minecraft.util.EnumFacing
import net.minecraft.util.Vec3
import net.minecraftforge.client.model.IFlexibleBakedModel
import net.minecraftforge.client.model.ISmartItemModel
import net.minecraftforge.common.property.IExtendedBlockState

import scala.collection.convert.WrapAsJava.bufferAsJavaList
import scala.collection.mutable

class ServerRackModel(val parent: IFlexibleBakedModel) extends SmartBlockModelBase with ISmartItemModel {
  override def handleBlockState(state: IBlockState) = state match {
    case extended: IExtendedBlockState => new BlockModel(extended)
    case _ => missingModel
  }

  override def handleItemState(stack: ItemStack) = parent

  protected def serverRackTexture = Array(
    Textures.getSprite(Textures.Block.GenericTop),
    Textures.getSprite(Textures.Block.GenericTop),
    Textures.getSprite(Textures.Block.RackSide),
    Textures.getSprite(Textures.Block.RackSide),
    Textures.getSprite(Textures.Block.RackSide),
    Textures.getSprite(Textures.Block.RackSide)
  )

  protected def serverTexture = Array(
    Textures.getSprite(Textures.Block.GenericTop),
    Textures.getSprite(Textures.Block.GenericTop),
    Textures.getSprite(Textures.Block.RackFront),
    Textures.getSprite(Textures.Block.RackFront),
    Textures.getSprite(Textures.Block.RackFront),
    Textures.getSprite(Textures.Block.RackFront)
  )

  protected final val Case = Array(
    makeBox(new Vec3(0 / 16f, 0 / 16f, 0 / 16f), new Vec3(16 / 16f, 2 / 16f, 16 / 16f)),
    makeBox(new Vec3(0 / 16f, 14 / 16f, 0 / 16f), new Vec3(16 / 16f, 16 / 16f, 16 / 16f)),
    makeBox(new Vec3(0 / 16f, 2 / 16f, 0 / 16f), new Vec3(16 / 16f, 14 / 16f, 0.99f / 16f)),
    makeBox(new Vec3(0 / 16f, 2 / 16f, 15.01f / 16f), new Vec3(16 / 16f, 14 / 16f, 16 / 16f)),
    makeBox(new Vec3(0 / 16f, 2 / 16f, 0 / 16f), new Vec3(0.99f / 16f, 14 / 16f, 16 / 16f)),
    makeBox(new Vec3(15.01f / 16f, 2 / 16f, 0 / 16f), new Vec3(16 / 16f, 14f / 16f, 16 / 16f))
  )

  protected final val Servers = Array(
    makeBox(new Vec3(0.5f / 16f, 11 / 16f, 0.5f / 16f), new Vec3(15.5f / 16f, 14 / 16f, 15.5f / 16f)),
    makeBox(new Vec3(0.5f / 16f, 8 / 16f, 0.5f / 16f), new Vec3(15.5f / 16f, 11 / 16f, 15.5f / 16f)),
    makeBox(new Vec3(0.5f / 16f, 5 / 16f, 0.5f / 16f), new Vec3(15.5f / 16f, 8 / 16f, 15.5f / 16f)),
    makeBox(new Vec3(0.5f / 16f, 2 / 16f, 0.5f / 16f), new Vec3(15.5f / 16f, 5 / 16f, 15.5f / 16f))
  )

  class BlockModel(val state: IExtendedBlockState) extends SmartBlockModelBase {
    override def getFaceQuads(side: EnumFacing) = {

      state.getValue(block.property.PropertyTile.Tile) match {
        case rack: tileentity.Rack =>
          val facing = rack.facing
          val faces = mutable.ArrayBuffer.empty[BakedQuad]

          for (side <- EnumFacing.values if side != facing) {
            faces ++= bakeQuads(Case(side.getIndex), serverRackTexture, None)
          }

//          for (i <- 0 until 4 if rack.isPresent(i).isDefined) {
//            faces ++= bakeQuads(Servers(i), serverTexture, None)
//          }

          bufferAsJavaList(faces)
        case _ => super.getFaceQuads(side)
      }
    }
  }

}
