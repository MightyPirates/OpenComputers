package li.cil.oc.common.block

import java.util

import li.cil.oc.Constants
import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.client.KeyBindings
import li.cil.oc.common.item.data.RobotData
import li.cil.oc.common.tileentity
import li.cil.oc.util.Color
import li.cil.oc.util.ItemCosts
import li.cil.oc.util.ItemUtils
import net.minecraft.block.Block
import net.minecraft.block.state.IBlockState
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.EnumDyeColor
import net.minecraft.item.EnumRarity
import net.minecraft.item.ItemBlock
import net.minecraft.item.ItemStack
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumFacing
import net.minecraft.util.StatCollector
import net.minecraft.world.World
import org.lwjgl.input

class Item(value: Block) extends ItemBlock(value) {
  setHasSubtypes(true)

  private lazy val Cases = Set(
    api.Items.get(Constants.BlockName.CaseTier1),
    api.Items.get(Constants.BlockName.CaseTier2),
    api.Items.get(Constants.BlockName.CaseTier3),
    api.Items.get(Constants.BlockName.CaseCreative)
  )

  private lazy val Screens = Set(
    api.Items.get(Constants.BlockName.ScreenTier1),
    api.Items.get(Constants.BlockName.ScreenTier2),
    api.Items.get(Constants.BlockName.ScreenTier3)
  )

  private lazy val Robot = api.Items.get(Constants.BlockName.Robot)

  override def addInformation(stack: ItemStack, player: EntityPlayer, tooltip: util.List[_], advanced: Boolean) {
    super.addInformation(stack, player, tooltip, advanced)
    (block, tooltip) match {
      case (simple: SimpleBlock, lines: util.List[String]@unchecked) =>
        simple.addInformation(getMetadata(stack.getItemDamage), stack, player, lines, advanced)

        if (input.Keyboard.isKeyDown(input.Keyboard.KEY_LMENU)) {
          ItemCosts.addTooltip(stack, lines)
        }
        else {
          lines.add(StatCollector.translateToLocalFormatted(
            Settings.namespace + "tooltip.MaterialCosts",
            input.Keyboard.getKeyName(KeyBindings.materialCosts.getKeyCode)))
        }
      case _ =>
    }
  }

  override def getColorFromItemStack(stack: ItemStack, tintIndex: Int) = {
    if (Screens.contains(api.Items.get(stack)))
      Color.rgbValues(EnumDyeColor.byDyeDamage(tintIndex))
    else if (Cases.contains(api.Items.get(stack)))
      Color.rgbValues(Color.byTier(ItemUtils.caseTier(stack)))
    else if (api.Items.get(stack) == Robot)
      tintIndex
    else super.getColorFromItemStack(stack, tintIndex)
  }

  override def getRarity(stack: ItemStack) = block match {
    case simple: SimpleBlock => simple.rarity(stack)
    case _ => EnumRarity.COMMON
  }

  override def getMetadata(itemDamage: Int) = itemDamage

  override def getUnlocalizedName = block match {
    case simple: SimpleBlock => simple.getUnlocalizedName
    case _ => Settings.namespace + "tile"
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
        case keyboard: tileentity.Keyboard =>
          keyboard.setFromEntityPitchAndYaw(player)
          keyboard.setFromFacing(side)
        case rotatable: tileentity.traits.Rotatable =>
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