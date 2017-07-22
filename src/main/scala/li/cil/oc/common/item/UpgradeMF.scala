package li.cil.oc.common.item

import java.util

import li.cil.oc.util.BlockPosition
import li.cil.oc.{Localization, Settings}
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound

class UpgradeMF(val parent: Delegator) extends traits.Delegate with traits.ItemTier {

  override def onItemUseFirst(stack: ItemStack, player: EntityPlayer, position: BlockPosition, side: Int, hitX: Float, hitY: Float, hitZ: Float): Boolean = {
    if (!player.worldObj.isRemote && player.isSneaking) {
      if (!stack.hasTagCompound) {
        stack.setTagCompound(new NBTTagCompound())
      }
      val data = stack.getTagCompound
      data.setIntArray(Settings.namespace + "coord", Array(position.x, position.y, position.z, player.worldObj.provider.dimensionId, side))
      return true
    }
    super.onItemUseFirst(stack, player, position, side, hitX, hitY, hitZ)
  }

  override protected def tooltipExtended(stack: ItemStack, tooltip: util.List[String]) {
    tooltip.add(Localization.Tooltip.MFULinked(stack.getTagCompound match {
      case data: NBTTagCompound => data.hasKey(Settings.namespace +"coord")
      case _ => false
    }))
  }
}
