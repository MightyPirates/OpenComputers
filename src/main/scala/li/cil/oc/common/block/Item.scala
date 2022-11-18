package li.cil.oc.common.block

import java.util

import li.cil.oc.Constants
import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.common.block
import li.cil.oc.common.item.data.MicrocontrollerData
import li.cil.oc.common.item.data.PrintData
import li.cil.oc.common.item.data.RobotData
import li.cil.oc.common.tileentity
import li.cil.oc.util.Color
import li.cil.oc.util.ItemColorizer
import li.cil.oc.util.Rarity
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.item // Rarity
import net.minecraft.item.BlockItem
import net.minecraft.item.BlockItemUseContext
import net.minecraft.item.DyeColor
import net.minecraft.item.Item.Properties
import net.minecraft.item.ItemStack
import net.minecraft.util.Direction
import net.minecraft.util.math.BlockRayTraceResult
import net.minecraft.util.text.ITextComponent
import net.minecraft.util.text.StringTextComponent

class Item(value: Block, props: Properties) extends BlockItem(value, props) {
  override def getRarity(stack: ItemStack): item.Rarity = getBlock match {
    case _: block.Microcontroller => {
      val data = new MicrocontrollerData(stack)
      Rarity.byTier(data.tier)
    }
    case _: block.RobotProxy => {
      val data = new RobotData(stack)
      Rarity.byTier(data.tier)
    }
    case _ => super.getRarity(stack)
  }

  override def getName(stack: ItemStack): ITextComponent = {
    if (api.Items.get(stack) == api.Items.get(Constants.BlockName.Print)) {
      val data = new PrintData(stack)
      data.label.map(new StringTextComponent(_)).getOrElse(super.getName(stack))
    }
    else super.getName(stack)
  }

  @Deprecated
  override def getDescriptionId: String = getBlock match {
    case simple: SimpleBlock => simple.getDescriptionId
    case _ => Settings.namespace + "tile"
  }

  override def getDamage(stack: ItemStack): Int = {
    if (api.Items.get(stack) == api.Items.get(Constants.BlockName.Cable)) {
      if (ItemColorizer.hasColor(stack)) {
        ItemColorizer.getColor(stack)
      }
      else Color.rgbValues(DyeColor.LIGHT_GRAY)
    }
    else super.getDamage(stack)
  }

  override def setDamage(stack: ItemStack, damage: Int): Unit = {
    if (api.Items.get(stack) == api.Items.get(Constants.BlockName.Cable)) {
      if(damage != Color.rgbValues(DyeColor.LIGHT_GRAY)) {
        ItemColorizer.setColor(stack, damage)
      } else {
        ItemColorizer.removeColor(stack)
      }
    }
    else super.setDamage(stack, damage)
  }

  override def placeBlock(ctx: BlockItemUseContext, newState: BlockState): Boolean = {
    // When placing robots in creative mode, we have to copy the stack
    // manually before it's placed to ensure different component addresses
    // in the different robots, to avoid interference of screens e.g.
    val needsCopying = ctx.getPlayer.isCreative && api.Items.get(ctx.getItemInHand) == api.Items.get(Constants.BlockName.Robot)
    val ctxToUse = if (needsCopying) {
      val stackToUse = new RobotData(ctx.getItemInHand).copyItemStack()
      val hitResult = new BlockRayTraceResult(ctx.getClickLocation, ctx.getClickedFace, ctx.getClickedPos, ctx.isInside)
      new BlockItemUseContext(ctx.getLevel, ctx.getPlayer, ctx.getHand, stackToUse, hitResult)
    }
    else ctx
    if (super.placeBlock(ctxToUse, newState)) {
      // If it's a rotatable block try to make it face the player.
      ctx.getLevel.getBlockEntity(ctxToUse.getClickedPos) match {
        case keyboard: tileentity.Keyboard => // Ignore.
        case rotatable: tileentity.traits.Rotatable =>
          rotatable.setFromEntityPitchAndYaw(ctxToUse.getPlayer)
          if (!rotatable.validFacings.contains(rotatable.pitch)) {
            rotatable.pitch = rotatable.validFacings.headOption.getOrElse(Direction.NORTH)
          }
          if (!rotatable.isInstanceOf[tileentity.RobotProxy]) {
            rotatable.invertRotation()
          }
        case _ => // Ignore.
      }
      true
    }
    else false
  }
}
