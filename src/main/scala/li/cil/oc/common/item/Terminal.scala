package li.cil.oc.common.item

import java.util
import java.util.UUID

import cpw.mods.fml.relauncher.{Side, SideOnly}
import li.cil.oc.common.{GuiType, Tier, tileentity}
import li.cil.oc.server.{PacketSender => ServerPacketSender}
import li.cil.oc.{OpenComputers, Settings}
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.world.World

class Terminal(val parent: Delegator) extends Delegate {
  override def maxStackSize = 1

  private var iconOn: Option[Icon] = None
  private var iconOff: Option[Icon] = None

  def hasServer(stack: ItemStack) = stack.hasTagCompound && stack.getTagCompound.hasKey(Settings.namespace + "server")

  @SideOnly(Side.CLIENT)
  override def tooltipLines(stack: ItemStack, player: EntityPlayer, tooltip: util.List[String], advanced: Boolean) {
    super.tooltipLines(stack, player, tooltip, advanced)
    if (hasServer(stack)) {
      val server = stack.getTagCompound.getString(Settings.namespace + "server")
      tooltip.add("§8" + server.substring(0, 13) + "...§7")
    }
  }

  // TODO check if server is in range and running
  // Unlike in the GUI handler the result should definitely be cached here.
  @SideOnly(Side.CLIENT)
  override def icon(stack: ItemStack, pass: Int) = if (hasServer(stack)) iconOn else iconOff

  override def registerIcons(iconRegister: IconRegister) = {
    super.registerIcons(iconRegister)

    iconOn = Option(iconRegister.registerIcon(Settings.resourceDomain + ":TerminalOn"))
    iconOff = Option(iconRegister.registerIcon(Settings.resourceDomain + ":TerminalOff"))
  }

  override def onItemUse(stack: ItemStack, player: EntityPlayer, world: World, x: Int, y: Int, z: Int, side: Int, hitX: Float, hitY: Float, hitZ: Float) = {
    world.getTileEntity(x, y, z) match {
      case rack: tileentity.ServerRack if side == rack.facing.ordinal() =>
        val l = 2 / 16.0
        val h = 14 / 16.0
        val slot = (((1 - hitY) - l) / (h - l) * 4).toInt
        if (slot >= 0 && slot <= 3 && rack.items(slot).isDefined) {
          if (!world.isRemote) {
            rack.servers(slot) match {
              case Some(server) =>
                val terminal = rack.terminals(slot)
                val key = UUID.randomUUID().toString
                val keys = terminal.keys
                if (!stack.hasTagCompound) {
                  stack.setTagCompound(new NBTTagCompound())
                }
                else {
                  keys -= stack.getTagCompound.getString(Settings.namespace + "key")
                }
                val maxSize = Settings.get.terminalsPerTier(math.min(Tier.Three, server.tier))
                while (keys.length >= maxSize) {
                  keys.remove(0)
                }
                keys += key
                terminal.connect(server.machine.node)
                ServerPacketSender.sendServerState(rack, slot)
                stack.getTagCompound.setString(Settings.namespace + "key", key)
                stack.getTagCompound.setString(Settings.namespace + "server", server.machine.node.address)
                player.inventory.markDirty()
              case _ => // Huh?
            }
          }
          true
        }
        else false
      case _ => super.onItemUse(stack, player, world, x, y, z, side, hitX, hitY, hitZ)
    }
  }

  override def onItemRightClick(stack: ItemStack, world: World, player: EntityPlayer) = {
    if (!player.isSneaking && stack.hasTagCompound) {
      val key = stack.getTagCompound.getString(Settings.namespace + "key")
      val server = stack.getTagCompound.getString(Settings.namespace + "server")
      if (key != null && !key.isEmpty && server != null && !server.isEmpty) {
        if (world.isRemote) {
          player.openGui(OpenComputers, GuiType.Terminal.id, world, 0, 0, 0)
        }
        player.swingItem()
      }
    }
    super.onItemRightClick(stack, world, player)
  }
}
