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
import net.minecraft.block.state.IBlockState
import net.minecraft.client.renderer.block.model.BakedQuad
import net.minecraft.client.renderer.block.model.IBakedModel
import net.minecraft.client.renderer.block.model.ItemOverrideList
import net.minecraft.entity.EntityLivingBase
import net.minecraft.item.EnumDyeColor
import net.minecraft.item.ItemStack
import net.minecraft.util.EnumFacing
import net.minecraft.world.World
import net.minecraftforge.common.property.IExtendedBlockState

import scala.collection.convert.WrapAsJava.bufferAsJavaList
import scala.collection.mutable

object PrintModel extends SmartBlockModelBase {
  override def getOverrides: ItemOverrideList = ItemOverride

  override def getQuads(state: IBlockState, side: EnumFacing, rand: Long): util.List[BakedQuad] =
    state match {
      case extended: IExtendedBlockState =>
        extended.getValue(block.property.PropertyTile.Tile) match {
          case t: tileentity.Print =>
            val faces = mutable.ArrayBuffer.empty[BakedQuad]

            for (shape <- t.shapes if !Strings.isNullOrEmpty(shape.texture)) {
              val bounds = shape.bounds.rotateTowards(t.facing)
              val texture = resolveTexture(shape.texture)
              faces ++= bakeQuads(makeBox(bounds.min, bounds.max), Array.fill(6)(texture), shape.tint.getOrElse(White))
            }

            bufferAsJavaList(faces)
          case _ => super.getQuads(state, side, rand)
        }
      case _ => super.getQuads(state, side, rand)
    }

  private def resolveTexture(name: String) = {
    val texture = Textures.getSprite(name)
    if (texture.getIconName == "missingno") Textures.getSprite("minecraft:blocks/" + name)
    else texture
  }

  class ItemModel(val stack: ItemStack) extends SmartBlockModelBase {
    val data = new PrintData(stack)

    override def getQuads(state: IBlockState, side: EnumFacing, rand: Long): util.List[BakedQuad] = {
      val faces = mutable.ArrayBuffer.empty[BakedQuad]

      Textures.Block.bind()
      val shapes =
        if (data.hasActiveState && KeyBindings.showExtendedTooltips)
          data.stateOn
        else
          data.stateOff
      for (shape <- shapes) {
        val bounds = shape.bounds
        val texture = resolveTexture(shape.texture)
        faces ++= bakeQuads(makeBox(bounds.min, bounds.max), Array.fill(6)(texture), shape.tint.getOrElse(White))
      }
      if (shapes.isEmpty) {
        val bounds = ExtendedAABB.unitBounds
        val texture = resolveTexture(Settings.resourceDomain + ":blocks/white")
        faces ++= bakeQuads(makeBox(bounds.min, bounds.max), Array.fill(6)(texture), Color.rgbValues(EnumDyeColor.LIME))
      }

      bufferAsJavaList(faces)
    }
  }

  object ItemOverride extends ItemOverrideList(Collections.emptyList()) {
    override def handleItemState(originalModel: IBakedModel, stack: ItemStack, world: World, entity: EntityLivingBase): IBakedModel = new ItemModel(stack)
  }

}
