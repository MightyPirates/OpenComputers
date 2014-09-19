package li.cil.oc.common.block

import java.util

import li.cil.oc.client.KeyBindings
import li.cil.oc.common.tileentity
import li.cil.oc.util.{ItemCosts, ItemUtils}
import li.cil.oc.{Settings, api}
import net.minecraft.block.Block
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.{EnumRarity, ItemBlock, ItemStack}
import net.minecraft.util.StatCollector
import net.minecraft.world.World
import net.minecraftforge.common.util.ForgeDirection
import org.lwjgl.input

class Item(value: Block) extends ItemBlock(value) {
  setHasSubtypes(true)

  def block = field_150939_a

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

  override def getRarity(stack: ItemStack) = block match {
    case simple: SimpleBlock => simple.rarity
    case _ => EnumRarity.common
  }

  override def getMetadata(itemDamage: Int) = itemDamage

  override def getUnlocalizedName = Settings.namespace + "tile"

  override def isBookEnchantable(a: ItemStack, b: ItemStack) = false

  override def placeBlockAt(stack: ItemStack, player: EntityPlayer, world: World, x: Int, y: Int, z: Int, side: Int, hitX: Float, hitY: Float, hitZ: Float, metadata: Int) = {
    // When placing robots in creative mode, we have to copy the stack
    // manually before it's placed to ensure different component addresses
    // in the different robots, to avoid interference of screens e.g.
    val needsCopying = player.capabilities.isCreativeMode && api.Items.get(stack) == api.Items.get("robot")
    val stackToUse = if (needsCopying) new ItemUtils.RobotData(stack).copyItemStack() else stack
    if (super.placeBlockAt(stackToUse, player, world, x, y, z, side, hitX, hitY, hitZ, metadata)) {
      // If it's a rotatable block try to make it face the player.
      world.getTileEntity(x, y, z) match {
        case keyboard: tileentity.Keyboard =>
          keyboard.setFromEntityPitchAndYaw(player)
          keyboard.setFromFacing(ForgeDirection.getOrientation(side))
        case rotatable: tileentity.traits.Rotatable =>
          rotatable.setFromEntityPitchAndYaw(player)
          if (!rotatable.validFacings.contains(rotatable.pitch)) {
            rotatable.pitch = rotatable.validFacings.headOption.getOrElse(ForgeDirection.NORTH)
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