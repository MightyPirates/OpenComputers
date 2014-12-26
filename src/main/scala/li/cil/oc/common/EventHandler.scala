package li.cil.oc.common

import java.util.Calendar

import net.minecraftforge.fml.common.Optional
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.PlayerEvent._
import net.minecraftforge.fml.common.gameevent.TickEvent
import net.minecraftforge.fml.common.gameevent.TickEvent.ServerTickEvent
import net.minecraftforge.fml.common.network.FMLNetworkEvent.ClientConnectedToServerEvent
import li.cil.oc._
import li.cil.oc.api.Network
import li.cil.oc.api.detail.ItemInfo
import li.cil.oc.client.renderer.PetRenderer
import li.cil.oc.client.{PacketSender => ClientPacketSender}
import li.cil.oc.common.tileentity.traits.power
import li.cil.oc.integration.Mods
import li.cil.oc.integration.util
import li.cil.oc.server.{PacketSender => ServerPacketSender}
import li.cil.oc.util._
import net.minecraft.client.Minecraft
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.item.ItemStack
import net.minecraft.server.MinecraftServer
import net.minecraft.tileentity.TileEntity
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.common.util.FakePlayer
import net.minecraft.util.EnumFacing
import net.minecraftforge.event.world.WorldEvent

import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object EventHandler {
  private val pending = mutable.Buffer.empty[() => Unit]

  def schedule(tileEntity: TileEntity) {
    if (SideTracker.isServer) pending.synchronized {
      pending += (() => Network.joinOrCreateNetwork(tileEntity))
    }
  }

  def schedule(f: () => Unit) {
    pending.synchronized {
      pending += f
    }
  }

  def scheduleWirelessRedstone(rs: server.component.RedstoneWireless) {
    if (SideTracker.isServer) pending.synchronized {
      pending += (() => if (!rs.owner.isInvalid) {
        util.WirelessRedstone.addReceiver(rs)
        util.WirelessRedstone.updateOutput(rs)
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
        if (!Settings.get.pureIgnorePower && Settings.get.ignorePower) {
          player.addChatMessage(Localization.Chat.WarningPower)
        }
        ServerPacketSender.sendPetVisibility(None, Some(player))
        // Do update check in local games and for OPs.
        if (!Mods.VersionChecker.isAvailable && (!MinecraftServer.getServer.isDedicatedServer || MinecraftServer.getServer.getConfigurationManager.canSendCommands(player.getGameProfile))) {
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
      PetRenderer.hidden += Minecraft.getMinecraft.thePlayer.getName
    }
    ClientPacketSender.sendPetVisibility()
  }

  lazy val drone = api.Items.get("drone")
  lazy val eeprom = api.Items.get("eeprom")
  lazy val mcu = api.Items.get("microcontroller")
  lazy val navigationUpgrade = api.Items.get("navigationUpgrade")

  @SubscribeEvent
  def onCrafting(e: ItemCraftedEvent) = {
    var didRecraft = false

    didRecraft = recraft(e, navigationUpgrade, stack => {
      // Restore the map currently used in the upgrade.
      Option(api.Driver.driverFor(e.crafting)) match {
        case Some(driver) => Option(ItemUtils.loadStack(driver.dataTag(stack).getCompoundTag(Settings.namespace + "map")))
        case _ => None
      }
    }) || didRecraft

    didRecraft = recraft(e, mcu, stack => {
      // Restore EEPROM currently used in microcontroller.
      new ItemUtils.MicrocontrollerData(stack).components.find(api.Items.get(_) == eeprom)
    }) || didRecraft

    didRecraft = recraft(e, drone, stack => {
      // Restore EEPROM currently used in drone.
      new ItemUtils.MicrocontrollerData(stack).components.find(api.Items.get(_) == eeprom)
    }) || didRecraft

    // Presents?
    if (!e.player.worldObj.isRemote) e.player match {
      case _: FakePlayer => // No presents for you, automaton. Such discrimination. Much bad conscience.
      case player: EntityPlayerMP =>
        // Presents!? If we didn't recraft, it's an OC item, and the time is right...
        if (Settings.get.presentChance > 0 && !didRecraft && api.Items.get(e.crafting) != null &&
          e.player.getRNG.nextFloat() < Settings.get.presentChance && timeForPresents) {
          // Presents!
          val present = api.Items.get("present").createItemStack(1)
          e.player.worldObj.playSoundAtEntity(e.player, "note.pling", 0.2f, 1f)
          if (e.player.inventory.addItemStackToInventory(present)) {
            e.player.inventory.markDirty()
            if (e.player.openContainer != null) {
              e.player.openContainer.detectAndSendChanges()
            }
          }
          else {
            e.player.dropPlayerItemWithRandomChoice(present, false)
          }
        }
      case _ => // Nope.
    }
  }

  private def timeForPresents = {
    val now = Calendar.getInstance()
    val month = now.get(Calendar.MONTH)
    val dayOfMonth = now.get(Calendar.DAY_OF_MONTH)
    // On the 12th day of Christmas, my robot brought to me~
    (month == Calendar.DECEMBER && dayOfMonth > 24) || (month == Calendar.JANUARY && dayOfMonth < 7) ||
    // OC's release-birthday!
    (month == Calendar.DECEMBER && dayOfMonth == 14)
  }

  private def recraft(e: ItemCraftedEvent, item: ItemInfo, callback: ItemStack => Option[ItemStack]): Boolean = {
    if (api.Items.get(e.crafting) == item) {
      for (slot <- 0 until e.craftMatrix.getSizeInventory) {
        val stack = e.craftMatrix.getStackInSlot(slot)
        if (api.Items.get(stack) == item) {
          callback(stack).foreach(extra => if (!e.player.inventory.addItemStackToInventory(extra)) {
            e.player.dropPlayerItemWithRandomChoice(extra, false)
          })
        }
      }
      true
    }
    else false
  }

  @SubscribeEvent
  def onWorldUnload(e: WorldEvent.Unload) {
    if (!e.world.isRemote) {
      import scala.collection.convert.WrapAsScala._
      e.world.loadedTileEntityList.collect {
        case te: tileentity.traits.TileEntity => te.dispose()
      }
    }
  }
}
