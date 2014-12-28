package li.cil.oc.client.renderer.block

import java.io.InputStreamReader
import java.util.Collections

import com.google.common.base.Charsets
import com.google.common.base.Optional
import li.cil.oc.OpenComputers
import li.cil.oc.Settings
import li.cil.oc.common.block
import net.minecraft.block.Block
import net.minecraft.block.state.IBlockState
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.block.model.BlockPart
import net.minecraft.client.renderer.block.model.BlockPartFace
import net.minecraft.client.renderer.block.model.ItemCameraTransforms
import net.minecraft.client.renderer.block.model.ModelBlock
import net.minecraft.client.renderer.block.statemap.IStateMapper
import net.minecraft.client.renderer.block.statemap.StateMap
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import net.minecraft.client.resources.model.IBakedModel
import net.minecraft.client.resources.model.ModelBakery
import net.minecraft.client.resources.model.ModelResourceLocation
import net.minecraft.client.resources.model.SimpleBakedModel
import net.minecraft.item.Item
import net.minecraft.util.EnumFacing
import net.minecraft.util.ResourceLocation
import net.minecraftforge.client.event.ModelBakeEvent
import net.minecraftforge.client.model.ISmartBlockModel
import net.minecraftforge.common.property.IExtendedBlockState
import net.minecraftforge.common.property.IUnlistedProperty
import net.minecraftforge.fml.client.FMLClientHandler
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.registry.FMLControlledNamespacedRegistry

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

//  def init(): Unit = {
//    val mesher = Minecraft.getMinecraft.getRenderItem.getItemModelMesher
//    for (extended <- extendedBlocks) {
//      val blockName = extended.getUnlocalizedName.stripPrefix("tile.oc.")
//      val blockLocation = Settings.resourceDomain + ":" + blockName
//      val unlisted = extended.collectRawProperties()
//      val values = unlisted.map(property => property.getAllowedValues.collect {
//        case value: java.lang.Comparable[AnyRef]@unchecked => property.getName + "=" + property.getName(value)
//      })
//      val variants = values.foldLeft(Iterable(""))((acc, value) => cross(acc, value)).map(_.stripPrefix(","))
//      for ((variant, pseudoMeta) <- variants.zipWithIndex) {
//        val variantLocation = new ModelResourceLocation(blockLocation, variant)
//        mesher.register(Item.getItemFromBlock(extended), pseudoMeta + 1, variantLocation)
//      }
//    }
//  }

  @SubscribeEvent
  def onModelBake(e: ModelBakeEvent): Unit = {
    val registry = e.modelRegistry
    for (extended <- extendedBlocks) {
      val blockName = extended.getUnlocalizedName.stripPrefix("tile.oc.")
      val blockLocation = Settings.resourceDomain + ":" + blockName
      val modelLocation = new ModelResourceLocation(blockLocation)
//      val model = e.modelRegistry.getObject(modelLocation)
      registry.putObject(modelLocation, new ExtendedBlockModel(blockName))

//      registry.putObject(modelLocation, new WrappedBlockModel(model))

//      extended match {
//        case rotatable: block.traits.Rotatable =>
//          registry.putObject(modelLocation, new RotatableBlockModel(model))
//        case rotatable: block.traits.OmniRotatable =>
//          registry.putObject(modelLocation, new OmniRotatableBlockModel( model))
//        case _ =>
//      }

//      loadModel(modelLocation) match {
//        case Some(model) =>
//          val builder = new SimpleBakedModel.Builder(model)
//          model.getElements.collect {
//            case blockPart: BlockPart =>
//              blockPart.mapFaces.collect {
//                case (facing: EnumFacing, facePart: BlockPartFace) =>
//                  val texture = new ResourceLocation(model.resolveTextureName(facePart.texture))
//                  if (facePart.cullFace == null) {
//                    builder.addGeneralQuad()
//                  }
//              }
//          }
//          registry.putObject(modelLocation, new ExtendedBlockModel(blockName, model))
//        case _ =>
//      }


//            val unlisted = extended.collectRawProperties()
//            val values = unlisted.map(property => property.getAllowedValues.collect {
//              case value: java.lang.Comparable[AnyRef]@unchecked => property.getName + "=" + property.getName(value)
//            })
//            val variants = values.foldLeft(Iterable(""))((acc, value) => cross(acc, value)).map(_.stripPrefix(","))
//            val mesher = Minecraft.getMinecraft.getRenderItem.getItemModelMesher
//            for ((variant, pseudoMeta) <- variants.zipWithIndex) {
//              val variantLocation = new ModelResourceLocation(blockLocation, variant)
//              mesher.register(Item.getItemFromBlock(extended), pseudoMeta + 1, variantLocation)
//            }

//      for (variant <- variants) {
//        val variantLocation = new ModelResourceLocation(blockLocation, variant)
//        registry.putObject(variantLocation, e.modelManager.getModel(variantLocation, variant))
//      }

//      val builder = new StateMap.Builder()
//      val properties = extended.collectRawProperties()
//      for (property <- properties) {
//        builder.setProperty(property)
//      }
//      e.modelManager.getBlockModelShapes.getBlockStateMapper.registerBlockStateMapper(extended, builder.build())


//      e.modelManager.getBlockModelShapes.getBlockStateMapper.registerBlockStateMapper(extended, new IStateMapper {
//        override def putStateModelLocations(block: Block) = {
//          val unlisted = extended.collectRawProperties()
//          val values = unlisted.map(property => property.getAllowedValues.collect {
//            case value: java.lang.Comparable[AnyRef]@unchecked => property.getName + "=" + property.getName(value)
//          })
//          val variants = values.foldLeft(Iterable(""))((acc, value) => cross(acc, value)).map(_.stripPrefix(","))
//        }

//        override def getModelResourceLocation(state: IBlockState) = {
//          state match {
//            case extended: IExtendedBlockState =>
//              val variant = extended.getUnlistedProperties.collect {
//                case (property, value) if value.isPresent => property.getName + "=" + property.valueToString(value.get)
//              }.mkString(",")
//              new ModelResourceLocation(blockLocation, variant)
//            case _ => super.getModelResourceLocation(state)
//          }
//        }
//      })
    }

//    abstract class WrappedBlockModel(val wrappedModel: IBakedModel) extends ISmartBlockModel {
//      override def getFaceQuads(side: EnumFacing) = wrappedModel.getFaceQuads(side)
//
//      override def getGeneralQuads = wrappedModel.getGeneralQuads
//
//      override def isAmbientOcclusion = wrappedModel.isAmbientOcclusion
//
//      override def isGui3d = wrappedModel.isGui3d
//
//      override def isBuiltInRenderer = wrappedModel.isBuiltInRenderer
//
//      override def getTexture = wrappedModel.getTexture
//
//      override def getItemCameraTransforms = wrappedModel.getItemCameraTransforms
//    }

//    class RotatableBlockModel(wrappedModel: IBakedModel) extends WrappedBlockModel(wrappedModel) {
//      override def handleBlockState(state: IBlockState) = {
//      }
//    }

  }

//  private def loadModel(location: ModelResourceLocation) = {
//    val resource = Minecraft.getMinecraft.getResourceManager.getResource(location)
//    val stream = new InputStreamReader(resource.getInputStream, Charsets.UTF_8)
//    try {
//      val model = ModelBlock.deserialize(stream)
//      model.name = resource.toString
//      Some(model)
//    }
//    catch {
//      case t: Throwable =>
//        OpenComputers.log.warn(s"Failed loading block model for $location.", t)
//        None
//    }
//    finally {
//      stream.close()
//    }
//  }

//  private def cross(xs: Iterable[String], ys: Iterable[String]) = for { x <- xs; y <- ys } yield x + "," + y
}

class ExtendedBlockModel(val blockName: String) extends ISmartBlockModel {
//  var statefulModel: Option[IBakedModel] = None

  override def handleBlockState(state: IBlockState) = {
    state match {
      case extended: IExtendedBlockState =>
        val variant = extended.getUnlistedProperties.collect {
          case (key: IUnlistedProperty[AnyRef]@unchecked, value: Optional[AnyRef]@unchecked) if value.isPresent => key.getName + "=" + key.valueToString(value.get)
        }.mkString(",")
        val location = new ModelResourceLocation(Settings.resourceDomain + ":" + blockName, variant)
//        statefulModel = Option(getSimpleModel(location))
        getSimpleModel(location)
      case _ =>
    }
    this
  }

  private def getSimpleModel(location: ModelResourceLocation) = Minecraft.getMinecraft.getRenderItem.getItemModelMesher.getModelManager.getModel(location)

  override def getFaceQuads(side: EnumFacing) = Collections.emptyList() // statefulModel.map(_.getFaceQuads(side)).getOrElse(Collections.emptyList)

  override def getGeneralQuads = Collections.emptyList() // statefulModel.map(_.getGeneralQuads).getOrElse(Collections.emptyList)

  override def isAmbientOcclusion = true // statefulModel.fold(true)(_.isAmbientOcclusion)

  override def isGui3d = true // statefulModel.fold(true)(_.isGui3d)

  override def isBuiltInRenderer = false

  override def getTexture = null // statefulModel.fold(null: TextureAtlasSprite)(_.getTexture)

  override def getItemCameraTransforms = ItemCameraTransforms.DEFAULT // statefulModel.fold(null: ItemCameraTransforms)(_.getItemCameraTransforms)
}

//class WrappedBlockModel(val wrappedModel: IBakedModel) extends ISmartBlockModel {
//  override def handleBlockState(state: IBlockState) = wrappedModel
////    val model = Minecraft.getMinecraft.getRenderItem.getItemModelMesher.getModelManager.getModel()
////    wrappedModel
////  }
//
//  override def getFaceQuads(side: EnumFacing) = wrappedModel.getFaceQuads(side)
//
//  override def getGeneralQuads = wrappedModel.getGeneralQuads
//
//  override def isAmbientOcclusion = wrappedModel.isAmbientOcclusion
//
//  override def isGui3d = wrappedModel.isGui3d
//
//  override def isBuiltInRenderer = wrappedModel.isBuiltInRenderer
//
//  override def getTexture = wrappedModel.getTexture
//
//  override def getItemCameraTransforms = wrappedModel.getItemCameraTransforms
//}