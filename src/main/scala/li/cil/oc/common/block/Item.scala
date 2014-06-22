package li.cil.oc.common.block

import java.util

import li.cil.oc.common.tileentity
import li.cil.oc.util.ItemUtils
import li.cil.oc.{Settings, api}
import net.minecraft.block.Block
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.{EnumRarity, ItemBlock, ItemStack}
import net.minecraft.world.World
import net.minecraftforge.common.ForgeDirection

class Item(id: Int) extends ItemBlock(id) {
  setHasSubtypes(true)

  override def addInformation(stack: ItemStack, player: EntityPlayer, tooltip: util.List[_], advanced: Boolean) {
    super.addInformation(stack, player, tooltip, advanced)
    Block.blocksList(getBlockID) match {
      case delegator: Delegator[_] => delegator.addInformation(getMetadata(stack.getItemDamage), stack, player, tooltip.asInstanceOf[util.List[String]], advanced)
      case _ =>
    }
  }

  override def getRarity(stack: ItemStack) = Delegator.subBlock(stack) match {
    case Some(subBlock) => subBlock.rarity
    case _ => EnumRarity.common
  }

  override def getMetadata(itemDamage: Int) = itemDamage

  override def getUnlocalizedName = Settings.namespace + "tile"

  override def getUnlocalizedName(stack: ItemStack) =
    Block.blocksList(getBlockID) match {
      case delegator: Delegator[_] => Settings.namespace + "tile." + delegator.getUnlocalizedName(stack.getItemDamage)
      case block => block.getUnlocalizedName
    }

  override def isBookEnchantable(a: ItemStack, b: ItemStack) = false

  override def placeBlockAt(stack: ItemStack, player: EntityPlayer, world: World, x: Int, y: Int, z: Int, side: Int, hitX: Float, hitY: Float, hitZ: Float, metadata: Int) = {
    // When placing robots in creative mode, we have to copy the stack
    // manually before it's placed to ensure different component addresses
    // in the different robots, to avoid interference of screens e.g.
    val needsCopying = player.capabilities.isCreativeMode && api.Items.get(stack) == api.Items.get("robot")
    val stackToUse = if (needsCopying) new ItemUtils.RobotData(stack).copyItemStack() else stack
    if (super.placeBlockAt(stackToUse, player, world, x, y, z, side, hitX, hitY, hitZ, metadata)) {
      // If it's a rotatable block try to make it face the player.
      world.getBlockTileEntity(x, y, z) match {
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