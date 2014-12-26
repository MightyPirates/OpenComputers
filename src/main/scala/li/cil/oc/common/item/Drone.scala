package li.cil.oc.common.item

import java.util

import li.cil.oc.client.KeyBindings
import li.cil.oc.common.entity
import li.cil.oc.util.BlockPosition
import li.cil.oc.util.ItemUtils
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.util.EnumFacing

class Drone(val parent: Delegator) extends Delegate {
  override protected def tooltipExtended(stack: ItemStack, tooltip: util.List[String]): Unit = {
    if (KeyBindings.showExtendedTooltips) {
      val info = new ItemUtils.MicrocontrollerData(stack)
      for (component <- info.components) {
        tooltip.add("- " + component.getDisplayName)
      }
    }
  }

  override def onItemUse(stack: ItemStack, player: EntityPlayer, position: BlockPosition, side: EnumFacing, hitX: Float, hitY: Float, hitZ: Float) = {
    val world = position.world.get
    if (!world.isRemote) {
      val drone = new entity.Drone(world)
      drone.initializeAfterPlacement(stack, player, position.offset(hitX * 1.1f, hitY * 1.1f, hitZ * 1.1f))
      world.spawnEntityInWorld(drone)
    }
    stack.stackSize -= 1
    true
  }
}
