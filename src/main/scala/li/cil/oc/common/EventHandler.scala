package li.cil.oc.common

import java.util.Calendar

import appeng.api.networking.IGridBlock
import appeng.api.util.AEPartLocation
import li.cil.oc._
import li.cil.oc.api.Network
import li.cil.oc.api.detail.ItemInfo
import li.cil.oc.api.internal.Colored
import li.cil.oc.api.internal.Rack
import li.cil.oc.api.internal.Server
import li.cil.oc.api.machine.MachineHost
import li.cil.oc.api.network.Environment
import li.cil.oc.api.network.SidedComponent
import li.cil.oc.api.network.SidedEnvironment
import li.cil.oc.client.renderer.PetRenderer
import li.cil.oc.common.capabilities.CapabilityColored
import li.cil.oc.common.capabilities.CapabilityEnvironment
import li.cil.oc.common.capabilities.CapabilitySidedComponent
import li.cil.oc.common.capabilities.CapabilitySidedEnvironment
import li.cil.oc.common.component.TerminalServer
import li.cil.oc.common.item.data.MicrocontrollerData
import li.cil.oc.common.item.data.RobotData
import li.cil.oc.common.item.data.TabletData
import li.cil.oc.common.item.traits
import li.cil.oc.common.tileentity.Robot
import li.cil.oc.common.tileentity.traits.power
import li.cil.oc.integration.Mods
import li.cil.oc.integration.util
import li.cil.oc.server.component.Keyboard
import li.cil.oc.server.machine.Callbacks
import li.cil.oc.server.machine.Machine
import li.cil.oc.server.machine.luac.LuaStateFactory
import li.cil.oc.server.{PacketSender => ServerPacketSender}
import li.cil.oc.util.ExtendedWorld._
import li.cil.oc.util.StackOption._
import li.cil.oc.util._
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.ServerPlayerEntity
import net.minecraft.util.SoundEvents
import net.minecraft.item.ItemStack
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.SoundCategory
import net.minecraft.util.Util
import net.minecraft.world.chunk.Chunk
import net.minecraft.world.server.ChunkHolder
import net.minecraft.world.server.ChunkManager
import net.minecraft.world.server.ServerWorld
import net.minecraftforge.client.event.ClientPlayerNetworkEvent
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.common.util.FakePlayer
import net.minecraftforge.event.AttachCapabilitiesEvent
import net.minecraftforge.event.TickEvent
import net.minecraftforge.event.TickEvent.ClientTickEvent
import net.minecraftforge.event.TickEvent.ServerTickEvent
import net.minecraftforge.event.entity.EntityJoinWorldEvent
import net.minecraftforge.event.entity.player.PlayerEvent._
import net.minecraftforge.event.world.BlockEvent
import net.minecraftforge.event.world.ChunkEvent
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.ObfuscationReflectionHelper
import net.minecraftforge.fml.server.ServerLifecycleHooks

import scala.collection.convert.ImplicitConversionsToScala._
import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object EventHandler {
  private var serverTicks = 0L
  private val pendingServerTimed = mutable.PriorityQueue.empty[(Long, () => Unit)](Ordering.by(x => -x._1))

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

  def scheduleServer(f: () => Unit, delay: Int): Unit = {
    pendingServerTimed.synchronized {
      pendingServerTimed += (serverTicks + (delay max 0)) -> f
    }
  }

  def scheduleClient(f: () => Unit) {
    pendingClient.synchronized {
      pendingClient += f
    }
  }

  object AE2 {
    def scheduleAE2Add(tileEntity: power.AppliedEnergistics2): Unit = {
      if (SideTracker.isServer) pendingServer.synchronized {
        tileEntity match {
          case tile: IGridBlock =>
            pendingServer += (() => if (!tileEntity.isRemoved) {
              tileEntity.getGridNode(AEPartLocation.INTERNAL).updateState()
            })
          case _ =>
        }
      }
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
  def onAttachCapabilitiesItemStack(event: AttachCapabilitiesEvent[ItemStack]): Unit = {
    if (!event.getCapabilities.containsKey(traits.Chargeable.KEY)) {
      event.getObject match {
        case stack: ItemStack => stack.getItem match {
          case chargeable: traits.Chargeable => {
            val provider = new traits.Chargeable.Provider(stack, chargeable)
            event.addCapability(traits.Chargeable.KEY, provider)
            event.addListener(new Runnable {
              override def run = provider.invalidate
            })
          }
          case _ =>
        }
        case _ =>
      }
    }
  }

  @SubscribeEvent
  def onAttachCapabilities(event: AttachCapabilitiesEvent[TileEntity]): Unit = {
    event.getObject match {
      case tileEntity: TileEntity with Environment => {
        val provider = new CapabilityEnvironment.Provider(tileEntity)
        event.addCapability(CapabilityEnvironment.ProviderEnvironment, provider)
        event.addListener(new Runnable {
          override def run = provider.invalidate
        })
      }
      case _ =>
    }

    event.getObject match {
      case tileEntity: TileEntity with Environment with SidedComponent => {
        val provider = new CapabilitySidedComponent.Provider(tileEntity)
        event.addCapability(CapabilitySidedComponent.SidedComponent, provider)
        event.addListener(new Runnable {
          override def run = provider.invalidate
        })
      }
      case tileEntity: TileEntity with SidedEnvironment => {
        val provider = new CapabilitySidedEnvironment.Provider(tileEntity)
        event.addCapability(CapabilitySidedEnvironment.ProviderSidedEnvironment, provider)
        event.addListener(new Runnable {
          override def run = provider.invalidate
        })
      }
      case _ =>
    }

    event.getObject match {
      case tileEntity: TileEntity with Colored => {
        val provider = new CapabilityColored.Provider(tileEntity)
        event.addCapability(CapabilityColored.ProviderColored, provider)
        event.addListener(new Runnable {
          override def run = provider.invalidate
        })
      }
      case _ =>
    }
  }

  @SubscribeEvent
  def onServerTick(e: ServerTickEvent): Any = if (e.phase == TickEvent.Phase.START) {
    pendingServer.synchronized {
      val adds = pendingServer.toArray
      pendingServer.clear()
      adds
    } foreach (callback => {
      try callback() catch {
        case t: Throwable => OpenComputers.log.warn("Error in scheduled tick action.", t)
      }
    })

    serverTicks += 1
    while (pendingServerTimed.nonEmpty && pendingServerTimed.head._1 < serverTicks) {
      val (_, callback) = pendingServerTimed.dequeue()
      try callback() catch {
        case t: Throwable => OpenComputers.log.warn("Error in scheduled tick action.", t)
      }
    }

    val invalid = mutable.ArrayBuffer.empty[Robot]
    runningRobots.foreach(robot => {
      if (robot.isRemoved) invalid += robot
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
    if (SideTracker.isServer) e.getPlayer match {
      case _: FakePlayer => // Nope
      case player: ServerPlayerEntity =>
        if (!LuaStateFactory.isAvailable && !LuaStateFactory.luajRequested) {
          player.sendMessage(Localization.Chat.WarningLuaFallback, Util.NIL_UUID)
        }
        // Gaaah, MC 1.8 y u do this to me? Sending the packets here directly can lead to them
        // arriving on the client before it has a world and player instance, which causes all
        // sorts of trouble. It worked perfectly fine in MC 1.7.10... oSWDEG'PIl;dg'poinEG\a'pi=
        EventHandler.scheduleServer(() => {
          ServerPacketSender.sendPetVisibility(None, Some(player))
          ServerPacketSender.sendLootDisks(player)
        })
        // Do update check in local games and for OPs.
        val server = ServerLifecycleHooks.getCurrentServer
        if (server.getPlayerList.isOp(player.getGameProfile)) {
          Future {
            UpdateCheck.info foreach {
              case Some(release) => player.sendMessage(Localization.Chat.InfoNewVersion(release.tag_name), Util.NIL_UUID)
              case _ =>
            }
          }
        }
      case _ =>
    }
  }

  @SubscribeEvent
  def clientLoggedIn(e: ClientPlayerNetworkEvent.LoggedInEvent) {
    PetRenderer.isInitialized = false
    PetRenderer.hidden.clear()
    Loot.disksForClient.clear()
    Loot.disksForCyclingClient.clear()

    client.Sound.startLoop(null, "computer_running", 0f, 0)
    scheduleServer(() => client.Sound.stopLoop(null))
  }

  @SubscribeEvent
  def onBlockBreak(e: BlockEvent.BreakEvent): Unit = {
    e.getWorld.getBlockEntity(e.getPos) match {
      case c: tileentity.Case =>
        if (c.isCreative && (!e.getPlayer.isCreative || !c.canInteract(e.getPlayer.getName.getString))) {
          e.setCanceled(true)
        }
      case r: tileentity.RobotProxy =>
        val robot = r.robot
        if (robot.isCreative && (!e.getPlayer.isCreative || !robot.canInteract(e.getPlayer.getName.getString))) {
          e.setCanceled(true)
        }
      case _ =>
    }
  }

  @SubscribeEvent
  def onPlayerRespawn(e: PlayerRespawnEvent) {
    keyboards.foreach(_.releasePressedKeys(e.getPlayer))
  }

  @SubscribeEvent
  def onPlayerChangedDimension(e: PlayerChangedDimensionEvent) {
    keyboards.foreach(_.releasePressedKeys(e.getPlayer))
  }

  @SubscribeEvent
  def onPlayerLogout(e: PlayerLoggedOutEvent) {
    keyboards.foreach(_.releasePressedKeys(e.getPlayer))
  }

  @SubscribeEvent
  def onEntityJoinWorld(e: EntityJoinWorldEvent): Unit = {
    if (Settings.get.giveManualToNewPlayers && !e.getWorld.isClientSide) e.getEntity match {
      case player: PlayerEntity if !player.isInstanceOf[FakePlayer] =>
        val persistedData = PlayerUtils.persistedData(player)
        if (!persistedData.getBoolean(Settings.namespace + "receivedManual")) {
          persistedData.putBoolean(Settings.namespace + "receivedManual", true)
          player.inventory.add(api.Items.get(Constants.ItemName.Manual).createItemStack(1))
        }
      case _ =>
    }
  }

  lazy val drone = api.Items.get(Constants.ItemName.Drone)
  lazy val eeprom = api.Items.get(Constants.ItemName.EEPROM)
  lazy val mcu = api.Items.get(Constants.BlockName.Microcontroller)
  lazy val navigationUpgrade = api.Items.get(Constants.ItemName.NavigationUpgrade)
  lazy val robot = api.Items.get(Constants.BlockName.Robot)
  lazy val tablet = api.Items.get(Constants.ItemName.Tablet)

  @SubscribeEvent
  def onCrafting(e: ItemCraftedEvent) = {
    var didRecraft = false

    didRecraft = recraft(e, navigationUpgrade, stack => {
      // Restore the map currently used in the upgrade.
      Option(api.Driver.driverFor(e.getCrafting)) match {
        case Some(driver) => StackOption(ItemStack.of(driver.dataTag(stack).getCompound(Settings.namespace + "map")))
        case _ => EmptyStack
      }
    }) || didRecraft

    didRecraft = recraft(e, mcu, stack => {
      // Restore EEPROM currently used in microcontroller.
      new MicrocontrollerData(stack).components.find(api.Items.get(_) == eeprom).asStackOption
    }) || didRecraft

    didRecraft = recraft(e, drone, stack => {
      // Restore EEPROM currently used in drone.
      new MicrocontrollerData(stack).components.find(api.Items.get(_) == eeprom).asStackOption
    }) || didRecraft

    didRecraft = recraft(e, robot, stack => {
      // Restore EEPROM currently used in robot.
      new RobotData(stack).components.find(api.Items.get(_) == eeprom).asStackOption
    }) || didRecraft

    didRecraft = recraft(e, tablet, stack => {
      // Restore EEPROM currently used in tablet.
      new TabletData(stack).items.collect { case item if !item.isEmpty => item }.find(api.Items.get(_) == eeprom).asStackOption
    }) || didRecraft

    // Presents?
    e.getPlayer match {
      case _: FakePlayer => // No presents for you, automaton. Such discrimination. Much bad conscience.
      case player: ServerPlayerEntity if player.level != null && !player.level.isClientSide =>
        // Presents!? If we didn't recraft, it's an OC item, and the time is right...
        if (Settings.get.presentChance > 0 && !didRecraft && api.Items.get(e.getCrafting) != null &&
          e.getPlayer.getRandom.nextFloat() < Settings.get.presentChance && timeForPresents) {
          // Presents!
          val present = api.Items.get(Constants.ItemName.Present).createItemStack(1)
          e.getPlayer.level.playSound(e.getPlayer, e.getPlayer.getX, e.getPlayer.getY, e.getPlayer.getZ, SoundEvents.NOTE_BLOCK_PLING, SoundCategory.MASTER, 0.2f, 1f)
          InventoryUtils.addToPlayerInventory(present, e.getPlayer)
        }
      case _ => // Nope.
    }

    Achievement.onCraft(e.getCrafting, e.getPlayer)
  }

  @SubscribeEvent
  def onPickup(e: ItemPickupEvent): Unit = {
    val entity = e.getOriginalEntity
    Option(entity).flatMap(e => Option(e.getItem)) match {
      case Some(stack) =>
        Achievement.onAssemble(stack, e.getPlayer)
        Achievement.onCraft(stack, e.getPlayer)
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

  private def recraft(e: ItemCraftedEvent, item: ItemInfo, callback: ItemStack => StackOption): Boolean = {
    if (api.Items.get(e.getCrafting) == item) {
      for (slot <- 0 until e.getInventory.getContainerSize) {
        val stack = e.getInventory.getItem(slot)
        if (api.Items.get(stack) == item) {
          callback(stack).foreach(extra =>
            InventoryUtils.addToPlayerInventory(extra, e.getPlayer))
        }
      }
      true
    }
    else false
  }

  private val getChunks = ObfuscationReflectionHelper.findMethod(classOf[ChunkManager], "func_223491_f")

  private def getChunks(world: ServerWorld): Iterable[ChunkHolder] = try {
    getChunks.invoke(world.getChunkSource.chunkMap).asInstanceOf[java.lang.Iterable[ChunkHolder]]
  }
  catch {
    case e: Throwable =>
      throw new Error("Could not access server chunk list", e)
  }

  // This is called from the ServerThread *and* the ClientShutdownThread, which
  // can potentially happen at the same time... for whatever reason. So let's
  // synchronize what we're doing here to avoid race conditions (e.g. when
  // disposing networks, where this actually triggered an assert).
  @SubscribeEvent
  def onWorldUnload(e: WorldEvent.Unload): Unit = this.synchronized {
    if (!e.getWorld.isClientSide) {
      val world = e.getWorld.asInstanceOf[ServerWorld]
      world.blockEntityList.collect {
        case te: tileentity.traits.TileEntity => te.dispose()
      }

      getChunks(world).foreach(holder => {
        val chunk = holder.getTickingChunk
        if (chunk != null) chunk.getEntitySections.foreach {
          _.iterator.collect {
            case host: MachineHost => host.machine.stop()
          }
        }
      })

      Callbacks.clear()
    }
    else {
      TerminalServer.loaded.clear()
    }
  }

  @SubscribeEvent
  def onChunkUnloaded(e: ChunkEvent.Unload): Unit = {
    if (!e.getWorld.isClientSide) e.getChunk match {
      case chunk: Chunk => {
        chunk.getEntitySections.foreach(_.collect {
          case host: MachineHost => host.machine match {
            case machine: Machine => scheduleClose(machine)
            case _ => // Dafuq?
          }
          case rack: Rack =>
            (0 until rack.getContainerSize).
              map(rack.getMountable).
              collect { case server: Server if server.machine != null => server.machine.stop() }
        })
      }
      case _ =>
    }
  }
}
