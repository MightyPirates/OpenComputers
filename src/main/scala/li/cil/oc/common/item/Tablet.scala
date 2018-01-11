package li.cil.oc.common.item

import java.lang.Iterable
import java.util
import java.util.UUID
import java.util.concurrent.Callable
import java.util.concurrent.TimeUnit

import com.google.common.cache.CacheBuilder
import com.google.common.cache.RemovalListener
import com.google.common.cache.RemovalNotification
import cpw.mods.fml.common.eventhandler.SubscribeEvent
import cpw.mods.fml.common.gameevent.TickEvent.ClientTickEvent
import cpw.mods.fml.common.gameevent.TickEvent.ServerTickEvent
import cpw.mods.fml.relauncher.Side
import cpw.mods.fml.relauncher.SideOnly
import li.cil.oc.Constants
import li.cil.oc.Localization
import li.cil.oc.OpenComputers
import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.api.Driver
import li.cil.oc.api.Machine
import li.cil.oc.api.driver.item.Chargeable
import li.cil.oc.api.driver.item.Container
import li.cil.oc.api.internal
import li.cil.oc.api.machine.MachineHost
import li.cil.oc.api.network.Connector
import li.cil.oc.api.network.Message
import li.cil.oc.api.network.Node
import li.cil.oc.client.KeyBindings
import li.cil.oc.common.GuiType
import li.cil.oc.common.Slot
import li.cil.oc.common.Tier
import li.cil.oc.common.inventory.ComponentInventory
import li.cil.oc.common.item.data.TabletData
import li.cil.oc.integration.opencomputers.DriverScreen
import li.cil.oc.server.component
import li.cil.oc.util.Audio
import li.cil.oc.util.BlockPosition
import li.cil.oc.util.ExtendedNBT._
import li.cil.oc.util.Rarity
import li.cil.oc.util.RotationHelper
import li.cil.oc.util.Tooltip
import net.minecraft.client.Minecraft
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.server.MinecraftServer
import net.minecraft.server.integrated.IntegratedServer
import net.minecraft.world.World
import net.minecraftforge.common.util.ForgeDirection
import net.minecraftforge.event.world.WorldEvent

import scala.collection.convert.WrapAsJava._
import scala.collection.convert.WrapAsScala._

class Tablet(val parent: Delegator) extends traits.Delegate with Chargeable {
  final val TimeToAnalyze = 10

  // Must be assembled to be usable so we hide it in the item list.
  showInItemList = false

  override def maxStackSize = 1

  private var iconOn: Option[Icon] = None
  private var iconOff: Option[Icon] = None

  @SideOnly(Side.CLIENT)
  override def icon(stack: ItemStack, pass: Int) = {
    if (stack.hasTagCompound) {
      val data = new TabletData(stack)
      if (data.isRunning) iconOn else iconOff
    } else super.icon(stack, pass)
  }

  override def registerIcons(iconRegister: IconRegister) = {
    super.registerIcons(iconRegister)

    iconOn = Option(iconRegister.registerIcon(Settings.resourceDomain + ":TabletOn"))
    iconOff = Option(iconRegister.registerIcon(Settings.resourceDomain + ":TabletOff"))
  }

  // ----------------------------------------------------------------------- //

  override protected def tooltipExtended(stack: ItemStack, tooltip: util.List[String]): Unit = {
    if (KeyBindings.showExtendedTooltips) {
      val info = new TabletData(stack)
      // Ignore/hide the screen.
      val components = info.items.drop(1)
      if (components.length > 1) {
        tooltip.addAll(Tooltip.get("Server.Components"))
        components.collect {
          case Some(component) => tooltip.add("- " + component.getDisplayName)
        }
      }
    }
  }

  override def rarity(stack: ItemStack) = {
    val data = new TabletData(stack)
    Rarity.byTier(data.tier)
  }

  override def isDamageable = true

  override def damage(stack: ItemStack) = {
    val nbt = stack.getTagCompound
    if (nbt != null) {
      val data = new TabletData()
      data.load(nbt)
      (data.maxEnergy - data.energy).toInt
    }
    else 100
  }

  override def maxDamage(stack: ItemStack) = {
    val nbt = stack.getTagCompound
    if (nbt != null) {
      val data = new TabletData()
      data.load(nbt)
      data.maxEnergy.toInt max 1
    }
    else 100
  }

  // ----------------------------------------------------------------------- //

  def canCharge(stack: ItemStack): Boolean = true

  def charge(stack: ItemStack, amount: Double, simulate: Boolean): Double = {
    if (amount < 0) amount
    else {
      val data = new TabletData(stack)
      val charge = math.min(data.maxEnergy - data.energy, amount)
      if (!simulate) {
        data.energy += charge
        data.save(stack)
      }
      amount - charge
    }
  }

  // ----------------------------------------------------------------------- //

  override def update(stack: ItemStack, world: World, entity: Entity, slot: Int, selected: Boolean) =
    entity match {
      case player: EntityPlayer =>
        // Play an audio cue to let players know when they finished analyzing a block.
        if (world.isRemote && player.getItemInUseDuration == TimeToAnalyze && api.Items.get(player.getItemInUse) == api.Items.get(Constants.ItemName.Tablet)) {
          Audio.play(player.posX.toFloat, player.posY.toFloat + 2, player.posZ.toFloat, ".")
        }
        Tablet.get(stack, player).update(world, player, slot, selected)
      case _ =>
    }

  override def onItemUseFirst(stack: ItemStack, player: EntityPlayer, position: BlockPosition, side: Int, hitX: Float, hitY: Float, hitZ: Float): Boolean = {
    Tablet.currentlyAnalyzing = Some((position, side, hitX, hitY, hitZ))
    super.onItemUseFirst(stack, player, position, side, hitX, hitY, hitZ)
  }

  override def onItemUse(stack: ItemStack, player: EntityPlayer, position: BlockPosition, side: Int, hitX: Float, hitY: Float, hitZ: Float): Boolean = {
    player.setItemInUse(stack, getMaxItemUseDuration(stack))
    true
  }

  override def onItemRightClick(stack: ItemStack, world: World, player: EntityPlayer) = {
    player.setItemInUse(stack, getMaxItemUseDuration(stack))
    stack
  }

  override def getMaxItemUseDuration(stack: ItemStack): Int = 72000

  override def onPlayerStoppedUsing(stack: ItemStack, player: EntityPlayer, duration: Int): Unit = {
    val world = player.getEntityWorld
    val didAnalyze = getMaxItemUseDuration(stack) - duration >= TimeToAnalyze
    if (didAnalyze) {
      if (!world.isRemote) {
        Tablet.currentlyAnalyzing match {
          case Some((position, side, hitX, hitY, hitZ)) => try {
            val computer = Tablet.get(stack, player).machine
            if (computer.isRunning) {
              val data = new NBTTagCompound()
              computer.node.sendToReachable("tablet.use", data, stack, player, position, ForgeDirection.getOrientation(side), Float.box(hitX), Float.box(hitY), Float.box(hitZ))
              if (!data.hasNoTags) {
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
      if (player.isSneaking) {
        if (!world.isRemote) {
          val tablet = Tablet.Server.get(stack, player)
          tablet.machine.stop()
          if (tablet.data.tier > Tier.One) {
            player.openGui(OpenComputers, GuiType.TabletInner.id, world, 0, 0, 0)
          }
        }
      }
      else {
        if (!world.isRemote) {
          val computer = Tablet.get(stack, player).machine
          computer.start()
          computer.lastError match {
            case message if message != null => player.addChatMessage(Localization.Analyzer.LastError(message))
            case _ =>
          }
        }
        else {
          player.openGui(OpenComputers, GuiType.Tablet.id, world, 0, 0, 0)
        }
      }
    }
  }
}

class TabletWrapper(var stack: ItemStack, var player: EntityPlayer) extends ComponentInventory with MachineHost with internal.Tablet {
  // Remember our *original* world, so we know which tablets to clear on dimension
  // changes of players holding tablets - since the player entity instance may be
  // kept the same and components are not required to properly handle world changes.
  val world = player.worldObj

  lazy val machine = if (world.isRemote) null else Machine.create(this)

  val data = new TabletData()

  val tablet = if (world.isRemote) null else new component.Tablet(this)

  var autoSave = true

  private var isInitialized = !world.isRemote

  private var lastRunning = false

  def isCreative = data.tier == Tier.Four

  def items = data.items

  override def facing = RotationHelper.fromYaw(player.rotationYaw)

  override def toLocal(value: ForgeDirection) =
    RotationHelper.toLocal(ForgeDirection.NORTH, facing, value)

  override def toGlobal(value: ForgeDirection) =
    RotationHelper.toGlobal(ForgeDirection.NORTH, facing, value)

  def readFromNBT() {
    if (stack.hasTagCompound) {
      val data = stack.getTagCompound
      load(data)
      if (!world.isRemote) {
        tablet.load(data.getCompoundTag(Settings.namespace + "component"))
        machine.load(data.getCompoundTag(Settings.namespace + "data"))
      }
    }
  }

  def writeToNBT(clearState: Boolean = true) {
    if (!stack.hasTagCompound) {
      stack.setTagCompound(new NBTTagCompound())
    }
    val data = stack.getTagCompound
    if (!world.isRemote) {
      if (!data.hasKey(Settings.namespace + "data")) {
        data.setTag(Settings.namespace + "data", new NBTTagCompound())
      }
      data.setNewCompoundTag(Settings.namespace + "component", tablet.save)
      data.setNewCompoundTag(Settings.namespace + "data", machine.save)

      if (clearState) {
        // Force tablets into stopped state to avoid errors when trying to
        // load deleted machine states.
        data.getCompoundTag(Settings.namespace + "data").removeTag("state")
      }
    }
    save(data)
  }

  readFromNBT()
  if (!world.isRemote) {
    api.Network.joinNewNetwork(machine.node)
    val charge = math.max(0, this.data.energy - tablet.node.globalBuffer)
    tablet.node.changeBuffer(charge)
    writeToNBT()
  }

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

  override def host = this

  override def getSizeInventory = items.length

  override def isItemValidForSlot(slot: Int, stack: ItemStack) = slot == getSizeInventory - 1 && (Option(Driver.driverFor(stack, getClass)) match {
    case Some(driver) =>
      // Same special cases, similar as in robot, but allow keyboards,
      // because clip-on keyboards kinda seem to make sense, I guess.
      driver != DriverScreen &&
        driver.slot(stack) == containerSlotType &&
        driver.tier(stack) <= containerSlotTier
    case _ => false
  })

  override def isUseableByPlayer(player: EntityPlayer) = machine != null && machine.canInteract(player.getCommandSenderName)

  override def markDirty(): Unit = {
    data.save(stack)
    player.inventory.markDirty()
  }

  // ----------------------------------------------------------------------- //

  override def xPosition = player.posX

  override def yPosition = player.posY + player.getEyeHeight

  override def zPosition = player.posZ

  override def markChanged() {}

  // ----------------------------------------------------------------------- //

  def containerSlotType = data.container.fold(Slot.None)(stack =>
    Option(Driver.driverFor(stack, getClass)) match {
      case Some(driver: Container) => driver.providedSlot(stack)
      case _ => Slot.None
    })

  def containerSlotTier = data.container.fold(Tier.None)(stack =>
    Option(Driver.driverFor(stack, getClass)) match {
      case Some(driver: Container) => driver.providedTier(stack)
      case _ => Tier.None
    })

  override def internalComponents(): Iterable[ItemStack] = (0 until getSizeInventory).collect {
    case slot if getStackInSlot(slot) != null && isComponentSlot(slot, getStackInSlot(slot)) => getStackInSlot(slot)
  }

  override def componentSlot(address: String) = components.indexWhere(_.exists(env => env.node != null && env.node.address == address))

  override def onMachineConnect(node: Node) = onConnect(node)

  override def onMachineDisconnect(node: Node) = onDisconnect(node)

  // ----------------------------------------------------------------------- //

  override def node = Option(machine).fold(null: Node)(_.node)

  // ----------------------------------------------------------------------- //

  def update(world: World, player: EntityPlayer, slot: Int, selected: Boolean) {
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
    }
    if (!world.isRemote) {
      if (isCreative && world.getTotalWorldTime % Settings.get.tickFrequency == 0) {
        machine.node.asInstanceOf[Connector].changeBuffer(Double.PositiveInfinity)
      }
      machine.update()
      updateComponents()
      data.isRunning = machine.isRunning
      data.energy = tablet.node.globalBuffer()
      data.maxEnergy = tablet.node.globalBufferSize()

      if (lastRunning != machine.isRunning) {
        lastRunning = machine.isRunning
        markDirty()

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

  override def load(nbt: NBTTagCompound) {
    data.load(nbt)
  }

  override def save(nbt: NBTTagCompound) {
    saveComponents()
    data.save(nbt)
  }
}

object Tablet {
  // This is super-hacky, but since it's only used on the client we get away
  // with storing context information for analyzing a block in the singleton.
  var currentlyAnalyzing: Option[(BlockPosition, Int, Float, Float, Float)] = None

  def getId(stack: ItemStack) = {

    if (!stack.hasTagCompound) {
      stack.setTagCompound(new NBTTagCompound())
    }
    if (!stack.getTagCompound.hasKey(Settings.namespace + "tablet")) {
      stack.getTagCompound.setString(Settings.namespace + "tablet", UUID.randomUUID().toString)
    }
    stack.getTagCompound.getString(Settings.namespace + "tablet")
  }

  def get(stack: ItemStack, holder: EntityPlayer) = {
    if (holder.worldObj.isRemote) Client.get(stack, holder)
    else Server.get(stack, holder)
  }

  @SubscribeEvent
  def onWorldSave(e: WorldEvent.Save) {
    Server.saveAll(e.world)
  }

  @SubscribeEvent
  def onWorldUnload(e: WorldEvent.Unload) {
    Client.clear(e.world)
    Server.clear(e.world)
  }

  @SubscribeEvent
  def onClientTick(e: ClientTickEvent) {
    Client.cleanUp()
    MinecraftServer.getServer match {
      case integrated: IntegratedServer if Minecraft.getMinecraft.isGamePaused =>
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
    val cache = com.google.common.cache.CacheBuilder.newBuilder().
      expireAfterAccess(timeout, TimeUnit.SECONDS).
      removalListener(this).
      asInstanceOf[CacheBuilder[String, TabletWrapper]].
      build[String, TabletWrapper]()

    protected def timeout = 10

    // To allow access in cache entry init.
    private var currentStack: ItemStack = _

    private var currentHolder: EntityPlayer = _

    def get(stack: ItemStack, holder: EntityPlayer) = {
      val id = getId(stack)
      cache.synchronized {
        currentStack = stack
        currentHolder = holder
        var wrapper = cache.get(id, this)

        // Force re-load on world change, in case some components store a
        // reference to the world object.
        if (holder.worldObj != wrapper.world) {
          wrapper.writeToNBT(clearState = false)
          wrapper.autoSave = false
          cache.invalidate(id)
          cache.cleanUp()
          wrapper = cache.get(id, this)
        }

        wrapper.stack = stack
        wrapper.player = holder
        wrapper
      }
    }

    def call = {
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

    def keepAlive() = {
      // Just touching to update last access time.
      cache.getAllPresent(asJavaIterable(cache.asMap.keys))
    }
  }

  object Client extends Cache {
    override protected def timeout = 5

    def get(stack: ItemStack) = {
      if (stack.hasTagCompound && stack.getTagCompound.hasKey(Settings.namespace + "tablet")) {
        val id = stack.getTagCompound.getString(Settings.namespace + "tablet")
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
