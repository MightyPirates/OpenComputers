package li.cil.oc.common

import java.util.Calendar

import cpw.mods.fml.common.Optional
import cpw.mods.fml.common.eventhandler.SubscribeEvent
import cpw.mods.fml.common.gameevent.PlayerEvent._
import cpw.mods.fml.common.gameevent.TickEvent
import cpw.mods.fml.common.gameevent.TickEvent.ClientTickEvent
import cpw.mods.fml.common.gameevent.TickEvent.ServerTickEvent
import cpw.mods.fml.common.network.FMLNetworkEvent.ClientConnectedToServerEvent
import li.cil.oc._
import li.cil.oc.api.Network
import li.cil.oc.api.detail.ItemInfo
import li.cil.oc.api.internal.Rack
import li.cil.oc.api.internal.Server
import li.cil.oc.api.machine.MachineHost
import li.cil.oc.client.renderer.PetRenderer
import li.cil.oc.common.asm.ClassTransformer
import li.cil.oc.common.component.TerminalServer
import li.cil.oc.common.item.data.MicrocontrollerData
import li.cil.oc.common.item.data.RobotData
import li.cil.oc.common.item.data.TabletData
import li.cil.oc.common.recipe.Recipes
import li.cil.oc.common.tileentity.Robot
import li.cil.oc.common.tileentity.traits.power
import li.cil.oc.integration.Mods
import li.cil.oc.integration.util
import li.cil.oc.integration.util.Wrench
import li.cil.oc.server.component.Keyboard
import li.cil.oc.server.machine.Callbacks
import li.cil.oc.server.machine.Machine
import li.cil.oc.server.machine.luac.LuaStateFactory
import li.cil.oc.server.{PacketSender => ServerPacketSender}
import li.cil.oc.util.ExtendedWorld._
import li.cil.oc.util._
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.item.ItemStack
import net.minecraft.server.MinecraftServer
import net.minecraft.tileentity.TileEntity
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.common.util.FakePlayer
import net.minecraftforge.common.util.ForgeDirection
import net.minecraftforge.event.entity.EntityJoinWorldEvent
import net.minecraftforge.event.world.BlockEvent
import net.minecraftforge.event.world.ChunkEvent
import net.minecraftforge.event.world.WorldEvent

import scala.collection.convert.WrapAsScala._
import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object EventHandler {
  private val pendingServer = mutable.Buffer.empty[() => Unit]

  private val pendingClient = mutable.Buffer.empty[() => Unit]

  private val runningRobots = mutable.Set.empty[Robot]

  private val keyboards = java.util.Collections.newSetFromMap[Keyboard](new java.util.WeakHashMap[Keyboard, java.lang.Boolean])

  private val machines = mutable.Set.empty[Machine]

  def onRobotStart(robot: Robot): Unit = runningRobots += robot

  def onRobotStopped(robot: Robot): Unit = runningRobots -= robot

  def addKeyboard(keyboard: Keyboard): Unit = keyboards += keyboard

  def scheduleClose(machine: Machine): Unit = machines += machine

  def unscheduleClose(machine: Machine): Unit = machines -= machine

  def scheduleServer(tileEntity: TileEntity) {
    if (SideTracker.isServer) pendingServer.synchronized {
      pendingServer += (() => Network.joinOrCreateNetwork(tileEntity))
    }
  }

  def scheduleServer(f: () => Unit) {
    pendingServer.synchronized {
      pendingServer += f
    }
  }

  def scheduleClient(f: () => Unit) {
    pendingClient.synchronized {
      pendingClient += f
    }
  }

  @Optional.Method(modid = Mods.IDs.ForgeMultipart)
  def scheduleFMP(tileEntity: () => TileEntity) {
    if (SideTracker.isServer) pendingServer.synchronized {
      pendingServer += (() => Network.joinOrCreateNetwork(tileEntity()))
    }
  }

  @Optional.Method(modid = Mods.IDs.AppliedEnergistics2)
  def scheduleAE2Add(tileEntity: power.AppliedEnergistics2) {
    if (SideTracker.isServer) pendingServer.synchronized {
      pendingServer += (() => if (!tileEntity.isInvalid) {
        tileEntity.getGridNode(ForgeDirection.UNKNOWN).updateState()
      })
    }
  }

  @Optional.Method(modid = Mods.IDs.IndustrialCraft2)
  def scheduleIC2Add(tileEntity: power.IndustrialCraft2Experimental) {
    if (SideTracker.isServer) pendingServer.synchronized {
      pendingServer += (() => if (!tileEntity.addedToIC2PowerGrid && !tileEntity.isInvalid) {
        MinecraftForge.EVENT_BUS.post(new ic2.api.energy.event.EnergyTileLoadEvent(tileEntity.asInstanceOf[ic2.api.energy.tile.IEnergyTile]))
        tileEntity.addedToIC2PowerGrid = true
      })
    }
  }

  @Optional.Method(modid = Mods.IDs.IndustrialCraft2Classic)
  def scheduleIC2Add(tileEntity: power.IndustrialCraft2Classic) {
    if (SideTracker.isServer) pendingServer.synchronized {
      pendingServer += (() => if (!tileEntity.addedToIC2PowerGrid && !tileEntity.isInvalid) {
        MinecraftForge.EVENT_BUS.post(new ic2classic.api.energy.event.EnergyTileLoadEvent(tileEntity.asInstanceOf[ic2classic.api.energy.tile.IEnergyTile]))
        tileEntity.addedToIC2PowerGrid = true
      })
    }
  }

  def scheduleWirelessRedstone(rs: server.component.RedstoneWireless) {
    if (SideTracker.isServer) pendingServer.synchronized {
      pendingServer += (() => if (rs.node.network != null) {
        util.WirelessRedstone.addReceiver(rs)
        util.WirelessRedstone.updateOutput(rs)
      })
    }
  }

  @SubscribeEvent
  def onServerTick(e: ServerTickEvent) = if (e.phase == TickEvent.Phase.START) {
    pendingServer.synchronized {
      val adds = pendingServer.toArray
      pendingServer.clear()
      adds
    } foreach (callback => {
      try callback() catch {
        case t: Throwable => OpenComputers.log.warn("Error in scheduled tick action.", t)
      }
    })

    val invalid = mutable.ArrayBuffer.empty[Robot]
    runningRobots.foreach(robot => {
      if (robot.isInvalid) invalid += robot
      else if (robot.world != null) robot.machine.update()
    })
    runningRobots --= invalid
  }
  else if (e.phase == TickEvent.Phase.END) {
    // Clean up machines *after* a tick, to allow stuff to be saved, first.
    val closed = mutable.ArrayBuffer.empty[Machine]
    machines.foreach(machine => if (machine.tryClose()) {
      closed += machine
      if (machine.host.world == null || !machine.host.world.blockExists(BlockPosition(machine.host))) {
        if (machine.node != null) machine.node.remove()
      }
    })
    machines --= closed
  }

  @SubscribeEvent
  def onClientTick(e: ClientTickEvent) = if (e.phase == TickEvent.Phase.START) {
    pendingClient.synchronized {
      val adds = pendingClient.toArray
      pendingClient.clear()
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
      case _: FakePlayer => // Nope
      case player: EntityPlayerMP =>
        if (!LuaStateFactory.isAvailable) {
          player.addChatMessage(Localization.Chat.WarningLuaFallback)
        }
        if (!Settings.get.pureIgnorePower && Settings.get.ignorePower) {
          player.addChatMessage(Localization.Chat.WarningPower)
        }
        if (Recipes.hadErrors) {
          player.addChatMessage(Localization.Chat.WarningRecipes)
        }
        if (ClassTransformer.hadErrors) {
          player.addChatMessage(Localization.Chat.WarningClassTransformer)
        }
        if (ClassTransformer.hadSimpleComponentErrors) {
          player.addChatMessage(Localization.Chat.WarningSimpleComponent)
        }
        ServerPacketSender.sendPetVisibility(None, Some(player))
        ServerPacketSender.sendLootDisks(player)
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
    PetRenderer.isInitialized = false
    PetRenderer.hidden.clear()
    Loot.disksForClient.clear()
    Loot.disksForCyclingClient.clear()

    client.Sound.startLoop(null, "computer_running", 0f, 0)
    scheduleServer(() => client.Sound.stopLoop(null))
  }

  @SubscribeEvent
  def onBlockBreak(e: BlockEvent.BreakEvent): Unit = {
    e.world.getTileEntity(e.x, e.y, e.z) match {
      case c: tileentity.Case =>
        if (c.isCreative && (!e.getPlayer.capabilities.isCreativeMode || !c.canInteract(e.getPlayer.getCommandSenderName))) {
          e.setCanceled(true)
        }
      case r: tileentity.RobotProxy =>
        val robot = r.robot
        if (robot.isCreative && (!e.getPlayer.capabilities.isCreativeMode || !robot.canInteract(e.getPlayer.getCommandSenderName))) {
          e.setCanceled(true)
        }
      case _ =>
    }
  }

  @SubscribeEvent
  def onPlayerRespawn(e: PlayerRespawnEvent) {
    keyboards.foreach(_.releasePressedKeys(e.player))
  }

  @SubscribeEvent
  def onPlayerChangedDimension(e: PlayerChangedDimensionEvent) {
    keyboards.foreach(_.releasePressedKeys(e.player))
  }

  @SubscribeEvent
  def onPlayerLogout(e: PlayerLoggedOutEvent) {
    keyboards.foreach(_.releasePressedKeys(e.player))
  }

  @SubscribeEvent
  def onEntityJoinWorld(e: EntityJoinWorldEvent): Unit = {
    if (Settings.get.giveManualToNewPlayers && !e.world.isRemote) e.entity match {
      case player: EntityPlayer if !player.isInstanceOf[FakePlayer] =>
        val persistedData = PlayerUtils.persistedData(player)
        if (!persistedData.getBoolean(Settings.namespace + "receivedManual")) {
          persistedData.setBoolean(Settings.namespace + "receivedManual", true)
          player.inventory.addItemStackToInventory(api.Items.get(Constants.ItemName.Manual).createItemStack(1))
        }
      case _ =>
    }
  }

  lazy val drone = api.Items.get(Constants.ItemName.Drone)
  lazy val eeprom = api.Items.get(Constants.ItemName.EEPROM)
  lazy val floppy = api.Items.get(Constants.ItemName.Floppy)
  lazy val mcu = api.Items.get(Constants.BlockName.Microcontroller)
  lazy val navigationUpgrade = api.Items.get(Constants.ItemName.NavigationUpgrade)
  lazy val robot = api.Items.get(Constants.BlockName.Robot)
  lazy val tablet = api.Items.get(Constants.ItemName.Tablet)

  @SubscribeEvent
  def onCrafting(e: ItemCraftedEvent) = {
    var didRecraft = false

    didRecraft = recraft(e, navigationUpgrade, stack => {
      // Restore the map currently used in the upgrade.
      Option(api.Driver.driverFor(e.crafting)) match {
        case Some(driver) => Option(ItemStack.loadItemStackFromNBT(driver.dataTag(stack).getCompoundTag(Settings.namespace + "map")))
        case _ => None
      }
    }) || didRecraft

    didRecraft = recraft(e, mcu, stack => {
      // Restore EEPROM currently used in microcontroller.
      new MicrocontrollerData(stack).components.find(api.Items.get(_) == eeprom)
    }) || didRecraft

    didRecraft = recraft(e, drone, stack => {
      // Restore EEPROM currently used in drone.
      new MicrocontrollerData(stack).components.find(api.Items.get(_) == eeprom)
    }) || didRecraft

    didRecraft = recraft(e, robot, stack => {
      // Restore EEPROM currently used in robot.
      new RobotData(stack).components.find(api.Items.get(_) == eeprom)
    }) || didRecraft

    didRecraft = recraft(e, tablet, stack => {
      // Restore EEPROM currently used in tablet.
      new TabletData(stack).items.collect { case Some(item) => item }.find(api.Items.get(_) == eeprom)
    }) || didRecraft

    didRecraft = {
      if (Loot.isLootDisk(e.crafting)) {
        val stacks = (0 until e.craftMatrix.getSizeInventory).flatMap(i => Option(e.craftMatrix.getStackInSlot(i))).toArray
        if (stacks.length == 2) stacks.find(Wrench.isWrench) match {
          case Some(stack) =>
            stack.stackSize += 1
            true
          case _ => didRecraft
        }
        else didRecraft
      }
      else didRecraft
    }

    // Presents?
    e.player match {
      case _: FakePlayer => // No presents for you, automaton. Such discrimination. Much bad conscience.
      case player: EntityPlayerMP if player.getEntityWorld != null && !player.getEntityWorld.isRemote =>
        // Presents!? If we didn't recraft, it's an OC item, and the time is right...
        if (Settings.get.presentChance > 0 && !didRecraft && api.Items.get(e.crafting) != null &&
          e.player.getRNG.nextFloat() < Settings.get.presentChance && timeForPresents) {
          // Presents!
          val present = api.Items.get(Constants.ItemName.Present).createItemStack(1)
          e.player.worldObj.playSoundAtEntity(e.player, "note.pling", 0.2f, 1f)
          InventoryUtils.addToPlayerInventory(present, e.player)
        }
      case _ => // Nope.
    }

    Achievement.onCraft(e.crafting, e.player)
  }

  @SubscribeEvent
  def onPickup(e: ItemPickupEvent): Unit = {
    val entity = e.pickedUp
    Option(entity).flatMap(e => Option(e.getEntityItem)) match {
      case Some(stack) =>
        Achievement.onAssemble(stack, e.player)
        Achievement.onCraft(stack, e.player)
      case _ => // Huh.
    }
  }

  private def timeForPresents = {
    val now = Calendar.getInstance()
    val month = now.get(Calendar.MONTH)
    val dayOfMonth = now.get(Calendar.DAY_OF_MONTH)
    // On the 12th day of Christmas, my robot brought to me~
    (month == Calendar.DECEMBER && dayOfMonth > 24) || (month == Calendar.JANUARY && dayOfMonth < 7) ||
      (month == Calendar.FEBRUARY && dayOfMonth == 14) ||
      (month == Calendar.APRIL && dayOfMonth == 22) ||
      (month == Calendar.MAY && dayOfMonth == 1) ||
      (month == Calendar.OCTOBER && dayOfMonth == 3) ||
      (month == Calendar.DECEMBER && dayOfMonth == 14)
  }

  def isItTime = {
    val now = Calendar.getInstance()
    val month = now.get(Calendar.MONTH)
    val dayOfMonth = now.get(Calendar.DAY_OF_MONTH)
    month == Calendar.APRIL && dayOfMonth == 1
  }

  private def recraft(e: ItemCraftedEvent, item: ItemInfo, callback: ItemStack => Option[ItemStack]): Boolean = {
    if (api.Items.get(e.crafting) == item) {
      for (slot <- 0 until e.craftMatrix.getSizeInventory) {
        val stack = e.craftMatrix.getStackInSlot(slot)
        if (api.Items.get(stack) == item) {
          callback(stack).foreach(extra =>
            InventoryUtils.addToPlayerInventory(extra, e.player))
        }
      }
      true
    }
    else false
  }

  // This is called from the ServerThread *and* the ClientShutdownThread, which
  // can potentially happen at the same time... for whatever reason. So let's
  // synchronize what we're doing here to avoid race conditions (e.g. when
  // disposing networks, where this actually triggered an assert).
  @SubscribeEvent
  def onWorldUnload(e: WorldEvent.Unload): Unit = this.synchronized {
    if (!e.world.isRemote) {
      e.world.loadedTileEntityList.collect {
        case te: tileentity.traits.TileEntity => te.dispose()
      }
      e.world.loadedEntityList.collect {
        case host: MachineHost => host.machine.stop()
      }

      Callbacks.clear()
    }
    else {
      TerminalServer.loaded.clear()
    }
  }

  @SubscribeEvent
  def onChunkUnload(e: ChunkEvent.Unload): Unit = {
    if (!e.world.isRemote) {
      e.getChunk.entityLists.foreach(_.collect {
        case host: MachineHost => host.machine match {
          case machine: Machine => scheduleClose(machine)
          case _ => // Dafuq?
        }
        case rack: Rack =>
          (0 until rack.getSizeInventory).
            map(rack.getMountable).
            collect { case server: Server if server.machine != null => server.machine.stop() }
      })
    }
  }
}
