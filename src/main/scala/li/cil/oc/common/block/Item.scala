package li.cil.oc.common.block

import java.util

import li.cil.oc.Constants
import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.client.KeyBindings
import li.cil.oc.common.item.data.PrintData
import li.cil.oc.common.item.data.RobotData
import li.cil.oc.common.tileentity
import li.cil.oc.common.tileentity.TileEntityKeyboard
import li.cil.oc.common.tileentity.traits.RotatableImpl
import li.cil.oc.util.{DyeUtils, ItemColorizer, ItemCosts}
import net.minecraft.block.Block
import net.minecraft.block.state.IBlockState
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.EnumDyeColor
import net.minecraft.item.EnumRarity
import net.minecraft.item.ItemBlock
import net.minecraft.item.ItemStack
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.BlockPos
import net.minecraft.util.text.translation.I18n
import net.minecraft.world.World

class Item(value: Block) extends ItemBlock(value) {
  setHasSubtypes(true)

  override def addInformation(stack: ItemStack, player: EntityPlayer, tooltip: util.List[String], advanced: Boolean) {
    super.addInformation(stack, player, tooltip, advanced)
    block match {
      case (simple: AbstractBlock) =>
        simple.addInformation(getMetadata(stack.getItemDamage), stack, player, tooltip, advanced)

        if (KeyBindings.showMaterialCosts) {
          ItemCosts.addTooltip(stack, tooltip)
        }
        else {
          tooltip.add(I18n.translateToLocalFormatted(
            Settings.namespace + "tooltip.MaterialCosts",
            KeyBindings.getKeyBindingName(KeyBindings.materialCosts)))
        }
      case _ =>
    }
  }

  override def getRarity(stack: ItemStack) = block match {
    case simple: AbstractBlock => simple.rarity(stack)
    case _ => EnumRarity.COMMON
  }

  override def getMetadata(itemDamage: Int) = itemDamage

  override def getItemStackDisplayName(stack: ItemStack): String = {
    if (api.Items.get(stack) == api.Items.get(Constants.BlockName.Print)) {
      val data = new PrintData(stack)
      data.label.getOrElse(super.getItemStackDisplayName(stack))
    }
    else super.getItemStackDisplayName(stack)
  }

  override def getUnlocalizedName = block match {
    case simple: AbstractBlock => simple.getUnlocalizedName
    case _ => Settings.namespace + "tile"
  }

  override def getDamage(stack: ItemStack): Int = {
    if (api.Items.get(stack) == api.Items.get(Constants.BlockName.Cable)) {
      if (ItemColorizer.hasColor(stack)) {
        ItemColorizer.getColor(stack)
      }
      else DyeUtils.rgbValues(EnumDyeColor.SILVER)
    }
    else super.getDamage(stack)
  }

  override def isBookEnchantable(a: ItemStack, b: ItemStack) = false

  override def placeBlockAt(stack: ItemStack, player: EntityPlayer, world: World, pos: BlockPos, side: EnumFacing, hitX: Float, hitY: Float, hitZ: Float, newState: IBlockState) = {
    // When placing robots in creative mode, we have to copy the stack
    // manually before it's placed to ensure different component addresses
    // in the different robots, to avoid interference of screens e.g.
    val needsCopying = player.capabilities.isCreativeMode && api.Items.get(stack) == api.Items.get(Constants.BlockName.Robot)
    val stackToUse = if (needsCopying) new RobotData(stack).copyItemStack() else stack
    if (super.placeBlockAt(stackToUse, player, world, pos, side, hitX, hitY, hitZ, newState)) {
      // If it's a rotatable block try to make it face the player.
      world.getTileEntity(pos) match {
        case keyboard: TileEntityKeyboard =>
          keyboard.setFromEntityPitchAndYaw(player)
          keyboard.setFromFacing(side)
        case rotatable: RotatableImpl =>
          rotatable.setFromEntityPitchAndYaw(player)
          if (!rotatable.validFacings.contains(rotatable.pitch)) {
            rotatable.pitch = rotatable.validFacings.headOption.getOrElse(EnumFacing.NORTH)
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