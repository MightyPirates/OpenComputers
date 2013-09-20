package li.cil.oc.common.block

import net.minecraft.item.ItemBlock
import li.cil.oc.Config
import net.minecraft.item.ItemStack
import net.minecraft.block.Block
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.world.World
import li.cil.oc.Blocks
import net.minecraftforge.common.ForgeDirection
import li.cil.oc.common.tileentity.TileEntityRotatable

/** Used to represent multiblocks when in item form. */
class ItemBlockMulti(id: Int) extends ItemBlock(id) {
  setHasSubtypes(true)

  override def getMetadata(itemDamage: Int) = itemDamage

  override def getUnlocalizedName = "oc.block"

  override def getUnlocalizedName(item: ItemStack) =
    Block.blocksList(item.itemID) match {
      case multiBlock: BlockMulti => "oc.block." + multiBlock.getUnlocalizedName(item.getItemDamage)
      case block => block.getUnlocalizedName
    }

  override def isBookEnchantable(a: ItemStack, b: ItemStack) = false

  override def placeBlockAt(item: ItemStack, player: EntityPlayer, world: World, x: Int, y: Int, z: Int, side: Int, hitX: Float, hitY: Float, hitZ: Float, metadata: Int) = {
    if (super.placeBlockAt(item, player, world, x, y, z, side, hitX, hitY, hitZ, metadata)) {
      // If it's a rotatable block try to make it face the player.
      world.getBlockTileEntity(x, y, z) match {
        case rotatable: TileEntityRotatable =>
          rotatable.setFromEntityPitchAndYaw(player).invertRotation()
      }
      true
    }
    else false
  }
}