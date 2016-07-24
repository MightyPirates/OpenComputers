package li.cil.oc.client.renderer.block

import java.util
import java.util.Collections

import li.cil.oc.client.Textures
import li.cil.oc.common.block
import li.cil.oc.common.item.data.PrintData
import li.cil.oc.common.tileentity
import net.minecraft.block.state.IBlockState
import net.minecraft.client.renderer.block.model.BakedQuad
import net.minecraft.client.renderer.block.model.IBakedModel
import net.minecraft.client.renderer.block.model.ItemOverrideList
import net.minecraft.entity.EntityLivingBase
import net.minecraft.item.ItemStack
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.Vec3d
import net.minecraft.world.World
import net.minecraftforge.client.event.TextureStitchEvent
import net.minecraftforge.common.property.IExtendedBlockState
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

import scala.collection.convert.WrapAsJava.bufferAsJavaList
import scala.collection.mutable

object NetSplitterModel extends SmartBlockModelBase {
  override def getOverrides: ItemOverrideList = ItemOverride

  override def getQuads(state: IBlockState, side: EnumFacing, rand: Long): util.List[BakedQuad] =
    state match {
      case extended: IExtendedBlockState =>
        extended.getValue(block.property.PropertyTile.Tile) match {
          case t: tileentity.NetSplitter =>
            val faces = mutable.ArrayBuffer.empty[BakedQuad]

            faces ++= BaseModel
            addSideQuads(faces, EnumFacing.values().map(t.isSideOpen))

            bufferAsJavaList(faces)
          case _ => super.getQuads(state, side, rand)
        }
      case _ => super.getQuads(state, side, rand)
    }

  protected def splitterTexture = Array(
    Textures.getSprite(Textures.Block.NetSplitterTop),
    Textures.getSprite(Textures.Block.NetSplitterTop),
    Textures.getSprite(Textures.Block.NetSplitterSide),
    Textures.getSprite(Textures.Block.NetSplitterSide),
    Textures.getSprite(Textures.Block.NetSplitterSide),
    Textures.getSprite(Textures.Block.NetSplitterSide)
  )

  protected def GenerateBaseModel() = {
    val faces = mutable.ArrayBuffer.empty[BakedQuad]

    // Bottom.
    faces ++= bakeQuads(makeBox(new Vec3d(0 / 16f, 0 / 16f, 5 / 16f), new Vec3d(5 / 16f, 5 / 16f, 11 / 16f)), splitterTexture, None)
    faces ++= bakeQuads(makeBox(new Vec3d(11 / 16f, 0 / 16f, 5 / 16f), new Vec3d(16 / 16f, 5 / 16f, 11 / 16f)), splitterTexture, None)
    faces ++= bakeQuads(makeBox(new Vec3d(5 / 16f, 0 / 16f, 0 / 16f), new Vec3d(11 / 16f, 5 / 16f, 5 / 16f)), splitterTexture, None)
    faces ++= bakeQuads(makeBox(new Vec3d(5 / 16f, 0 / 16f, 11 / 16f), new Vec3d(11 / 16f, 5 / 16f, 16 / 16f)), splitterTexture, None)
    // Corners.
    faces ++= bakeQuads(makeBox(new Vec3d(0 / 16f, 0 / 16f, 0 / 16f), new Vec3d(5 / 16f, 16 / 16f, 5 / 16f)), splitterTexture, None)
    faces ++= bakeQuads(makeBox(new Vec3d(11 / 16f, 0 / 16f, 0 / 16f), new Vec3d(16 / 16f, 16 / 16f, 5 / 16f)), splitterTexture, None)
    faces ++= bakeQuads(makeBox(new Vec3d(0 / 16f, 0 / 16f, 11 / 16f), new Vec3d(5 / 16f, 16 / 16f, 16 / 16f)), splitterTexture, None)
    faces ++= bakeQuads(makeBox(new Vec3d(11 / 16f, 0 / 16f, 11 / 16f), new Vec3d(16 / 16f, 16 / 16f, 16 / 16f)), splitterTexture, None)
    // Top.
    faces ++= bakeQuads(makeBox(new Vec3d(0 / 16f, 11 / 16f, 5 / 16f), new Vec3d(5 / 16f, 16 / 16f, 11 / 16f)), splitterTexture, None)
    faces ++= bakeQuads(makeBox(new Vec3d(11 / 16f, 11 / 16f, 5 / 16f), new Vec3d(16 / 16f, 16 / 16f, 11 / 16f)), splitterTexture, None)
    faces ++= bakeQuads(makeBox(new Vec3d(5 / 16f, 11 / 16f, 0 / 16f), new Vec3d(11 / 16f, 16 / 16f, 5 / 16f)), splitterTexture, None)
    faces ++= bakeQuads(makeBox(new Vec3d(5 / 16f, 11 / 16f, 11 / 16f), new Vec3d(11 / 16f, 16 / 16f, 16 / 16f)), splitterTexture, None)

    faces.toArray
  }

  protected var BaseModel = Array.empty[BakedQuad]

  @SubscribeEvent
  def onTextureStitch(e: TextureStitchEvent.Post): Unit = {
    BaseModel = GenerateBaseModel()
  }

  protected def addSideQuads(faces: mutable.ArrayBuffer[BakedQuad], openSides: Array[Boolean]): Unit = {
    val down = openSides(EnumFacing.DOWN.ordinal())
    faces ++= bakeQuads(makeBox(new Vec3d(5 / 16f, if (down) 0 / 16f else 2 / 16f, 5 / 16f), new Vec3d(11 / 16f, 5 / 16f, 11 / 16f)), splitterTexture, None)

    val up = openSides(EnumFacing.UP.ordinal())
    faces ++= bakeQuads(makeBox(new Vec3d(5 / 16f, 11 / 16f, 5 / 16f), new Vec3d(11 / 16f, if (up) 16 / 16f else 14f / 16f, 11 / 16f)), splitterTexture, None)

    val north = openSides(EnumFacing.NORTH.ordinal())
    faces ++= bakeQuads(makeBox(new Vec3d(5 / 16f, 5 / 16f, if (north) 0 / 16f else 2 / 16f), new Vec3d(11 / 16f, 11 / 16f, 5 / 16f)), splitterTexture, None)

    val south = openSides(EnumFacing.SOUTH.ordinal())
    faces ++= bakeQuads(makeBox(new Vec3d(5 / 16f, 5 / 16f, 11 / 16f), new Vec3d(11 / 16f, 11 / 16f, if (south) 16 / 16f else 14 / 16f)), splitterTexture, None)

    val west = openSides(EnumFacing.WEST.ordinal())
    faces ++= bakeQuads(makeBox(new Vec3d(if (west) 0 / 16f else 2 / 16f, 5 / 16f, 5 / 16f), new Vec3d(5 / 16f, 11 / 16f, 11 / 16f)), splitterTexture, None)

    val east = openSides(EnumFacing.EAST.ordinal())
    faces ++= bakeQuads(makeBox(new Vec3d(11 / 16f, 5 / 16f, 5 / 16f), new Vec3d(if (east) 16 / 16f else 14 / 16f, 11 / 16f, 11 / 16f)), splitterTexture, None)
  }

  class ItemModel(val stack: ItemStack) extends SmartBlockModelBase {
    val data = new PrintData(stack)

    override def getQuads(state: IBlockState, side: EnumFacing, rand: Long): util.List[BakedQuad] = {
      val faces = mutable.ArrayBuffer.empty[BakedQuad]

      Textures.Block.bind()

      faces ++= BaseModel
      addSideQuads(faces, EnumFacing.values().map(_ => false))

      bufferAsJavaList(faces)
    }
  }

  object ItemOverride extends ItemOverrideList(Collections.emptyList()) {
    override def handleItemState(originalModel: IBakedModel, stack: ItemStack, world: World, entity: EntityLivingBase): IBakedModel = new ItemModel(stack)
  }

}
