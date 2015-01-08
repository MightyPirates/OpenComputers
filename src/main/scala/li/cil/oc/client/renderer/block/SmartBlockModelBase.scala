package li.cil.oc.client.renderer.block

import java.util.Collections

import li.cil.oc.client.Textures
import li.cil.oc.util.Color
import net.minecraft.block.state.IBlockState
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.block.model.ItemCameraTransforms
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import net.minecraft.item.EnumDyeColor
import net.minecraft.item.ItemStack
import net.minecraft.util.EnumFacing
import net.minecraft.util.Vec3
import net.minecraftforge.client.model.ISmartBlockModel
import net.minecraftforge.client.model.ISmartItemModel

trait SmartBlockModelBase extends ISmartBlockModel with ISmartItemModel {
  override def handleBlockState(state: IBlockState) = missingModel

  override def handleItemState(stack: ItemStack) = missingModel

  override def getFaceQuads(side: EnumFacing): java.util.List[_] = Collections.emptyList()

  override def getGeneralQuads: java.util.List[_] = Collections.emptyList()

  override def isAmbientOcclusion = true

  override def isGui3d = true

  override def isBuiltInRenderer = false

  // Note: we don't care about the actual texture here, we just need the block
  // texture atlas. So any of our textures we know is loaded into it will do.
  override def getTexture = Textures.Block.getSprite(Textures.Block.CableCap)

  override def getItemCameraTransforms = ItemCameraTransforms.DEFAULT

  protected def missingModel = Minecraft.getMinecraft.getRenderItem.getItemModelMesher.getModelManager.getMissingModel

  // Standard faces for a unit cube.
  protected final val UnitCube = Array(
    Array(new Vec3(0, 0, 0), new Vec3(1, 0, 0), new Vec3(1, 0, 1), new Vec3(0, 0, 1)),
    Array(new Vec3(0, 1, 1), new Vec3(1, 1, 1), new Vec3(1, 1, 0), new Vec3(0, 1, 0)),
    Array(new Vec3(0, 1, 0), new Vec3(1, 1, 0), new Vec3(1, 0, 0), new Vec3(0, 0, 0)),
    Array(new Vec3(1, 1, 1), new Vec3(0, 1, 1), new Vec3(0, 0, 1), new Vec3(1, 0, 1)),
    Array(new Vec3(0, 1, 1), new Vec3(0, 1, 0), new Vec3(0, 0, 0), new Vec3(0, 0, 1)),
    Array(new Vec3(1, 1, 0), new Vec3(1, 1, 1), new Vec3(1, 0, 1), new Vec3(1, 0, 0))
  )

  // Planes perpendicular to facings. Negative values mean we mirror along that,
  // axis which is done to mirror back faces and the y axis (because up is
  // positive but for our texture coordinates down is positive).
  protected final val Planes = Array(
    (new Vec3(1, 0, 0), new Vec3(0, 0, -1)),
    (new Vec3(1, 0, 0), new Vec3(0, 0, 1)),
    (new Vec3(-1, 0, 0), new Vec3(0, -1, 0)),
    (new Vec3(1, 0, 0), new Vec3(0, -1, 0)),
    (new Vec3(0, 0, 1), new Vec3(0, -1, 0)),
    (new Vec3(0, 0, -1), new Vec3(0, -1, 0))
  )

  protected def makeQuad(facing: EnumFacing, texture: TextureAtlasSprite, rotation: Int, color: Option[EnumDyeColor]): Array[Int] = {
    makeQuad(UnitCube(facing.getIndex), facing, texture, rotation, color)
  }

  protected def makeQuad(vertices: Array[Vec3], facing: EnumFacing, texture: TextureAtlasSprite, rotation: Int, color: Option[EnumDyeColor]): Array[Int] = {
    val (uAxis, vAxis) = Planes(facing.getIndex)
    val bgr = rgb2bgr(color.fold(0xFFFFFF)(Color.rgbValues(_)))
    val rot = (rotation + 4) % 4
    vertices.map(vertex => {
      var u = vertex.dotProduct(uAxis)
      var v = vertex.dotProduct(vAxis)
      if (uAxis.xCoord + uAxis.yCoord + uAxis.zCoord < 0) u = 1 + u
      if (vAxis.xCoord + vAxis.yCoord + vAxis.zCoord < 0) v = 1 + v
      for (i <- 0 until rot) {
        // (u, v) = (v, -u)
        val tmp = u
        u = v
        v = (-(tmp - 0.5)) + 0.5
      }
      rawData(vertex.xCoord, vertex.yCoord, vertex.zCoord, texture, u, v, bgr)
    }).flatten
  }

  // See FaceBakery#storeVertexData.
  private def rawData(x: Double, y: Double, z: Double, texture: TextureAtlasSprite, u: Double, v: Double, bgr: Int) = {
    Array(
      java.lang.Float.floatToRawIntBits(x.toFloat),
      java.lang.Float.floatToRawIntBits(y.toFloat),
      java.lang.Float.floatToRawIntBits(z.toFloat),
      0xFF000000 | bgr,
      java.lang.Float.floatToRawIntBits(texture.getInterpolatedU(u * 16)),
      java.lang.Float.floatToRawIntBits(texture.getInterpolatedV(v * 16)),
      0
    )
  }

  private def rgb2bgr(color: Int) = {
    ((color & 0x0000FF) << 16) | (color & 0x00FF00) | ((color & 0xFF0000) >>> 16)
  }
}
