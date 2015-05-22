package li.cil.oc.common.item

import java.util
import java.util.UUID

import li.cil.oc.Constants
import li.cil.oc.OpenComputers
import li.cil.oc.Settings
import li.cil.oc.common.GuiType
import li.cil.oc.common.Tier
import li.cil.oc.common.tileentity
import li.cil.oc.server.{PacketSender => ServerPacketSender}
import li.cil.oc.util.BlockPosition
import li.cil.oc.util.ExtendedWorld._
import net.minecraft.client.resources.model.ModelBakery
import net.minecraft.client.resources.model.ModelResourceLocation
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.EnumFacing
import net.minecraft.world.World
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly

class Terminal(val parent: Delegator) extends traits.Delegate with CustomModel {
  override def maxStackSize = 1

  def hasServer(stack: ItemStack) = stack.hasTagCompound && stack.getTagCompound.hasKey(Settings.namespace + "server")

  @SideOnly(Side.CLIENT)
  override def tooltipLines(stack: ItemStack, player: EntityPlayer, tooltip: util.List[String], advanced: Boolean) {
    super.tooltipLines(stack, player, tooltip, advanced)
    if (hasServer(stack)) {
      val server = stack.getTagCompound.getString(Settings.namespace + "server")
      tooltip.add("§8" + server.substring(0, 13) + "...§7")
    }
  }

  @SideOnly(Side.CLIENT)
  private def modelLocationFromState(running: Boolean) = {
    new ModelResourceLocation(Settings.resourceDomain + ":" + Constants.ItemName.Terminal + (if (running) "_on" else "_off"), "inventory")
  }

  @SideOnly(Side.CLIENT)
  override def getModelLocation(stack: ItemStack): ModelResourceLocation = {
    modelLocationFromState(hasServer(stack))
  }

  @SideOnly(Side.CLIENT)
  override def registerModelLocations(): Unit = {
    for (state <- Seq(true, false)) {
      val location = modelLocationFromState(state)
      ModelBakery.addVariantName(parent, location.getResourceDomain + ":" + location.getResourcePath)
    }
  }

  override def onItemUse(stack: ItemStack, player: EntityPlayer, position: BlockPosition, side: EnumFacing, hitX: Float, hitY: Float, hitZ: Float) = {
    val world = position.world.get
    world.getTileEntity(position) match {
      case rack: tileentity.ServerRack if side == rack.facing =>
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
      case _ => super.onItemUse(stack, player, position, side, hitX, hitY, hitZ)
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
