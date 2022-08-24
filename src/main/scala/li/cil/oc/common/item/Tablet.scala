package li.cil.oc.common.item

import java.lang.Iterable
import java.util
import java.util.UUID
import java.util.concurrent.Callable
import java.util.concurrent.TimeUnit

import com.google.common.cache.{CacheBuilder, RemovalListener, RemovalNotification}
import com.google.common.collect.ImmutableMap
import li.cil.oc.Constants
import li.cil.oc.CreativeTab
import li.cil.oc.Localization
import li.cil.oc.OpenComputers
import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.api.Driver
import li.cil.oc.api.Machine
import li.cil.oc.api.driver.item.Container
import li.cil.oc.api.internal
import li.cil.oc.api.machine.MachineHost
import li.cil.oc.api.network.Connector
import li.cil.oc.api.network.Message
import li.cil.oc.api.network.Node
import li.cil.oc.{client, server}
import li.cil.oc.client.KeyBindings
import li.cil.oc.client.gui
import li.cil.oc.common.Slot
import li.cil.oc.common.Tier
import li.cil.oc.common.container
import li.cil.oc.common.container.ContainerTypes
import li.cil.oc.common.inventory.ComponentInventory
import li.cil.oc.common.item.data.TabletData
import li.cil.oc.integration.opencomputers.DriverScreen
import li.cil.oc.server.{PacketSender, component}
import li.cil.oc.util.Audio
import li.cil.oc.util.BlockPosition
import li.cil.oc.util.ExtendedNBT._
import li.cil.oc.util.Rarity
import li.cil.oc.util.RotationHelper
import li.cil.oc.util.Tooltip
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.model.ModelBakery
import net.minecraft.client.renderer.model.ModelResourceLocation
import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.player.{PlayerEntity, PlayerInventory, ServerPlayerEntity}
import net.minecraft.inventory.container.INamedContainerProvider
import net.minecraft.item // Rarity
import net.minecraft.item.Item
import net.minecraft.item.Item.Properties
import net.minecraft.item.ItemGroup
import net.minecraft.item.ItemStack
import net.minecraft.nbt.CompoundNBT
import net.minecraft.server.integrated.IntegratedServer
import net.minecraft.util.ActionResult
import net.minecraft.util.ActionResultType
import net.minecraft.util.Direction
import net.minecraft.util.Hand
import net.minecraft.util.NonNullList
import net.minecraft.util.ResourceLocation
import net.minecraft.util.Util
import net.minecraft.util.math.BlockPos
import net.minecraft.util.text.ITextComponent
import net.minecraft.util.text.StringTextComponent
import net.minecraft.world.World
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import net.minecraftforge.client.model.ModelLoader
import net.minecraftforge.common.extensions.IForgeItem
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.event.TickEvent.ClientTickEvent
import net.minecraftforge.event.TickEvent.ServerTickEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.server.ServerLifecycleHooks

import scala.collection.JavaConverters.asJavaIterable
import scala.collection.convert.ImplicitConversionsToJava._
import scala.collection.convert.ImplicitConversionsToScala._

class Tablet(props: Properties = new Properties().tab(CreativeTab)) extends Item(props) with IForgeItem with traits.SimpleItem with CustomModel with traits.Chargeable {
  final val TimeToAnalyze = 10

  override def maxStackSize = 1

  // ----------------------------------------------------------------------- //

  override protected def tooltipExtended(stack: ItemStack, tooltip: util.List[ITextComponent]): Unit = {
    if (KeyBindings.showExtendedTooltips) {
      val info = new TabletData(stack)
      // Ignore/hide the screen.
      val components = info.items.drop(1)
      if (components.length > 1) {
        for (curr <- Tooltip.get("server.Components")) {
          tooltip.add(new StringTextComponent(curr).setStyle(Tooltip.DefaultStyle))
        }
        components.collect {
          case component if !component.isEmpty => tooltip.add(new StringTextComponent("- " + component.getHoverName.getString).setStyle(Tooltip.DefaultStyle))
        }
      }
    }
  }

  @Deprecated
  override def getRarity(stack: ItemStack): item.Rarity = {
    val data = new TabletData(stack)
    Rarity.byTier(data.tier)
  }

  override def showDurabilityBar(stack: ItemStack) = true

  override def getDurabilityForDisplay(stack: ItemStack): Double = {
    if (stack.hasTag) {
      val data = Tablet.Client.getWeak(stack) match {
        case Some(wrapper) => wrapper.data
        case _ => new TabletData(stack)
      }
      1 - data.energy / data.maxEnergy
    }
    else 1.0
  }

  // ----------------------------------------------------------------------- //

  @OnlyIn(Dist.CLIENT)
  private def modelLocationFromState(running: Option[Boolean]) = {
    val suffix = running match {
      case Some(state) => if (state) "_on" else "_off"
      case _ => ""
    }
    new ModelResourceLocation(Settings.resourceDomain + ":" + Constants.ItemName.Tablet + suffix, "inventory")
  }

  @OnlyIn(Dist.CLIENT)
  override def getModelLocation(stack: ItemStack): ModelResourceLocation = {
    modelLocationFromState(Tablet.Client.getWeak(stack) match {
      case Some(tablet: TabletWrapper) => Some(tablet.data.isRunning)
      case _ => None
    })
  }

  @OnlyIn(Dist.CLIENT)
  override def registerModelLocations(): Unit = {
    for (state <- Seq(None, Some(true), Some(false))) {
      ModelLoader.addSpecialModel(modelLocationFromState(state))
    }
  }

  def canCharge(stack: ItemStack): Boolean = true

  def charge(stack: ItemStack, amount: Double, simulate: Boolean): Double = {
    if (amount < 0) return amount
    val data = new TabletData(stack)
    traits.Chargeable.applyCharge(amount, data.energy, data.maxEnergy, used => if (!simulate) {
      data.energy += used
      data.saveData(stack)
    })
  }

  // ----------------------------------------------------------------------- //

  // Must be assembled to be usable so we hide it in the item list.
  override def fillItemCategory(tab: ItemGroup, list: NonNullList[ItemStack]) {}

  override def inventoryTick(stack: ItemStack, world: World, entity: Entity, slot: Int, selected: Boolean): Unit =
    entity match {
      case player: PlayerEntity =>
        // Play an audio cue to let players know when they finished analyzing a block.
        if (world.isClientSide && player.getUseItemRemainingTicks == TimeToAnalyze && api.Items.get(player.getUseItem) == api.Items.get(Constants.ItemName.Tablet)) {
          Audio.play(player.getX.toFloat, player.getY.toFloat + 2, player.getZ.toFloat, ".")
        }
        Tablet.get(stack, player).update(world, player, slot, selected)
      case _ =>
    }

  override def onItemUseFirst(stack: ItemStack, player: PlayerEntity, world: World, pos: BlockPos, side: Direction, hitX: Float, hitY: Float, hitZ: Float, hand: Hand): ActionResultType = {
    Tablet.currentlyAnalyzing = Some((BlockPosition(pos, world), side, hitX, hitY, hitZ))
    super.onItemUseFirst(stack, player, world, pos, side, hitX, hitY, hitZ, hand)
  }

  override def onItemUse(stack: ItemStack, player: PlayerEntity, position: BlockPosition, side: Direction, hitX: Float, hitY: Float, hitZ: Float): Boolean = {
    player.startUsingItem(if (player.getItemInHand(Hand.MAIN_HAND) == stack) Hand.MAIN_HAND else Hand.OFF_HAND)
    true
  }

  override def use(stack: ItemStack, world: World, player: PlayerEntity): ActionResult[ItemStack] = {
    player.startUsingItem(if (player.getItemInHand(Hand.MAIN_HAND) == stack) Hand.MAIN_HAND else Hand.OFF_HAND)
    new ActionResult(ActionResultType.sidedSuccess(world.isClientSide), stack)
  }

  override def getUseDuration(stack: ItemStack): Int = 72000

  override def releaseUsing(stack: ItemStack, world: World, entity: LivingEntity, duration: Int): Unit = {
    entity match {
      case player: PlayerEntity =>
        val didAnalyze = getUseDuration(stack) - duration >= TimeToAnalyze
        if (didAnalyze) {
          if (!world.isClientSide) {
            Tablet.currentlyAnalyzing match {
              case Some((position, side, hitX, hitY, hitZ)) => try {
                val computer = Tablet.get(stack, player).machine
                if (computer.isRunning) {
                  val data = new CompoundNBT()
                  computer.node.sendToReachable("tablet.use", data, stack, player, position, side, Float.box(hitX), Float.box(hitY), Float.box(hitZ))
                  if (!data.isEmpty) {
                    computer.signal("tablet_use", data)
                  }
                }
              }
              catch {
                case t: Throwable => OpenComputers.log.warn("Block analysis on tablet right click failed gloriously!", t)
              }
              case _ =>
            }
          }
        }
        else {
          if (player.isCrouching) {
            if (!world.isClientSide) {
              val tablet = Tablet.Server.get(stack, player)
              tablet.machine.stop()
              if (tablet.data.tier > Tier.One) player match {
                case srvPlr: ServerPlayerEntity => ContainerTypes.openTabletGui(srvPlr, Tablet.get(stack, player))
                case _ =>
              }
            }
          }
          else {
            if (!world.isClientSide) {
              val computer = Tablet.get(stack, player).machine
              computer.start()
              computer.lastError match {
                case message if message != null => player.sendMessage(Localization.Analyzer.LastError(message), Util.NIL_UUID)
                case _ =>
              }
            }
            else {
              Tablet.get(stack, player).components.collect {
                case Some(buffer: api.internal.TextBuffer) => buffer
              }.headOption match {
                case Some(buffer: api.internal.TextBuffer) => Minecraft.getInstance.pushGuiLayer(new gui.Screen(buffer, true, () => true, () => buffer.isRenderingEnabled))
                case _ =>
              }
            }
          }
        }
      case _ =>
    }
  }

  override def maxCharge(stack: ItemStack): Double = new TabletData(stack).maxEnergy

  override def getCharge(stack: ItemStack): Double = new TabletData(stack).energy

  override def setCharge(stack: ItemStack, amount: Double): Unit = {
    val data = new TabletData(stack)
    data.energy = (0.0 max amount) min maxCharge(stack)
    data.saveData(stack)
  }
}

class TabletWrapper(var stack: ItemStack, var player: PlayerEntity) extends ComponentInventory with MachineHost with internal.Tablet with INamedContainerProvider {
  // Remember our *original* world, so we know which tablets to clear on dimension
  // changes of players holding tablets - since the player entity instance may be
  // kept the same and components are not required to properly handle world changes.
  val world: World = player.level

  lazy val machine: api.machine.Machine = if (world.isClientSide) null else Machine.create(this)

  val data = new TabletData()

  val tablet: component.Tablet = if (world.isClientSide) null else new component.Tablet(this)

  //// Client side only
  private var isInitialized = !world.isClientSide

  var timesChanged: Int = 0

  var isDirty: Boolean = true
  ////

  // Server side only
  private var lastRunning = false

  var autoSave = true
  ////

  def isCreative: Boolean = data.tier == Tier.Four

  def items: Array[ItemStack] = data.items

  override def facing: Direction = RotationHelper.fromYaw(player.yRot)

  override def toLocal(value: Direction): Direction =
    RotationHelper.toLocal(Direction.NORTH, facing, value)

  override def toGlobal(value: Direction): Direction =
    RotationHelper.toGlobal(Direction.NORTH, facing, value)

  def readFromNBT() {
    if (stack.hasTag) {
      val data = stack.getTag
      loadData(data)
      if (!world.isClientSide) {
        tablet.loadData(data.getCompound(Settings.namespace + "component"))
        machine.loadData(data.getCompound(Settings.namespace + "data"))
      }
    }
  }

  def writeToNBT(clearState: Boolean = true) {
    val data = stack.getOrCreateTag
    if (!world.isClientSide) {
      if (!data.contains(Settings.namespace + "data")) {
        data.put(Settings.namespace + "data", new CompoundNBT())
      }
      data.setNewCompoundTag(Settings.namespace + "component", tablet.saveData(_))
      data.setNewCompoundTag(Settings.namespace + "data", machine.saveData(_))

      if (clearState) {
        // Force tablets into stopped state to avoid errors when trying to
        // load deleted machine states.
        data.getCompound(Settings.namespace + "data").remove("state")
      }
    }
    saveData(data)
  }

  readFromNBT()
  if (!world.isClientSide) {
    api.Network.joinNewNetwork(machine.node)
    val charge = Math.max(0, this.data.energy - tablet.node.globalBuffer)
    tablet.node.changeBuffer(charge)
    writeToNBT()
  }

  // ----------------------------------------------------------------------- //

  override def getDisplayName = getName

  override def createMenu(id: Int, playerInventory: PlayerInventory, player: PlayerEntity) =
    new container.Tablet(ContainerTypes.TABLET, id, playerInventory, stack, this, containerSlotType, containerSlotTier)

  // ----------------------------------------------------------------------- //

  override def onConnect(node: Node) {
    if (node == this.node) {
      connectComponents()
      node.connect(tablet.node)
    }
    else node.host match {
      case buffer: api.internal.TextBuffer =>
        buffer.setMaximumColorDepth(api.internal.TextBuffer.ColorDepth.FourBit)
        buffer.setMaximumResolution(80, 25)
      case _ =>
    }
  }

  override protected def connectItemNode(node: Node) {
    super.connectItemNode(node)
    if (node != null) node.host match {
      case buffer: api.internal.TextBuffer => components collect {
        case Some(keyboard: api.internal.Keyboard) => buffer.node.connect(keyboard.node)
      }
      case keyboard: api.internal.Keyboard => components collect {
        case Some(buffer: api.internal.TextBuffer) => keyboard.node.connect(buffer.node)
      }
      case _ =>
    }
  }

  override def onDisconnect(node: Node) {
    if (node == this.node) {
      disconnectComponents()
      tablet.node.remove()
    }
  }

  override def onMessage(message: Message) {}

  override def host: TabletWrapper = this

  override def getContainerSize: Int = items.length

  override def canPlaceItem(slot: Int, stack: ItemStack): Boolean = slot == getContainerSize - 1 && (Option(Driver.driverFor(stack, getClass)) match {
    case Some(driver) =>
      // Same special cases, similar as in robot, but allow keyboards,
      // because clip-on keyboards kinda seem to make sense, I guess.
      driver != DriverScreen &&
        driver.slot(stack) == containerSlotType &&
        driver.tier(stack) <= containerSlotTier
    case _ => false
  })

  override def stillValid(player: PlayerEntity): Boolean = machine != null && machine.canInteract(player.getName.getString)

  override def setChanged(): Unit = {
    data.saveData(stack)
    player.inventory.setChanged()
  }

  // ----------------------------------------------------------------------- //

  override def xPosition: Double = player.getX

  override def yPosition: Double = player.getY + player.getEyeHeight

  override def zPosition: Double = player.getZ

  override def markChanged() {}

  // ----------------------------------------------------------------------- //

  def containerSlotType: String =
    if (data.container.isEmpty) Slot.None
    else Option(Driver.driverFor(data.container, getClass)) match {
      case Some(driver: Container) => driver.providedSlot(data.container)
      case _ => Slot.None
    }

  def containerSlotTier: Int =
    if (data.container.isEmpty) Tier.None
    else Option(Driver.driverFor(data.container, getClass)) match {
      case Some(driver: Container) => driver.providedTier(data.container)
      case _ => Tier.None
    }

  override def internalComponents(): Iterable[ItemStack] = (0 until getContainerSize).collect {
    case slot if !getItem(slot).isEmpty && isComponentSlot(slot, getItem(slot)) => getItem(slot)
  }

  override def componentSlot(address: String): Int = components.indexWhere(_.exists(env => env.node != null && env.node.address == address))

  override def onMachineConnect(node: Node): Unit = onConnect(node)

  override def onMachineDisconnect(node: Node): Unit = onDisconnect(node)

  // ----------------------------------------------------------------------- //

  override def node: Node = Option(machine).fold(null: Node)(_.node)

  // ----------------------------------------------------------------------- //

  def update(world: World, player: PlayerEntity, slot: Int, selected: Boolean) {
    this.player = player
    if (!isInitialized) {
      isInitialized = true
      // This delayed initialization on the client side is required to allow
      // the server to set up the tablet wrapper first (since packets generated
      // in the component setup would otherwise be queued before the events that
      // caused this wrapper's initialization).
      connectComponents()
      components collect {
        case Some(buffer: api.internal.TextBuffer) =>
          buffer.setMaximumColorDepth(api.internal.TextBuffer.ColorDepth.FourBit)
          buffer.setMaximumResolution(80, 25)
      }

      client.PacketSender.sendMachineItemStateRequest(stack)
    }
    if (!world.isClientSide) {
      if (isCreative && world.getGameTime % Settings.get.tickFrequency == 0) {
        machine.node.asInstanceOf[Connector].changeBuffer(Double.PositiveInfinity)
      }
      machine.update()
      updateComponents()
      data.isRunning = machine.isRunning
      data.energy = tablet.node.globalBuffer()
      data.maxEnergy = tablet.node.globalBufferSize()

      if (lastRunning != machine.isRunning) {
        lastRunning = machine.isRunning
        setChanged()

        player match {
          case mp: ServerPlayerEntity => server.PacketSender.sendMachineItemState(mp, stack, machine.isRunning)
          case _ =>
        }

        if (machine.isRunning) {
          components collect {
            case Some(buffer: api.internal.TextBuffer) =>
              buffer.setPowerState(true)
          }
        }
      }
    }
  }

  // ----------------------------------------------------------------------- //

  override def loadData(nbt: CompoundNBT) {
    data.loadData(nbt)
  }

  override def saveData(nbt: CompoundNBT) {
    saveComponents()
    data.saveData(nbt)
  }
}

object Tablet {
  // This is super-hacky, but since it's only used on the client we get away
  // with storing context information for analyzing a block in the singleton.
  var currentlyAnalyzing: Option[(BlockPosition, Direction, Float, Float, Float)] = None

  def getId(stack: ItemStack): String = {
    val data = stack.getOrCreateTag
    if (!data.contains(Settings.namespace + "tablet")) {
      data.putString(Settings.namespace + "tablet", UUID.randomUUID().toString)
    }
    data.getString(Settings.namespace + "tablet")
  }

  def get(stack: ItemStack, holder: PlayerEntity): TabletWrapper = {
    if (holder.level.isClientSide) Client.get(stack, holder)
    else Server.get(stack, holder)
  }

  @SubscribeEvent
  def onWorldSave(e: WorldEvent.Save) {
    Server.saveAll(e.getWorld.asInstanceOf[World])
  }

  @SubscribeEvent
  def onWorldUnload(e: WorldEvent.Unload) {
    Client.clear(e.getWorld.asInstanceOf[World])
    Server.clear(e.getWorld.asInstanceOf[World])
  }

  @SubscribeEvent
  def onClientTick(e: ClientTickEvent) {
    Client.cleanUp()
    ServerLifecycleHooks.getCurrentServer match {
      case integrated: IntegratedServer if Minecraft.getInstance.isPaused =>
        // While the game is paused, manually keep all tablets alive, to avoid
        // them being cleared from the cache, causing them to stop.
        Client.keepAlive()
        Server.keepAlive()
      case _ => // Never mind!
    }
  }

  @SubscribeEvent
  def onServerTick(e: ServerTickEvent) {
    Server.cleanUp()
  }

  abstract class Cache extends Callable[TabletWrapper] with RemovalListener[String, TabletWrapper] {
    val cache: com.google.common.cache.Cache[String, TabletWrapper] = com.google.common.cache.CacheBuilder.newBuilder().
      expireAfterAccess(timeout, TimeUnit.SECONDS).
      removalListener(this).
      asInstanceOf[CacheBuilder[String, TabletWrapper]].
      build[String, TabletWrapper]()

    protected def timeout = 10

    // To allow access in cache entry init.
    private var currentStack: ItemStack = _

    private var currentHolder: PlayerEntity = _

    def get(stack: ItemStack, holder: PlayerEntity): TabletWrapper = {
      val id = getId(stack)
      cache.synchronized {
        currentStack = stack
        currentHolder = holder

        // if the item is still cached, we can detect if it is dirty (client side only)
        if (holder.level.isClientSide) {
          Client.getWeak(stack) match {
            case Some(weak) =>
              val timesChanged = holder.inventory.getTimesChanged
              if (timesChanged != weak.timesChanged) {
                if (!weak.isDirty) {
                  weak.isDirty = true
                  client.PacketSender.sendMachineItemStateRequest(stack)
                }
                weak.timesChanged = timesChanged
              }
            case _ =>
          }
        }

        var wrapper = cache.get(id, this)

        // Force re-load on world change, in case some components store a
        // reference to the world object.
        if (holder.level != wrapper.world) {
          wrapper.writeToNBT(clearState = false)
          wrapper.autoSave = false
          cache.invalidate(id)
          cache.cleanUp()
          wrapper = cache.get(id, this)
        }

        currentStack = null
        currentHolder = null

        wrapper.stack = stack
        wrapper.player = holder
        wrapper
      }
    }

    def call: TabletWrapper = {
      new TabletWrapper(currentStack, currentHolder)
    }

    def onRemoval(e: RemovalNotification[String, TabletWrapper]) {
      val tablet = e.getValue
      if (tablet.node != null) {
        // Server.
        if (tablet.autoSave) tablet.writeToNBT()
        tablet.machine.stop()
        for (node <- tablet.machine.node.network.nodes) {
          node.remove()
        }
        if (tablet.autoSave) tablet.writeToNBT()
        tablet.setChanged()
      }
    }

    def clear(world: World) {
      cache.synchronized {
        val tabletsInWorld = cache.asMap.filter(_._2.world == world)
        cache.invalidateAll(asJavaIterable(tabletsInWorld.keys))
        cache.cleanUp()
      }
    }

    def cleanUp() {
      cache.synchronized(cache.cleanUp())
    }

    def keepAlive(): ImmutableMap[String, TabletWrapper] = {
      // Just touching to update last access time.
      cache.getAllPresent(asJavaIterable(cache.asMap.keys))
    }
  }

  object Client extends Cache {
    override protected def timeout = 5

    def getWeak(stack: ItemStack): Option[TabletWrapper] = {
      val key = getId(stack)
      val map = cache.asMap
      if (map.containsKey(key))
        Some(map.entrySet.find(entry => entry.getKey == key).get.getValue)
      else
        None
    }

    def get(stack: ItemStack): Option[TabletWrapper] = {
      if (stack.hasTag && stack.getTag.contains(Settings.namespace + "tablet")) {
        val id = stack.getTag.getString(Settings.namespace + "tablet")
        cache.synchronized(Option(cache.getIfPresent(id)))
      }
      else None
    }
  }

  object Server extends Cache {
    def saveAll(world: World) {
      cache.synchronized {
        for (tablet <- cache.asMap.values if tablet.world == world) {
          tablet.writeToNBT()
        }
      }
    }
  }

}
