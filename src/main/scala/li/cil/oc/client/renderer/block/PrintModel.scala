package li.cil.oc.client.renderer.block

import java.util
import java.util.Collections

import com.google.common.base.Strings
import li.cil.oc.Settings
import li.cil.oc.client.KeyBindings
import li.cil.oc.client.Textures
import li.cil.oc.common.block
import li.cil.oc.common.item.data.PrintData
import li.cil.oc.common.tileentity
import li.cil.oc.util.Color
import li.cil.oc.util.ExtendedAABB
import li.cil.oc.util.ExtendedAABB._
import net.minecraft.block.BlockState
import net.minecraft.client.world.ClientWorld
import net.minecraft.client.renderer.model.BakedQuad
import net.minecraft.client.renderer.model.IBakedModel
import net.minecraft.client.renderer.model.ItemOverrideList
import net.minecraft.client.renderer.texture.MissingTextureSprite
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import net.minecraft.entity.LivingEntity
import net.minecraft.item.DyeColor
import net.minecraft.item.ItemStack
import net.minecraft.util.Direction
import net.minecraft.util.ResourceLocation
import net.minecraftforge.client.model.data.IModelData

import scala.collection.JavaConverters.bufferAsJavaList
import scala.collection.mutable

object PrintModel extends SmartBlockModelBase {
  override def getOverrides: ItemOverrideList = ItemOverride

  override def getQuads(state: BlockState, side: Direction, rand: util.Random, data: IModelData): util.List[BakedQuad] =
    data match {
      case t: tileentity.Print =>
        val faces = mutable.ArrayBuffer.empty[BakedQuad]

        for (shape <- t.shapes if !Strings.isNullOrEmpty(shape.texture)) {
          val bounds = shape.bounds.rotateTowards(t.facing)
          val texture = resolveTexture(shape.texture)
          faces ++= bakeQuads(makeBox(bounds.minVec, bounds.maxVec), Array.fill(6)(texture), shape.tint.getOrElse(White))
        }

        bufferAsJavaList(faces)
      case _ => super.getQuads(state, side, rand)
    }

  private def resolveTexture(name: String): TextureAtlasSprite = try {
    val texture = Textures.getSprite(new ResourceLocation(name))
    if (texture.getName == MissingTextureSprite.getLocation) Textures.getSprite(new ResourceLocation("minecraft:blocks/" + name))
    else texture
  }
  catch {
    case _: Throwable => Textures.getSprite(MissingTextureSprite.getLocation)
  }

  class ItemModel(val stack: ItemStack) extends SmartBlockModelBase {
    val data = new PrintData(stack)

    override def getQuads(state: BlockState, side: Direction, rand: util.Random): util.List[BakedQuad] = {
      val faces = mutable.ArrayBuffer.empty[BakedQuad]

      val shapes =
        if (data.hasActiveState && KeyBindings.showExtendedTooltips)
          data.stateOn
        else
          data.stateOff
      for (shape <- shapes) {
        val bounds = shape.bounds
        val texture = resolveTexture(shape.texture)
        faces ++= bakeQuads(makeBox(bounds.minVec, bounds.maxVec), Array.fill(6)(texture), shape.tint.getOrElse(White))
      }
      if (shapes.isEmpty) {
        val bounds = ExtendedAABB.unitBounds
        val texture = resolveTexture(Settings.resourceDomain + ":blocks/white")
        faces ++= bakeQuads(makeBox(bounds.minVec, bounds.maxVec), Array.fill(6)(texture), Color.rgbValues(DyeColor.LIME))
      }

      bufferAsJavaList(faces)
    }
  }

  object ItemOverride extends ItemOverrideList {
    override def resolve(originalModel: IBakedModel, stack: ItemStack, world: ClientWorld, entity: LivingEntity): IBakedModel = new ItemModel(stack)
  }

}
