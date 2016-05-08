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
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.item.ItemStack
import net.minecraft.util.ActionResult
import net.minecraft.util.EnumFacing
import net.minecraft.world.World
import net.minecraftforge.common.util.FakePlayer
import net.minecraftforge.event.entity.player.PlayerInteractEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object Analyzer {
  private lazy val analyzer = api.Items.get(Constants.ItemName.Analyzer)

  @SubscribeEvent
  def onInteract(e: PlayerInteractEvent.EntityInteract): Unit = {
    val player = e.getEntityPlayer
    val held = player.getHeldItem(e.getHand)
    if (api.Items.get(held) == analyzer) {
      if (analyze(e.getTarget, player, EnumFacing.DOWN, 0, 0, 0)) {
        player.swingArm(e.getHand)
        e.setCanceled(true)
      }
    }
  }

  def analyze(thing: AnyRef, player: EntityPlayer, side: EnumFacing, hitX: Float, hitY: Float, hitZ: Float): Boolean = {
    val world = player.worldObj
    thing match {
      case analyzable: Analyzable =>
        if (!world.isRemote) {
          analyzeNodes(analyzable.onAnalyze(player, side, hitX, hitY, hitZ), player)
        }
        true
      case host: SidedEnvironment =>
        if (!world.isRemote) {
          analyzeNodes(Array(host.sidedNode(side)), player)
        }
        true
      case host: Environment =>
        if (!world.isRemote) {
          analyzeNodes(Array(host.node), player)
        }
        true
      case _ =>
        false
    }
  }

  private def analyzeNodes(nodes: Array[Node], player: EntityPlayer) = if (nodes != null) for (node <- nodes if node != null) {
    player match {
      case _: FakePlayer => // Nope
      case playerMP: EntityPlayerMP =>
        if (node != null) node.host match {
          case machine: Machine =>
            if (machine != null) {
              if (machine.lastError != null) {
                playerMP.addChatMessage(Localization.Analyzer.LastError(machine.lastError))
              }
              playerMP.addChatMessage(Localization.Analyzer.Components(machine.componentCount, machine.maxComponents))
              val list = machine.users
              if (list.nonEmpty) {
                playerMP.addChatMessage(Localization.Analyzer.Users(list))
              }
            }
          case _ =>
        }
        node match {
          case connector: Connector =>
            if (connector.localBufferSize > 0) {
              playerMP.addChatMessage(Localization.Analyzer.StoredEnergy(f"${connector.localBuffer}%.2f/${connector.localBufferSize}%.2f"))
            }
            playerMP.addChatMessage(Localization.Analyzer.TotalEnergy(f"${connector.globalBuffer}%.2f/${connector.globalBufferSize}%.2f"))
          case _ =>
        }
        node match {
          case component: Component =>
            playerMP.addChatMessage(Localization.Analyzer.ComponentName(component.name))
          case _ =>
        }
        val address = node.address()
        if (address != null && !address.isEmpty) {
          playerMP.addChatMessage(Localization.Analyzer.Address(address))
          PacketSender.sendAnalyze(address, playerMP)
        }
      case _ =>
    }
  }
}

class Analyzer(val parent: Delegator) extends traits.Delegate {
  override def onItemRightClick(stack: ItemStack, world: World, player: EntityPlayer): ActionResult[ItemStack] = {
    if (player.isSneaking && stack.hasTagCompound) {
      stack.getTagCompound.removeTag(Settings.namespace + "clipboard")
      if (stack.getTagCompound.hasNoTags) {
        stack.setTagCompound(null)
      }
    }
    super.onItemRightClick(stack, world, player)
  }

  override def onItemUse(stack: ItemStack, player: EntityPlayer, position: BlockPosition, side: EnumFacing, hitX: Float, hitY: Float, hitZ: Float) = {
    val world = player.getEntityWorld
    world.getTileEntity(position) match {
      case screen: tileentity.Screen if side == screen.facing =>
        if (player.isSneaking) {
          screen.copyToAnalyzer(hitX, hitY, hitZ)
        }
        else if (stack.hasTagCompound && stack.getTagCompound.hasKey(Settings.namespace + "clipboard")) {
          if (!world.isRemote) {
            screen.origin.buffer.clipboard(stack.getTagCompound.getString(Settings.namespace + "clipboard"), player)
          }
          true
        }
        else false
      case _ => Analyzer.analyze(position.world.get.getTileEntity(position), player, side, hitX, hitY, hitZ)
    }
  }
}
