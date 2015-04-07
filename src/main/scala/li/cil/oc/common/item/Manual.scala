package li.cil.oc.common.item

import li.cil.oc.OpenComputers
import li.cil.oc.common.GuiType
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.world.World

class Manual(val parent: Delegator) extends Delegate {
  override def onItemRightClick(stack: ItemStack, world: World, player: EntityPlayer): ItemStack = {
    player.openGui(OpenComputers, GuiType.Manual.id, world, 0, 0, 0)
    super.onItemRightClick(stack, world, player)
  }
}
