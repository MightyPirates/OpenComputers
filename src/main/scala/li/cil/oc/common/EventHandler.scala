package li.cil.oc.common

import cpw.mods.fml.common.Optional
import cpw.mods.fml.common.eventhandler.SubscribeEvent
import cpw.mods.fml.common.gameevent.PlayerEvent._
import cpw.mods.fml.common.gameevent.TickEvent
import cpw.mods.fml.common.gameevent.TickEvent.ServerTickEvent
import cpw.mods.fml.common.network.FMLNetworkEvent.ClientConnectedToServerEvent
import li.cil.oc._
import li.cil.oc.api.Network
import li.cil.oc.client.renderer.PetRenderer
import li.cil.oc.client.{PacketSender => ClientPacketSender}
import li.cil.oc.common.tileentity.traits.power
import li.cil.oc.server.{PacketSender => ServerPacketSender}
import li.cil.oc.util.mods.Mods
import li.cil.oc.util.{LuaStateFactory, SideTracker, mods}
import net.minecraft.client.Minecraft
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.item.ItemStack
import net.minecraft.server.MinecraftServer
import net.minecraft.tileentity.TileEntity
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.world.WorldEvent

import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object EventHandler {
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

  @SubscribeEvent
  def onTick(e: ServerTickEvent) = if (e.phase == TickEvent.Phase.START) {
    pending.synchronized {
      val adds = pending.toArray
      pending.clear()
      adds
    } foreach (callback => {
      try callback() catch {
        case t: Throwable => OpenComputers.log.warn("Error in scheduled tick action.", t)
      }
    })
  }

  @SubscribeEvent
  def playerLoggedIn(e: PlayerLoggedInEvent) {
    if (SideTracker.isServer) e.player match {
      case player: EntityPlayerMP =>
        if (!LuaStateFactory.isAvailable) {
          player.addChatMessage(Localization.Chat.WarningLuaFallback)
        }
        if (Mods.ProjectRedTransmission.isAvailable && !mods.ProjectRed.isAPIAvailable) {
          player.addChatMessage(Localization.Chat.WarningProjectRed)
        }
        if (!Settings.get.pureIgnorePower && Settings.get.ignorePower) {
          player.addChatMessage(Localization.Chat.WarningPower)
        }
        OpenComputers.tampered match {
          case Some(event) => player.addChatMessage(Localization.Chat.WarningFingerprint(event))
          case _ =>
        }
        ServerPacketSender.sendPetVisibility(None, Some(player))
        // Do update check in local games and for OPs.
        if (!Mods.VersionChecker.isAvailable && (!MinecraftServer.getServer.isDedicatedServer || MinecraftServer.getServer.getConfigurationManager.func_152596_g(player.getGameProfile))) {
          Future {
            UpdateCheck.info onSuccess {
              case Some(release) => player.addChatMessage(Localization.Chat.InfoNewVersion(release.tag_name))
            }
          }
        }
      case _ =>
    }
  }

  @SubscribeEvent
  def clientLoggedIn(e: ClientConnectedToServerEvent) {
    PetRenderer.hidden.clear()
    if (Settings.get.hideOwnPet) {
      PetRenderer.hidden += Minecraft.getMinecraft.thePlayer.getCommandSenderName
    }
    ClientPacketSender.sendPetVisibility()
  }

  lazy val navigationUpgrade = api.Items.get("navigationUpgrade")

  @SubscribeEvent
  def onCrafting(e: ItemCraftedEvent) = {
    if (api.Items.get(e.crafting) == navigationUpgrade) {
      Option(api.Driver.driverFor(e.crafting)).foreach(driver =>
        for (i <- 0 until e.craftMatrix.getSizeInventory) {
          val stack = e.craftMatrix.getStackInSlot(i)
          if (stack != null && api.Items.get(stack) == navigationUpgrade) {
            // Restore the map currently used in the upgrade.
            val nbt = driver.dataTag(stack)
            val map = ItemStack.loadItemStackFromNBT(nbt.getCompoundTag(Settings.namespace + "map"))
            if (!e.player.inventory.addItemStackToInventory(map)) {
              e.player.dropPlayerItemWithRandomChoice(map, false)
            }
          }
        })
    }
  }

  @SubscribeEvent
  def onWorldUnload(e: WorldEvent.Unload) {
    if (!e.world.isRemote) {
      import scala.collection.convert.WrapAsScala._
      e.world.loadedTileEntityList.collect {
        case te: tileentity.traits.TileEntity => te.onChunkUnload()
      }
    }
  }
}
