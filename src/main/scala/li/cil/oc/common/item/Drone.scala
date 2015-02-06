package li.cil.oc.common.item

import java.util

import li.cil.oc.client.KeyBindings
import li.cil.oc.common.entity
import li.cil.oc.common.item.data.MicrocontrollerData
import li.cil.oc.integration.util.NEI
import li.cil.oc.server.agent
import li.cil.oc.util.BlockPosition
import li.cil.oc.util.Rarity
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.util.EnumFacing

class Drone(val parent: Delegator) extends Delegate {
  NEI.hide(this)

  showInItemList = false

  override protected def tooltipExtended(stack: ItemStack, tooltip: util.List[String]): Unit = {
    if (KeyBindings.showExtendedTooltips) {
      val info = new MicrocontrollerData(stack)
      for (component <- info.components if component != null) {
        tooltip.add("- " + component.getDisplayName)
      }
    }
  }

  override def rarity(stack: ItemStack) = {
    val data = new MicrocontrollerData(stack)
    Rarity.byTier(data.tier)
  }

  override def onItemUse(stack: ItemStack, player: EntityPlayer, position: BlockPosition, side: EnumFacing, hitX: Float, hitY: Float, hitZ: Float) = {
    val world = position.world.get
    if (!world.isRemote) {
      val drone = new entity.Drone(world)
      player match {
        case fakePlayer: agent.Player =>
          drone.ownerName = fakePlayer.agent.ownerName
          drone.ownerUUID = fakePlayer.agent.ownerUUID
        case _ =>
          drone.ownerName = player.getName
          drone.ownerUUID = player.getGameProfile.getId
      }
      drone.initializeAfterPlacement(stack, player, position.offset(hitX * 1.1f, hitY * 1.1f, hitZ * 1.1f))
      world.spawnEntityInWorld(drone)
    }
    stack.stackSize -= 1
    true
  }
}
