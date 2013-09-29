package li.cil.oc.common.block

import li.cil.oc.common.tileentity.Rotatable
import net.minecraft.block.Block
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemBlock
import net.minecraft.item.ItemStack
import net.minecraft.world.World

/** Used to represent multiblocks when in item form. */
class Item(id: Int) extends ItemBlock(id) {
  setHasSubtypes(true)

  override def getMetadata(itemDamage: Int) = itemDamage

  override def getUnlocalizedName = "oc.block"

  override def getUnlocalizedName(item: ItemStack) =
    Block.blocksList(item.itemID) match {
      case multiBlock: Delegator => "oc.block." + multiBlock.getUnlocalizedName(item.getItemDamage)
      case block => block.getUnlocalizedName
    }

  override def isBookEnchantable(a: ItemStack, b: ItemStack) = false

  override def placeBlockAt(item: ItemStack, player: EntityPlayer, world: World, x: Int, y: Int, z: Int, side: Int, hitX: Float, hitY: Float, hitZ: Float, metadata: Int) = {
    if (super.placeBlockAt(item, player, world, x, y, z, side, hitX, hitY, hitZ, metadata)) {
      // If it's a rotatable block try to make it face the player.
      world.getBlockTileEntity(x, y, z) match {
        case rotatable: Rotatable =>
          rotatable.setFromEntityPitchAndYaw(player).invertRotation()
      }
      true
    }
    else false
  }
}