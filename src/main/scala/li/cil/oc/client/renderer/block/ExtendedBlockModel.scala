package li.cil.oc.client.renderer.block

import java.util.Collections

import li.cil.oc.Settings
import li.cil.oc.client.Textures
import li.cil.oc.common.block
import li.cil.oc.common.tileentity
import net.minecraft.block.Block
import net.minecraft.block.state.IBlockState
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.block.model.BakedQuad
import net.minecraft.client.renderer.block.model.ItemCameraTransforms
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import net.minecraft.client.resources.model.ModelResourceLocation
import net.minecraft.util.EnumFacing
import net.minecraft.util.RegistrySimple
import net.minecraftforge.client.event.ModelBakeEvent
import net.minecraftforge.client.model.ISmartBlockModel
import net.minecraftforge.common.property.IExtendedBlockState
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

import scala.collection.convert.WrapAsJava._
import scala.collection.convert.WrapAsScala._
import scala.collection.mutable

object ExtendedBlockModel {
  private val extendedBlocks = mutable.ArrayBuffer.empty[block.traits.Extended]

  def registerBlock(instance: Block): Unit = {
    instance match {
      case extended: block.traits.Extended => extendedBlocks += extended
      case _ =>
    }
  }

  @SubscribeEvent
  def onModelBake(e: ModelBakeEvent): Unit = {
    val registry = e.modelRegistry.asInstanceOf[RegistrySimple]
    registry.getKeys.collect {
      case location: ModelResourceLocation =>
        if (location.toString.startsWith(Settings.resourceDomain + ":screen1#")) {
          registry.putObject(location, ScreenRenderer)
        }
    }
  }
}

trait SmartBlockModelBase extends ISmartBlockModel {
  override def handleBlockState(state: IBlockState) = missingModel

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
}

object ScreenRenderer extends SmartBlockModelBase {
  override def handleBlockState(state: IBlockState) = state match {
    case extended: IExtendedBlockState => new ScreenRenderer(extended)
    case _ => missingModel
  }
}

class ScreenRenderer(val state: IExtendedBlockState) extends SmartBlockModelBase {
  override def getFaceQuads(side: EnumFacing) = {
    state.getValue(block.Screen.Tile) match {
      case screen: tileentity.Screen =>
        // TODO Translate facing to "local" coordinate system of our default texture layout.
        val facing = screen.toLocal(side)

        val textures = if (screen.width == 1 && screen.height == 1) {
          Textures.Block.Screen.Single
        }
        else if (screen.width == 1) {
          val (_, y) = screen.localPosition
          if (y == 0) Textures.Block.Screen.VerticalBottom
          else if (y == screen.height - 1) Textures.Block.Screen.VerticalTop
          else Textures.Block.Screen.VerticalMiddle
        }
        else if (screen.height == 1) {
          val (x, _) = screen.localPosition
          if (x == 0) Textures.Block.Screen.HorizontalRight
          else if (x == screen.width - 1) Textures.Block.Screen.HorizontalLeft
          else Textures.Block.Screen.HorizontalMiddle
        }
        else {
          val (x, y) = screen.localPosition
          // TODO Differentiate horizontal and vertical multiscreens.
          if (x == 0)
            if (y == 0) Textures.Block.Screen.MultiBottomRight
            else if (y == screen.height - 1) Textures.Block.Screen.MultiTopRight
            else Textures.Block.Screen.MultiMiddleRight
          else if (x == screen.width - 1)
            if (y == 0) Textures.Block.Screen.MultiBottomLeft
            else if (y == screen.height - 1) Textures.Block.Screen.MultiTopLeft
            else Textures.Block.Screen.MultiMiddleLeft
          else
            if (y == 0) Textures.Block.Screen.MultiBottomMiddle
            else if (y == screen.height - 1) Textures.Block.Screen.MultiTopMiddle
            else Textures.Block.Screen.MultiMiddleMiddle
        }

        List(new BakedQuad(makeQuad(side, Textures.Block.getSprite(textures(facing.ordinal()))), -1, side))
      case _ => super.getFaceQuads(side)
    }
  }

  private def makeQuad(facing: EnumFacing, texture: TextureAtlasSprite) = {
    val face = faces(facing.getIndex)
    face.map(data => {
      val (x, y, z, u, v) = data
      rawData(x, y, z, texture, u, v)
    }).flatten
  }

  // See FaceBakery#storeVertexData.
  private def rawData(x: Float, y: Float, z: Float, texture: TextureAtlasSprite, u: Float, v: Float) = {
    Array(
      java.lang.Float.floatToRawIntBits(x),
      java.lang.Float.floatToRawIntBits(y),
      java.lang.Float.floatToRawIntBits(z),
      0xFFFFFFFF,
      java.lang.Float.floatToRawIntBits(texture.getInterpolatedU(u)),
      java.lang.Float.floatToRawIntBits(texture.getInterpolatedV(v)),
      0
    )
  }
}