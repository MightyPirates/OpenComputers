package li.cil.oc.client.renderer.block

import li.cil.oc.client.Textures
import li.cil.oc.common.block
import li.cil.oc.common.item.data.PrintData
import li.cil.oc.common.tileentity
import net.minecraft.block.state.IBlockState
import net.minecraft.client.renderer.block.model.BakedQuad
import net.minecraft.item.ItemStack
import net.minecraft.util.EnumFacing
import net.minecraft.util.Vec3
import net.minecraftforge.client.model.ISmartItemModel
import net.minecraftforge.common.property.IExtendedBlockState

import scala.collection.convert.WrapAsJava.bufferAsJavaList
import scala.collection.mutable

object NetSplitterModel extends SmartBlockModelBase with ISmartItemModel {
  override def handleBlockState(state: IBlockState) = state match {
    case extended: IExtendedBlockState => new BlockModel(extended)
    case _ => missingModel
  }

  override def handleItemState(stack: ItemStack) = new ItemModel(stack)

  protected def splitterTexture = Array.fill(6)(Textures.getSprite(Textures.Block.NetSplitter))

  protected final val BaseModel = {
    val faces = mutable.ArrayBuffer.empty[BakedQuad]

    // Bottom.
    faces ++= bakeQuads(makeBox(new Vec3(0 / 16f, 0 / 16f, 5 / 16f), new Vec3(5 / 16f, 5 / 16f, 11 / 16f)), splitterTexture, None)
    faces ++= bakeQuads(makeBox(new Vec3(11 / 16f, 0 / 16f, 5 / 16f), new Vec3(16 / 16f, 5 / 16f, 11 / 16f)), splitterTexture, None)
    faces ++= bakeQuads(makeBox(new Vec3(5 / 16f, 0 / 16f, 0 / 16f), new Vec3(11 / 16f, 5 / 16f, 5 / 16f)), splitterTexture, None)
    faces ++= bakeQuads(makeBox(new Vec3(5 / 16f, 0 / 16f, 11 / 16f), new Vec3(11 / 16f, 5 / 16f, 16 / 16f)), splitterTexture, None)
    // Corners.
    faces ++= bakeQuads(makeBox(new Vec3(0 / 16f, 0 / 16f, 0 / 16f), new Vec3(5 / 16f, 16 / 16f, 5 / 16f)), splitterTexture, None)
    faces ++= bakeQuads(makeBox(new Vec3(11 / 16f, 0 / 16f, 0 / 16f), new Vec3(16 / 16f, 16 / 16f, 5 / 16f)), splitterTexture, None)
    faces ++= bakeQuads(makeBox(new Vec3(0 / 16f, 0 / 16f, 11 / 16f), new Vec3(5 / 16f, 16 / 16f, 16 / 16f)), splitterTexture, None)
    faces ++= bakeQuads(makeBox(new Vec3(11 / 16f, 0 / 16f, 11 / 16f), new Vec3(16 / 16f, 16 / 16f, 16 / 16f)), splitterTexture, None)
    // Top.
    faces ++= bakeQuads(makeBox(new Vec3(0 / 16f, 11 / 16f, 5 / 16f), new Vec3(5 / 16f, 16 / 16f, 11 / 16f)), splitterTexture, None)
    faces ++= bakeQuads(makeBox(new Vec3(11 / 16f, 11 / 16f, 5 / 16f), new Vec3(16 / 16f, 16 / 16f, 11 / 16f)), splitterTexture, None)
    faces ++= bakeQuads(makeBox(new Vec3(5 / 16f, 11 / 16f, 0 / 16f), new Vec3(11 / 16f, 16 / 16f, 5 / 16f)), splitterTexture, None)
    faces ++= bakeQuads(makeBox(new Vec3(5 / 16f, 11 / 16f, 11 / 16f), new Vec3(11 / 16f, 16 / 16f, 16 / 16f)), splitterTexture, None)

    faces.toArray
  }

  protected def addSideQuads(faces: mutable.ArrayBuffer[BakedQuad], openSides: Array[Boolean]): Unit = {
    val down = openSides(EnumFacing.DOWN.ordinal())
    faces ++= bakeQuads(makeBox(new Vec3(5 / 16f, if (down) 0 / 16f else 2 / 16f, 5 / 16f), new Vec3(11 / 16f, 5 / 16f, 11 / 16f)), splitterTexture, None)

    val up = openSides(EnumFacing.UP.ordinal())
    faces ++= bakeQuads(makeBox(new Vec3(5 / 16f, 11 / 16f, 5 / 16f), new Vec3(11 / 16f, if (up) 16 / 16f else 14f / 16f, 11 / 16f)), splitterTexture, None)

    val north = openSides(EnumFacing.NORTH.ordinal())
    faces ++= bakeQuads(makeBox(new Vec3(5 / 16f, 5 / 16f, if (north) 0 / 16f else 2 / 16f), new Vec3(11 / 16f, 11 / 16f, 5 / 16f)), splitterTexture, None)

    val south = openSides(EnumFacing.SOUTH.ordinal())
    faces ++= bakeQuads(makeBox(new Vec3(5 / 16f, 5 / 16f, 11 / 16f), new Vec3(11 / 16f, 11 / 16f, if (south) 16 / 16f else 14 / 16f)), splitterTexture, None)

    val west = openSides(EnumFacing.WEST.ordinal())
    faces ++= bakeQuads(makeBox(new Vec3(if (west) 0 / 16f else 2 / 16f, 5 / 16f, 5 / 16f), new Vec3(5 / 16f, 11 / 16f, 11 / 16f)), splitterTexture, None)

    val east = openSides(EnumFacing.EAST.ordinal())
    faces ++= bakeQuads(makeBox(new Vec3(11 / 16f, 5 / 16f, 5 / 16f), new Vec3(if (east) 16 / 16f else 14 / 16f, 11 / 16f, 11 / 16f)), splitterTexture, None)
  }

  class BlockModel(val state: IExtendedBlockState) extends SmartBlockModelBase {
    override def getGeneralQuads =
      state.getValue(block.property.PropertyTile.Tile) match {
        case t: tileentity.NetSplitter =>
          val faces = mutable.ArrayBuffer.empty[BakedQuad]

          faces ++= BaseModel
          addSideQuads(faces, EnumFacing.values().map(t.isSideOpen))

          bufferAsJavaList(faces)
        case _ => super.getGeneralQuads
      }
  }

  class ItemModel(val stack: ItemStack) extends SmartBlockModelBase {
    val data = new PrintData(stack)

    override def getGeneralQuads = {
      val faces = mutable.ArrayBuffer.empty[BakedQuad]

      Textures.Block.bind()

      faces ++= BaseModel
      addSideQuads(faces, EnumFacing.values().map(_ => false))

      bufferAsJavaList(faces)
    }
  }

}
