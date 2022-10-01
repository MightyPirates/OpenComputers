package li.cil.oc.common.item

import li.cil.oc.Constants
import li.cil.oc.Localization
import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.api.machine.Machine
import li.cil.oc.api.network.Analyzable
import li.cil.oc.api.network._
import li.cil.oc.common.tileentity
import li.cil.oc.server.PacketSender
import li.cil.oc.util.BlockPosition
import li.cil.oc.util.ExtendedWorld._
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.ServerPlayerEntity
import net.minecraft.item.Item
import net.minecraft.item.Item.Properties
import net.minecraft.item.ItemStack
import net.minecraft.util.ActionResult
import net.minecraft.util.Direction
import net.minecraft.util.Util
import net.minecraft.world.World
import net.minecraftforge.common.extensions.IForgeItem
import net.minecraftforge.common.util.FakePlayer
import net.minecraftforge.event.entity.player.PlayerInteractEvent
import net.minecraftforge.eventbus.api.SubscribeEvent

object Analyzer {
  private lazy val analyzer = api.Items.get(Constants.ItemName.Analyzer)

  @SubscribeEvent
  def onInteract(e: PlayerInteractEvent.EntityInteract): Unit = {
    val player = e.getPlayer
    val held = player.getItemInHand(e.getHand)
    if (api.Items.get(held) == analyzer) {
      if (analyze(e.getTarget, player, Direction.DOWN, 0, 0, 0)) {
        player.swing(e.getHand)
        e.setCanceled(true)
      }
    }
  }

  def analyze(thing: AnyRef, player: PlayerEntity, side: Direction, hitX: Float, hitY: Float, hitZ: Float): Boolean = {
    val world = player.level
    thing match {
      case analyzable: Analyzable =>
        if (!world.isClientSide) {
          analyzeNodes(analyzable.onAnalyze(player, side, hitX, hitY, hitZ), player)
        }
        true
      case host: SidedEnvironment =>
        if (!world.isClientSide) {
          analyzeNodes(Array(host.sidedNode(side)), player)
        }
        true
      case host: Environment =>
        if (!world.isClientSide) {
          analyzeNodes(Array(host.node), player)
        }
        true
      case _ =>
        false
    }
  }

  private def analyzeNodes(nodes: Array[Node], player: PlayerEntity) = if (nodes != null) for (node <- nodes if node != null) {
    player match {
      case _: FakePlayer => // Nope
      case playerMP: ServerPlayerEntity =>
        if (node != null) node.host match {
          case machine: Machine =>
            if (machine != null) {
              if (machine.lastError != null) {
                playerMP.sendMessage(Localization.Analyzer.LastError(machine.lastError), Util.NIL_UUID)
              }
              playerMP.sendMessage(Localization.Analyzer.Components(machine.componentCount, machine.maxComponents), Util.NIL_UUID)
              val list = machine.users
              if (list.nonEmpty) {
                playerMP.sendMessage(Localization.Analyzer.Users(list), Util.NIL_UUID)
              }
            }
          case _ =>
        }
        node match {
          case connector: Connector =>
            if (connector.localBufferSize > 0) {
              playerMP.sendMessage(Localization.Analyzer.StoredEnergy(f"${connector.localBuffer}%.2f/${connector.localBufferSize}%.2f"), Util.NIL_UUID)
            }
            playerMP.sendMessage(Localization.Analyzer.TotalEnergy(f"${connector.globalBuffer}%.2f/${connector.globalBufferSize}%.2f"), Util.NIL_UUID)
          case _ =>
        }
        node match {
          case component: Component =>
            playerMP.sendMessage(Localization.Analyzer.ComponentName(component.name), Util.NIL_UUID)
          case _ =>
        }
        val address = node.address()
        if (address != null && !address.isEmpty) {
          playerMP.sendMessage(Localization.Analyzer.Address(address), Util.NIL_UUID)
          PacketSender.sendAnalyze(address, playerMP)
        }
      case _ =>
    }
  }
}

class Analyzer(props: Properties) extends Item(props) with IForgeItem with traits.SimpleItem {
  override def use(stack: ItemStack, world: World, player: PlayerEntity): ActionResult[ItemStack] = {
    if (player.isCrouching && stack.hasTag) {
      stack.removeTagKey(Settings.namespace + "clipboard")
    }
    super.use(stack, world, player)
  }

  override def onItemUse(stack: ItemStack, player: PlayerEntity, position: BlockPosition, side: Direction, hitX: Float, hitY: Float, hitZ: Float) = {
    val world = player.level
    world.getBlockEntity(position) match {
      case screen: tileentity.Screen if side == screen.facing =>
        if (player.isCrouching) {
          screen.copyToAnalyzer(hitX, hitY, hitZ)
        }
        else if (stack.hasTag && stack.getTag.contains(Settings.namespace + "clipboard")) {
          if (!world.isClientSide) {
            screen.origin.buffer.clipboard(stack.getTag.getString(Settings.namespace + "clipboard"), player)
          }
          true
        }
        else false
      case _ => Analyzer.analyze(position.world.get.getBlockEntity(position), player, side, hitX, hitY, hitZ)
    }
  }
}
