package li.cil.oc.client.renderer.block

import java.util.Collections

import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.client.Textures
import li.cil.oc.common.Tier
import li.cil.oc.common.block
import li.cil.oc.common.tileentity
import li.cil.oc.util.Color
import net.minecraft.block.Block
import net.minecraft.block.state.IBlockState
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.block.model.BakedQuad
import net.minecraft.client.renderer.block.model.ItemCameraTransforms
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import net.minecraft.client.resources.model.ModelResourceLocation
import net.minecraft.item.EnumDyeColor
import net.minecraft.item.ItemStack
import net.minecraft.util.EnumFacing
import net.minecraft.util.RegistrySimple
import net.minecraftforge.client.event.ModelBakeEvent
import net.minecraftforge.client.model.ISmartBlockModel
import net.minecraftforge.client.model.ISmartItemModel
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
    val screenPattern = "^" + Settings.resourceDomain + ":screen\\d#.*"
    registry.getKeys.collect {
      case location: ModelResourceLocation if location.toString.matches(screenPattern) => registry.putObject(location, ScreenRenderer)
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

object ScreenRenderer extends SmartBlockModelBase with ISmartItemModel {
  override def handleBlockState(state: IBlockState) = state match {
    case extended: IExtendedBlockState => new BlockModel(extended)
    case _ => missingModel
  }

  override def handleItemState(stack: ItemStack) = new ItemModel(stack)

  private def multiCoords(value: Int, high: Int) = if (value == 0) 2 else if (value == high) 0 else 1

  class BlockModel(val state: IExtendedBlockState) extends SmartBlockModelBase {
    override def getFaceQuads(side: EnumFacing) =
      state.getValue(block.Screen.Tile) match {
        case screen: tileentity.Screen =>
          val facing = screen.toLocal(side)

          val (x, y) = screen.localPosition
          val pitch = if (screen.pitch == EnumFacing.NORTH) 0 else 1
          var rx = multiCoords(x, screen.width - 1)
          var ry = multiCoords(y, screen.height - 1)
          var rotation = 0

          if (screen.pitch == EnumFacing.DOWN)
            ry = 2 - ry
          if (side == EnumFacing.UP) {
            rotation += screen.yaw.getHorizontalIndex
            ry = 2 - ry
          }
          else {
            if (side == EnumFacing.DOWN) {
              if (screen.yaw.getAxis == EnumFacing.Axis.X) {
                rotation += 1
                rx = 2 - rx
              }
              if (screen.yaw.getAxisDirection.getOffset < 0)
                ry = 2 - ry
            }
            else if (screen.yaw.getAxisDirection.getOffset > 0 && pitch == 1)
              ry = 2 - ry
            if (screen.yaw == EnumFacing.NORTH || screen.yaw == EnumFacing.EAST)
              rx = 2 - rx
          }

          val textures =
            if (screen.width == 1 && screen.height == 1) {
              val result = Textures.Block.Screen.Single.clone()
              if (facing == EnumFacing.SOUTH)
                result(3) = Textures.Block.Screen.SingleFront(pitch)
              result
            }
            else if (screen.width == 1) {
              val result = Textures.Block.Screen.Vertical(pitch)(ry).clone()
              if (facing == EnumFacing.SOUTH)
                result(3) = Textures.Block.Screen.VerticalFront(pitch)(ry)
              result
            }
            else if (screen.height == 1) {
              val result = Textures.Block.Screen.Horizontal(pitch)(rx).clone()
              if (facing == EnumFacing.SOUTH)
                result(3) = Textures.Block.Screen.HorizontalFront(pitch)(rx)
              result
            }
            else {
              val result = Textures.Block.Screen.Multi(pitch)(ry)(rx).clone()
              if (facing == EnumFacing.SOUTH)
                result(3) = Textures.Block.Screen.MultiFront(pitch)(ry)(rx)
              result
            }

          seqAsJavaList(Seq(new BakedQuad(makeQuad(side, Textures.Block.getSprite(textures(facing.ordinal())), screen.color, rotation), -1, side)))
        case _ => super.getFaceQuads(side)
      }
  }

  class ItemModel(val stack: ItemStack) extends SmartBlockModelBase {
    val color = api.Items.get(stack).name() match {
      case "screen2" => Color.byTier(Tier.Two)
      case "screen3" => Color.byTier(Tier.Three)
      case _ => Color.byTier(Tier.One)
    }

    override def getFaceQuads(side: EnumFacing) = {
      val result =
        if (side == EnumFacing.NORTH)
          Textures.Block.Screen.SingleFront(0)
        else
          Textures.Block.Screen.Single(side.ordinal())
      seqAsJavaList(Seq(new BakedQuad(makeQuad(side, Textures.Block.getSprite(result), color, 0), -1, side)))
    }
  }

}
