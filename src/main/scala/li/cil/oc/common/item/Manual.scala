package li.cil.oc.common.item

import li.cil.oc.OpenComputers
import li.cil.oc.api
import li.cil.oc.client.gui
import li.cil.oc.common.GuiType
import li.cil.oc.common.block.SimpleBlock
import li.cil.oc.util.BlockPosition
import li.cil.oc.util.ExtendedWorld._
import net.minecraft.client.Minecraft
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.world.World

class Manual(val parent: Delegator) extends Delegate {
  override def onItemRightClick(stack: ItemStack, world: World, player: EntityPlayer): ItemStack = {
    if (world.isRemote) {
      if (player.isSneaking) {
        gui.Manual.reset()
      }
      player.openGui(OpenComputers, GuiType.Manual.id, world, 0, 0, 0)
    }
    super.onItemRightClick(stack, world, player)
  }

  override def onItemUse(stack: ItemStack, player: EntityPlayer, position: BlockPosition, side: Int, hitX: Float, hitY: Float, hitZ: Float): Boolean = {
    val world = player.getEntityWorld
    world.getBlock(position) match {
      case block: SimpleBlock =>
        if (world.isRemote) {
          player.openGui(OpenComputers, GuiType.Manual.id, world, 0, 0, 0)
          Minecraft.getMinecraft.currentScreen match {
            case manual: gui.Manual =>
              gui.Manual.reset()
              val descriptor = api.Items.get(new ItemStack(block))
              manual.pushPage("block/" + descriptor.name + ".md")
            case _ =>
          }
        }
        true
      case _ => super.onItemUse(stack, player, position, side, hitX, hitY, hitZ)
    }
  }
}
