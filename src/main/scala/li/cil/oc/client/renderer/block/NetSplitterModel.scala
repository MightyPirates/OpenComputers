package li.cil.oc.client.renderer.block

import java.util
import java.util.Collections

import li.cil.oc.client.Textures
import li.cil.oc.common.block
import li.cil.oc.common.item.data.PrintData
import li.cil.oc.common.tileentity
import net.minecraft.block.BlockState
import net.minecraft.client.world.ClientWorld
import net.minecraft.client.renderer.model.BakedQuad
import net.minecraft.client.renderer.model.IBakedModel
import net.minecraft.client.renderer.model.ItemOverrideList
import net.minecraft.entity.LivingEntity
import net.minecraft.item.ItemStack
import net.minecraft.util.Direction
import net.minecraft.util.math.vector.Vector3d
import net.minecraftforge.client.event.TextureStitchEvent
import net.minecraftforge.client.model.data.IModelData
import net.minecraftforge.eventbus.api.SubscribeEvent

import scala.collection.convert.WrapAsJava.bufferAsJavaList
import scala.collection.mutable

object NetSplitterModel extends SmartBlockModelBase {
  override def getOverrides: ItemOverrideList = ItemOverride

  override def getQuads(state: BlockState, side: Direction, rand: util.Random, data: IModelData): util.List[BakedQuad] =
    data match {
      case t: tileentity.NetSplitter =>
        val faces = mutable.ArrayBuffer.empty[BakedQuad]

        faces ++= BaseModel
        addSideQuads(faces, Direction.values().map(t.isSideOpen))

        bufferAsJavaList(faces)
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
    faces ++= bakeQuads(makeBox(new Vector3d(0 / 16f, 0 / 16f, 5 / 16f), new Vector3d(5 / 16f, 5 / 16f, 11 / 16f)), splitterTexture, None)
    faces ++= bakeQuads(makeBox(new Vector3d(11 / 16f, 0 / 16f, 5 / 16f), new Vector3d(16 / 16f, 5 / 16f, 11 / 16f)), splitterTexture, None)
    faces ++= bakeQuads(makeBox(new Vector3d(5 / 16f, 0 / 16f, 0 / 16f), new Vector3d(11 / 16f, 5 / 16f, 5 / 16f)), splitterTexture, None)
    faces ++= bakeQuads(makeBox(new Vector3d(5 / 16f, 0 / 16f, 11 / 16f), new Vector3d(11 / 16f, 5 / 16f, 16 / 16f)), splitterTexture, None)
    // Corners.
    faces ++= bakeQuads(makeBox(new Vector3d(0 / 16f, 0 / 16f, 0 / 16f), new Vector3d(5 / 16f, 16 / 16f, 5 / 16f)), splitterTexture, None)
    faces ++= bakeQuads(makeBox(new Vector3d(11 / 16f, 0 / 16f, 0 / 16f), new Vector3d(16 / 16f, 16 / 16f, 5 / 16f)), splitterTexture, None)
    faces ++= bakeQuads(makeBox(new Vector3d(0 / 16f, 0 / 16f, 11 / 16f), new Vector3d(5 / 16f, 16 / 16f, 16 / 16f)), splitterTexture, None)
    faces ++= bakeQuads(makeBox(new Vector3d(11 / 16f, 0 / 16f, 11 / 16f), new Vector3d(16 / 16f, 16 / 16f, 16 / 16f)), splitterTexture, None)
    // Top.
    faces ++= bakeQuads(makeBox(new Vector3d(0 / 16f, 11 / 16f, 5 / 16f), new Vector3d(5 / 16f, 16 / 16f, 11 / 16f)), splitterTexture, None)
    faces ++= bakeQuads(makeBox(new Vector3d(11 / 16f, 11 / 16f, 5 / 16f), new Vector3d(16 / 16f, 16 / 16f, 11 / 16f)), splitterTexture, None)
    faces ++= bakeQuads(makeBox(new Vector3d(5 / 16f, 11 / 16f, 0 / 16f), new Vector3d(11 / 16f, 16 / 16f, 5 / 16f)), splitterTexture, None)
    faces ++= bakeQuads(makeBox(new Vector3d(5 / 16f, 11 / 16f, 11 / 16f), new Vector3d(11 / 16f, 16 / 16f, 16 / 16f)), splitterTexture, None)

    faces.toArray
  }

  protected var BaseModel = Array.empty[BakedQuad]

  @SubscribeEvent
  def onTextureStitch(e: TextureStitchEvent.Post): Unit = {
    BaseModel = GenerateBaseModel()
  }

  protected def addSideQuads(faces: mutable.ArrayBuffer[BakedQuad], openSides: Array[Boolean]): Unit = {
    val down = openSides(Direction.DOWN.ordinal())
    faces ++= bakeQuads(makeBox(new Vector3d(5 / 16f, if (down) 0 / 16f else 2 / 16f, 5 / 16f), new Vector3d(11 / 16f, 5 / 16f, 11 / 16f)), splitterTexture, None)

    val up = openSides(Direction.UP.ordinal())
    faces ++= bakeQuads(makeBox(new Vector3d(5 / 16f, 11 / 16f, 5 / 16f), new Vector3d(11 / 16f, if (up) 16 / 16f else 14f / 16f, 11 / 16f)), splitterTexture, None)

    val north = openSides(Direction.NORTH.ordinal())
    faces ++= bakeQuads(makeBox(new Vector3d(5 / 16f, 5 / 16f, if (north) 0 / 16f else 2 / 16f), new Vector3d(11 / 16f, 11 / 16f, 5 / 16f)), splitterTexture, None)

    val south = openSides(Direction.SOUTH.ordinal())
    faces ++= bakeQuads(makeBox(new Vector3d(5 / 16f, 5 / 16f, 11 / 16f), new Vector3d(11 / 16f, 11 / 16f, if (south) 16 / 16f else 14 / 16f)), splitterTexture, None)

    val west = openSides(Direction.WEST.ordinal())
    faces ++= bakeQuads(makeBox(new Vector3d(if (west) 0 / 16f else 2 / 16f, 5 / 16f, 5 / 16f), new Vector3d(5 / 16f, 11 / 16f, 11 / 16f)), splitterTexture, None)

    val east = openSides(Direction.EAST.ordinal())
    faces ++= bakeQuads(makeBox(new Vector3d(11 / 16f, 5 / 16f, 5 / 16f), new Vector3d(if (east) 16 / 16f else 14 / 16f, 11 / 16f, 11 / 16f)), splitterTexture, None)
  }

  class ItemModel(val stack: ItemStack) extends SmartBlockModelBase {
    val data = new PrintData(stack)

    override def getQuads(state: BlockState, side: Direction, rand: util.Random): util.List[BakedQuad] = {
      val faces = mutable.ArrayBuffer.empty[BakedQuad]

      Textures.Block.bind()

      faces ++= BaseModel
      addSideQuads(faces, Direction.values().map(_ => false))

      bufferAsJavaList(faces)
    }
  }

  object ItemOverride extends ItemOverrideList {
    override def resolve(originalModel: IBakedModel, stack: ItemStack, world: ClientWorld, entity: LivingEntity): IBakedModel = new ItemModel(stack)
  }

}
