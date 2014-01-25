package li.cil.oc.common.item

import java.util
import li.cil.oc.Settings
import li.cil.oc.api.network._
import li.cil.oc.util.Tooltip
import net.minecraft.client.renderer.texture.IconRegister
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.util.ChatMessageComponent
import net.minecraft.world.World
import net.minecraftforge.common.ForgeDirection
import li.cil.oc.server.PacketSender
import cpw.mods.fml.common.network.Player

class Analyzer(val parent: Delegator) extends Delegate {
  val unlocalizedName = "Analyzer"

  override def tooltipLines(stack: ItemStack, player: EntityPlayer, tooltip: util.List[String], advanced: Boolean) {
    tooltip.addAll(Tooltip.get(unlocalizedName))
  }

  override def onItemUse(stack: ItemStack, player: EntityPlayer, world: World, x: Int, y: Int, z: Int, side: Int, hitX: Float, hitY: Float, hitZ: Float) = {
    world.getBlockTileEntity(x, y, z) match {
      case analyzable: Analyzable =>
        if (!world.isRemote) {
          analyzeNodes(analyzable.onAnalyze(player, side, hitX, hitY, hitZ), player)
        }
        true
      case host: SidedEnvironment =>
        if (!world.isRemote) {
          analyzeNodes(Array(host.sidedNode(ForgeDirection.getOrientation(side))), player)
        }
        true
      case host: Environment =>
        if (!world.isRemote) {
          analyzeNodes(Array(host.node), player)
        }
        true
      case _ => super.onItemUse(stack, player, world, x, y, z, side, hitX, hitY, hitZ)
    }
  }

  private def analyzeNodes(nodes: Array[Node], player: EntityPlayer) = if (nodes != null) for (node <- nodes) {
    node match {
      case connector: Connector =>
        if (connector.localBufferSize > 0) {
          player.sendChatToPlayer(ChatMessageComponent.createFromTranslationWithSubstitutions(
            Settings.namespace + "gui.Analyzer.StoredEnergy",
            "%.2f/%.2f".format(connector.localBuffer, connector.localBufferSize)))
        }
        player.sendChatToPlayer(ChatMessageComponent.createFromTranslationWithSubstitutions(
          Settings.namespace + "gui.Analyzer.TotalEnergy",
          "%.2f/%.2f".format(connector.globalBuffer, connector.globalBufferSize)))
      case _ =>
    }
    node match {
      case component: Component =>
        player.sendChatToPlayer(ChatMessageComponent.createFromTranslationWithSubstitutions(
          Settings.namespace + "gui.Analyzer.ComponentName",
          component.name))
      case _ =>
    }
    val address = node.address()
    if (address != null && !address.isEmpty) {
      player.sendChatToPlayer(ChatMessageComponent.createFromTranslationWithSubstitutions(
        Settings.namespace + "gui.Analyzer.Address",
        address))
    }
    PacketSender.sendAnalyze(address, player.asInstanceOf[Player])
  }

  override def registerIcons(iconRegister: IconRegister) {
    super.registerIcons(iconRegister)

    icon = iconRegister.registerIcon(Settings.resourceDomain + ":analyzer")
  }
}
