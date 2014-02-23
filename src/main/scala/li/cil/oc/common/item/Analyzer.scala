package li.cil.oc.common.item

import java.util
import li.cil.oc.Settings
import li.cil.oc.api.network._
import li.cil.oc.server.PacketSender
import li.cil.oc.util.Tooltip
import net.minecraft.client.renderer.texture.IIconRegister
import net.minecraft.entity.player.{EntityPlayerMP, EntityPlayer}
import net.minecraft.item.ItemStack
import net.minecraft.util.ChatComponentTranslation
import net.minecraft.world.World
import net.minecraftforge.common.util.ForgeDirection

class Analyzer(val parent: Delegator) extends Delegate {
  val unlocalizedName = "Analyzer"

  override def tooltipLines(stack: ItemStack, player: EntityPlayer, tooltip: util.List[String], advanced: Boolean) {
    tooltip.addAll(Tooltip.get(unlocalizedName))
  }

  override def onItemUse(stack: ItemStack, player: EntityPlayer, world: World, x: Int, y: Int, z: Int, side: Int, hitX: Float, hitY: Float, hitZ: Float) = {
    player match {
      case realPlayer: EntityPlayerMP =>
        world.getTileEntity(x, y, z) match {
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
          case _ => super.onItemUse(stack, realPlayer, world, x, y, z, side, hitX, hitY, hitZ)
        }
      case _ => false
    }
  }

  private def analyzeNodes(nodes: Array[Node], player: EntityPlayerMP) = if (nodes != null) for (node <- nodes) {
    node match {
      case connector: Connector =>
        if (connector.localBufferSize > 0) {
          player.addChatMessage(new ChatComponentTranslation(
            Settings.namespace + "gui.Analyzer.StoredEnergy",
            "%.2f/%.2f".format(connector.localBuffer, connector.localBufferSize)))
        }
        player.addChatMessage(new ChatComponentTranslation(
          Settings.namespace + "gui.Analyzer.TotalEnergy",
          "%.2f/%.2f".format(connector.globalBuffer, connector.globalBufferSize)))
      case _ =>
    }
    node match {
      case component: Component =>
        player.addChatMessage(new ChatComponentTranslation(
          Settings.namespace + "gui.Analyzer.ComponentName",
          component.name))
      case _ =>
    }
    val address = node.address()
    if (address != null && !address.isEmpty) {
      player.addChatMessage(new ChatComponentTranslation(
        Settings.namespace + "gui.Analyzer.Address",
        address))
    }
    PacketSender.sendAnalyze(address, player)
  }

  override def registerIcons(iconRegister: IIconRegister) {
    super.registerIcons(iconRegister)

    icon = iconRegister.registerIcon(Settings.resourceDomain + ":analyzer")
  }
}
