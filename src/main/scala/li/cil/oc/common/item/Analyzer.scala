package li.cil.oc.common.item

import li.cil.oc.Localization
import li.cil.oc.api.network.Analyzable
import li.cil.oc.api.network._
import li.cil.oc.server.PacketSender
import li.cil.oc.util.BlockPosition
import li.cil.oc.util.ExtendedWorld._
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.item.ItemStack
import net.minecraftforge.common.util.ForgeDirection

class Analyzer(val parent: Delegator) extends Delegate {
  override def onItemUse(stack: ItemStack, player: EntityPlayer, position: BlockPosition, side: Int, hitX: Float, hitY: Float, hitZ: Float) = {
    val world = position.world.get
    player match {
      case realPlayer: EntityPlayerMP =>
        world.getTileEntity(position) match {
          case analyzable: Analyzable =>
            if (!world.isRemote) {
              analyzeNodes(analyzable.onAnalyze(realPlayer, side, hitX, hitY, hitZ), realPlayer)
            }
            true
          case host: SidedEnvironment =>
            if (!world.isRemote) {
              analyzeNodes(Array(host.sidedNode(ForgeDirection.getOrientation(side))), realPlayer)
            }
            true
          case host: Environment =>
            if (!world.isRemote) {
              analyzeNodes(Array(host.node), realPlayer)
            }
            true
          case _ => super.onItemUse(stack, realPlayer, position, side, hitX, hitY, hitZ)
        }
      case _ => false
    }
  }

  private def analyzeNodes(nodes: Array[Node], player: EntityPlayerMP) = if (nodes != null) for (node <- nodes if node != null) {
    node match {
      case connector: Connector =>
        if (connector.localBufferSize > 0) {
          player.addChatMessage(Localization.Analyzer.StoredEnergy(f"${connector.localBuffer}%.2f/${connector.localBufferSize}%.2f"))
        }
        player.addChatMessage(Localization.Analyzer.TotalEnergy(f"${connector.globalBuffer}%.2f/${connector.globalBufferSize}%.2f"))
      case _ =>
    }
    node match {
      case component: Component =>
        player.addChatMessage(Localization.Analyzer.ComponentName(component.name))
      case _ =>
    }
    val address = node.address()
    if (address != null && !address.isEmpty) {
      player.addChatMessage(Localization.Analyzer.Address(address))
      PacketSender.sendAnalyze(address, player)
    }
  }
}
