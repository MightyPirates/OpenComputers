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

  // Standard faces for a unit cube, with uv coordinates.
  protected def faces = Array(
    Array((0f, 0f, 0f, 16f, 0f), (1f, 0f, 0f, 0f, 0f), (1f, 0f, 1f, 0f, 16f), (0f, 0f, 1f, 16f, 16f)),
    Array((0f, 1f, 0f, 16f, 16f), (0f, 1f, 1f, 16f, 0f), (1f, 1f, 1f, 0f, 0f), (1f, 1f, 0f, 0f, 16f)),
    Array((0f, 0f, 0f, 16f, 16f), (0f, 1f, 0f, 16f, 0f), (1f, 1f, 0f, 0f, 0f), (1f, 0f, 0f, 0f, 16f)),
    Array((0f, 0f, 1f, 16f, 16f), (1f, 0f, 1f, 0f, 16f), (1f, 1f, 1f, 0f, 0f), (0f, 1f, 1f, 16f, 0f)),
    Array((0f, 0f, 0f, 16f, 16f), (0f, 0f, 1f, 0f, 16f), (0f, 1f, 1f, 0f, 0f), (0f, 1f, 0f, 16f, 0f)),
    Array((1f, 0f, 0f, 16f, 16f), (1f, 1f, 0f, 16f, 0f), (1f, 1f, 1f, 0f, 0f), (1f, 0f, 1f, 0f, 16f))
  )

  protected def makeQuad(facing: EnumFacing, texture: TextureAtlasSprite, color: EnumDyeColor, rotation: Int) = {
    val face = faces(facing.getIndex)
    val verts = face.map(f => (f._1, f._2, f._3))
    val coords = face.map(f => (f._4, f._5))
    (verts, coords.drop(rotation) ++ coords.take(rotation)).zipped.
      map((v, c) => (v._1, v._2, v._3, c._1, c._2)).
      map(data => {
      val (x, y, z, u, v) = data
      rawData(x, y, z, texture, u, v, Color.rgbValues(color))
    }).flatten
  }

  // See FaceBakery#storeVertexData.
  protected def rawData(x: Float, y: Float, z: Float, texture: TextureAtlasSprite, u: Float, v: Float, color: Int) = {
    Array(
      java.lang.Float.floatToRawIntBits(x),
      java.lang.Float.floatToRawIntBits(y),
      java.lang.Float.floatToRawIntBits(z),
      0xFF000000 | rgb2bgr(color),
      java.lang.Float.floatToRawIntBits(texture.getInterpolatedU(u)),
      java.lang.Float.floatToRawIntBits(texture.getInterpolatedV(v)),
      0
    )
  }

  private def rgb2bgr(color: Int) = {
    ((color & 0x0000FF) << 16) | (color & 0x00FF00) | ((color & 0xFF0000) >>> 16)
  }
}
