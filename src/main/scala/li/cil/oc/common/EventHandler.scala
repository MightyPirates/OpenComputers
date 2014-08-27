package li.cil.oc.common

import java.util
import java.util.logging.Level

import cpw.mods.fml.common._
import cpw.mods.fml.common.network.{IConnectionHandler, Player}
import li.cil.oc._
import li.cil.oc.api.Network
import li.cil.oc.client.renderer.PetRenderer
import li.cil.oc.client.{PacketSender => ClientPacketSender}
import li.cil.oc.common.tileentity.traits.power
import li.cil.oc.server.{PacketSender => ServerPacketSender}
import li.cil.oc.util.mods.Mods
import li.cil.oc.util.{LuaStateFactory, SideTracker, mods}
import net.minecraft.client.Minecraft
import net.minecraft.entity.player.{EntityPlayer, EntityPlayerMP}
import net.minecraft.inventory.IInventory
import net.minecraft.item.ItemStack
import net.minecraft.network.packet.{NetHandler, Packet1Login}
import net.minecraft.network.{INetworkManager, NetLoginHandler}
import net.minecraft.server.MinecraftServer
import net.minecraft.tileentity.TileEntity
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.ForgeSubscribe
import net.minecraftforge.event.world.WorldEvent

import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object EventHandler extends ITickHandler with IConnectionHandler with ICraftingHandler {
  val pending = mutable.Buffer.empty[() => Unit]

  def schedule(tileEntity: TileEntity) {
    if (SideTracker.isServer) pending.synchronized {
      pending += (() => Network.joinOrCreateNetwork(tileEntity))
    }
  }

  @Optional.Method(modid = Mods.IDs.ForgeMultipart)
  def schedule(tileEntity: () => TileEntity) {
    if (SideTracker.isServer) pending.synchronized {
      pending += (() => Network.joinOrCreateNetwork(tileEntity()))
    }
  }

  @Optional.Method(modid = Mods.IDs.IndustrialCraft2)
  def scheduleIC2Add(tileEntity: power.IndustrialCraft2Experimental) {
    if (SideTracker.isServer) pending.synchronized {
      pending += (() => if (!tileEntity.addedToIC2PowerGrid && !tileEntity.isInvalid) {
        MinecraftForge.EVENT_BUS.post(new ic2.api.energy.event.EnergyTileLoadEvent(tileEntity.asInstanceOf[ic2.api.energy.tile.IEnergyTile]))
        tileEntity.addedToIC2PowerGrid = true
      })
    }
  }

  @Optional.Method(modid = Mods.IDs.IndustrialCraft2Classic)
  def scheduleIC2Add(tileEntity: power.IndustrialCraft2Classic) {
    if (SideTracker.isServer) pending.synchronized {
      pending += (() => if (!tileEntity.addedToIC2PowerGrid && !tileEntity.isInvalid) {
        MinecraftForge.EVENT_BUS.post(new ic2classic.api.energy.event.EnergyTileLoadEvent(tileEntity.asInstanceOf[ic2classic.api.energy.tile.IEnergyTile]))
        tileEntity.addedToIC2PowerGrid = true
      })
    }
  }

  def scheduleWirelessRedstone(rs: server.component.RedstoneWireless) {
    if (SideTracker.isServer) pending.synchronized {
      pending += (() => if (!rs.owner.isInvalid) {
        mods.WirelessRedstone.addReceiver(rs)
        mods.WirelessRedstone.updateOutput(rs)
      })
    }
  }

  override def getLabel = "OpenComputers Network Initialization Ticker"

  override def ticks() = util.EnumSet.of(TickType.SERVER)

  override def tickStart(`type`: util.EnumSet[TickType], tickData: AnyRef*) {
    pending.synchronized {
      val adds = pending.toArray
      pending.clear()
      adds
    } foreach (callback => {
      try callback() catch {
        case t: Throwable => OpenComputers.log.log(Level.WARNING, "Error in scheduled tick action.", t)
      }
    })
  }

  override def tickEnd(`type`: util.EnumSet[TickType], tickData: AnyRef*) = {}

  def playerLoggedIn(player: Player, netHandler: NetHandler, manager: INetworkManager) {
    if (netHandler.isServerHandler) player match {
      case p: EntityPlayerMP =>
        if (!LuaStateFactory.isAvailable) {
          p.sendChatToPlayer(Localization.Chat.WarningLuaFallback)
        }
        if (Mods.ProjectRedTransmission.isAvailable && !mods.ProjectRed.isAPIAvailable) {
          p.sendChatToPlayer(Localization.Chat.WarningProjectRed)
        }
        if (!Settings.get.pureIgnorePower && Settings.get.ignorePower) {
          p.sendChatToPlayer(Localization.Chat.WarningPower)
        }
        OpenComputers.tampered match {
          case Some(event) => p.sendChatToPlayer(Localization.Chat.WarningFingerprint(event))
          case _ =>
        }
        ServerPacketSender.sendPetVisibility(None, Some(p))
        // Do update check in local games and for OPs.
        if (!MinecraftServer.getServer.isDedicatedServer || MinecraftServer.getServer.getConfigurationManager.isPlayerOpped(p.getCommandSenderName)) {
          Future {
            UpdateCheck.info onSuccess {
              case Some(release) => p.sendChatToPlayer(Localization.Chat.InfoNewVersion(release.tag_name))
            }
          }
        }
      case _ =>
    }
  }

  def connectionReceived(netHandler: NetLoginHandler, manager: INetworkManager) = null

  def connectionOpened(netClientHandler: NetHandler, server: String, port: Int, manager: INetworkManager) {
  }

  def connectionOpened(netClientHandler: NetHandler, server: MinecraftServer, manager: INetworkManager) {
  }

  def connectionClosed(manager: INetworkManager) {
  }

  def clientLoggedIn(clientHandler: NetHandler, manager: INetworkManager, login: Packet1Login) {
    val player = clientHandler.getPlayer
    if (player == Minecraft.getMinecraft.thePlayer) {
      PetRenderer.hidden.clear()
      if (Settings.get.hideOwnPet) {
        PetRenderer.hidden += player.getCommandSenderName
      }
      ClientPacketSender.sendPetVisibility()
    }
  }

  lazy val navigationUpgrade = api.Items.get("navigationUpgrade")

  override def onCrafting(player: EntityPlayer, craftedStack: ItemStack, inventory: IInventory) = {
    if (api.Items.get(craftedStack) == navigationUpgrade) {
      Option(api.Driver.driverFor(craftedStack)).foreach(driver =>
        for (i <- 0 until inventory.getSizeInventory) {
          val stack = inventory.getStackInSlot(i)
          if (stack != null && api.Items.get(stack) == navigationUpgrade) {
            // Restore the map currently used in the upgrade.
            val nbt = driver.dataTag(stack)
            val map = ItemStack.loadItemStackFromNBT(nbt.getCompoundTag(Settings.namespace + "map"))
            if (!player.inventory.addItemStackToInventory(map)) {
              player.dropPlayerItemWithRandomChoice(map, false)
            }
          }
        })
    }
  }

  override def onSmelting(player: EntityPlayer, item: ItemStack) {}

  @ForgeSubscribe
  def onWorldUnload(e: WorldEvent.Unload) {
    if (!e.world.isRemote) {
      import scala.collection.convert.WrapAsScala._
      e.world.loadedTileEntityList.collect {
        case te: tileentity.traits.TileEntity => te.dispose()
      }
    }
  }
}
