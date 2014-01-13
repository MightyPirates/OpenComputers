package li.cil.oc.common.item

import li.cil.oc.OpenComputers
import li.cil.oc.common.GuiType
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.world.World

class Server(val parent: Delegator) extends Delegate {
  val unlocalizedName = "Server"

  override def onItemRightClick(stack: ItemStack, world: World, player: EntityPlayer) = {
    if (!player.isSneaking) {
      if (!world.isRemote) {
        player.openGui(OpenComputers, GuiType.Server.id, world, 0, 0, 0)
      }
      player.swingItem()
    }
    stack
  }
}
