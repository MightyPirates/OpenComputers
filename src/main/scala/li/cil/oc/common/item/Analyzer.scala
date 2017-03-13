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
    val world = player.world
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
      case host: NodeContainer =>
        if (!world.isRemote) {
          analyzeNodes(Array(host.getNode), player)
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
        if (node != null) node.getContainer match {
          case machine: Machine =>
            if (machine != null) {
              if (machine.lastError != null) {
                playerMP.sendMessage(Localization.Analyzer.LastError(machine.lastError))
              }
              playerMP.sendMessage(Localization.Analyzer.Components(machine.componentCount, machine.maxComponents))
              val list = machine.users
              if (list.nonEmpty) {
                playerMP.sendMessage(Localization.Analyzer.Users(list))
              }
            }
          case _ =>
        }
        node match {
          case connector: EnergyNode =>
            if (connector.getEnergyCapacity > 0) {
              playerMP.sendMessage(Localization.Analyzer.StoredEnergy(f"${connector.getEnergyStored}%.2f/${connector.getEnergyCapacity}%.2f"))
            }
            playerMP.sendMessage(Localization.Analyzer.TotalEnergy(f"${connector.getGlobalBuffer}%.2f/${connector.getGlobalBufferSize}%.2f"))
          case _ =>
        }
        node match {
          case component: ComponentNode =>
            playerMP.sendMessage(Localization.Analyzer.ComponentName(component.getName))
          case _ =>
        }
        val address = node.getAddress()
        if (address != null && !address.isEmpty) {
          playerMP.sendMessage(Localization.Analyzer.Address(address))
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
