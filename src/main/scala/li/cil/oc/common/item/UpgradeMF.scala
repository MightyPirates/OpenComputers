package li.cil.oc.common.item

import java.util

import li.cil.oc.Settings
import li.cil.oc.util.ExtendedWorld._
import li.cil.oc.util.{BlockPosition, Tooltip}
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.tileentity.TileEntity

class UpgradeMF(val parent: Delegator) extends traits.Delegate with traits.ItemTier {

  override def onItemUseFirst(stack: ItemStack, player: EntityPlayer, position: BlockPosition, side: Int, hitX: Float, hitY: Float, hitZ: Float): Boolean = {
    if (!player.worldObj.isRemote && player.isSneaking) {
      player.worldObj.getTileEntity(position) match {
        case tile: TileEntity =>
          if (!stack.hasTagCompound) {
            stack.setTagCompound(new NBTTagCompound())
          }
          val data = stack.getTagCompound
          data.setIntArray(Settings.namespace + "coord", Array(position.x, position.y, position.z, player.worldObj.provider.dimensionId, side))
          return true
        case _ =>
      }
    }
    super.onItemUseFirst(stack, player, position, side, hitX, hitY, hitZ)
  }

  override protected def tooltipExtended(stack: ItemStack, tooltip: util.List[String]) {
    stack.getTagCompound match {
      case data: NBTTagCompound =>
        tooltip.addAll(Tooltip.get(super.unlocalizedName + (if (data.hasKey("coord")) ".Linked" else ".Unlinked")))
      case _ =>
    }
  }
}
