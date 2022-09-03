package li.cil.oc.common.block

import java.util
import li.cil.oc.Constants
import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.common.item.data.PrintData
import li.cil.oc.common.item.data.RobotData
import li.cil.oc.common.tileentity
import li.cil.oc.util.Color
import li.cil.oc.util.ItemColorizer
import net.minecraft.block.Block
import net.minecraft.block.state.IBlockState
import net.minecraft.client.util.ITooltipFlag
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.EnumDyeColor
import net.minecraft.item.EnumRarity
import net.minecraft.item.ItemBlock
import net.minecraft.item.ItemStack
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import net.minecraftforge.fml.relauncher.{Side, SideOnly}

class Item(value: Block) extends ItemBlock(value) {
  setHasSubtypes(true)

  override def addInformation(stack: ItemStack, world: World, tooltip: util.List[String], flag: ITooltipFlag) {
    super.addInformation(stack, world, tooltip, flag)
    block match {
      case (simple: SimpleBlock) =>
        simple.addInformation(getMetadata(stack.getItemDamage), stack, world, tooltip, flag)
      case _ =>
    }
  }

  override def getRarity(stack: ItemStack): EnumRarity = block match {
    case simple: SimpleBlock => simple.rarity(stack)
    case _ => EnumRarity.COMMON
  }

  override def getMetadata(itemDamage: Int): Int = itemDamage

  override def getItemStackDisplayName(stack: ItemStack): String = {
    if (api.Items.get(stack) == api.Items.get(Constants.BlockName.Print)) {
      val data = new PrintData(stack)
      data.label.getOrElse(super.getItemStackDisplayName(stack))
    }
    else super.getItemStackDisplayName(stack)
  }

  override def getTranslationKey: String = block match {
    case simple: SimpleBlock => simple.getTranslationKey
    case _ => Settings.namespace + "tile"
  }

  override def isBookEnchantable(a: ItemStack, b: ItemStack) = false

  override def placeBlockAt(stack: ItemStack, player: EntityPlayer, world: World, pos: BlockPos, side: EnumFacing, hitX: Float, hitY: Float, hitZ: Float, newState: IBlockState): Boolean = {
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
