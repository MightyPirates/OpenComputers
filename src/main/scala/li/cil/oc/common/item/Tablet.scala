package li.cil.oc.common.item

import java.util
import java.util.UUID
import java.util.concurrent.{Callable, TimeUnit}

import com.google.common.cache.{CacheBuilder, RemovalListener, RemovalNotification}
import cpw.mods.fml.common.{ITickHandler, TickType}
import li.cil.oc.api.Machine
import li.cil.oc.api.driver.Container
import li.cil.oc.api.machine.Owner
import li.cil.oc.api.network.{Connector, Message, Node}
import li.cil.oc.common.GuiType
import li.cil.oc.common.inventory.ComponentInventory
import li.cil.oc.{OpenComputers, Settings, api}
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.world.World
import net.minecraftforge.event.ForgeSubscribe
import net.minecraftforge.event.world.WorldEvent

class Tablet(val parent: Delegator) extends Delegate {
  override def maxStackSize = 1

  override def update(stack: ItemStack, world: World, player: Entity, slot: Int, selected: Boolean) =
    Tablet.get(stack, player).update(world, player, slot, selected)

  override def onItemRightClick(stack: ItemStack, world: World, player: EntityPlayer) = {
    if (!player.isSneaking) {
      if (world.isRemote) {
        player.openGui(OpenComputers, GuiType.Tablet.id, world, 0, 0, 0)
      }
      else {
        Tablet.get(stack, player).start()
      }
      player.swingItem()
    }
    super.onItemRightClick(stack, world, player)
  }
}

class TabletWrapper(var stack: ItemStack, var holder: Entity) extends ComponentInventory with Container with Owner {
  lazy val computer = if (holder.worldObj.isRemote) null else Machine.create(this)

  val items = Array(
    Option(api.Items.get("cpu1").createItemStack(1)),
    Option(api.Items.get("ram4").createItemStack(1)),
    Option(api.Items.get("screen1").createItemStack(1)),
    Option(api.Items.get("keyboard").createItemStack(1)),
    Option(api.Items.get("graphicsCard2").createItemStack(1)),
    Option(api.Items.get("batteryUpgrade2").createItemStack(1)),
    Option(api.Items.get("navigationUpgrade").createItemStack(1)),
    Option(api.Items.get("openOS").createItemStack(1)),
    Option(api.Items.get("hdd1").createItemStack(1)),
    Option(api.Items.get("wlanCard").createItemStack(1))
  )

  def readFromNBT() {
    if (stack.hasTagCompound) {
      val data = stack.getTagCompound.getCompoundTag(Settings.namespace + "data")
      if (!world.isRemote) {
        computer.node.load(data)
      }
      load(data)
    }
  }

  def writeToNBT() {
    if (!stack.hasTagCompound) {
      stack.setTagCompound(new NBTTagCompound("tag"))
    }
    val nbt = stack.getTagCompound
    if (!nbt.hasKey(Settings.namespace + "data")) {
      nbt.setTag(Settings.namespace + "data", new NBTTagCompound())
    }
    val data = stack.getTagCompound.getCompoundTag(Settings.namespace + "data")
    if (!world.isRemote) {
      computer.node.save(data)
    }
    save(data)
  }

  readFromNBT()
  if (world.isRemote) {
    connectComponents()
    components collect {
      case Some(buffer: api.component.TextBuffer) =>
        buffer.setMaximumColorDepth(api.component.TextBuffer.ColorDepth.FourBit)
        buffer.setMaximumResolution(80, 25)
    }
  }
  else {
    api.Network.joinNewNetwork(computer.node)
    writeToNBT()
  }

  // ----------------------------------------------------------------------- //

  override def onConnect(node: Node) {
    if (node == this.node) {
      connectComponents()
    }
    else node.host match {
      case buffer: api.component.TextBuffer =>
        buffer.setMaximumColorDepth(api.component.TextBuffer.ColorDepth.FourBit)
        buffer.setMaximumResolution(80, 25)
      case _ =>
    }
  }

  override protected def connectItemNode(node: Node) {
    super.connectItemNode(node)
    if (node != null) node.host match {
      case buffer: api.component.TextBuffer => components collect {
        case Some(keyboard: api.component.Keyboard) => buffer.node.connect(keyboard.node)
      }
      case keyboard: api.component.Keyboard => components collect {
        case Some(buffer: api.component.TextBuffer) => keyboard.node.connect(buffer.node)
      }
      case _ =>
    }
  }

  override def onDisconnect(node: Node) {
    if (node == this.node) {
      disconnectComponents()
    }
  }

  override def onMessage(message: Message) {}

  override def componentContainer = this

  override def getSizeInventory = items.length

  override def isItemValidForSlot(slot: Int, stack: ItemStack) = true

  override def isUseableByPlayer(player: EntityPlayer) = canInteract(player.getCommandSenderName)

  override def onInventoryChanged() {}

  // ----------------------------------------------------------------------- //

  override def xPosition = holder.posX

  override def yPosition = holder.posY + 1

  override def zPosition = holder.posZ

  override def markChanged() {}

  // ----------------------------------------------------------------------- //

  override def x = holder.posX.toInt

  override def y = holder.posY.toInt + 1

  override def z = holder.posZ.toInt

  override def world = holder.worldObj

  override def installedMemory = items.foldLeft(0)((acc, itemOption) => acc + (itemOption match {
    case Some(item) => Option(api.Driver.driverFor(item)) match {
      case Some(driver: api.driver.Memory) => driver.amount(item)
      case _ => 0
    }
    case _ => 0
  }))

  override def maxComponents = items.foldLeft(0)((acc, itemOption) => acc + (itemOption match {
    case Some(item) => Option(api.Driver.driverFor(item)) match {
      case Some(driver: api.driver.Processor) => driver.supportedComponents(item)
      case _ => 0
    }
    case _ => 0
  }))

  override def markAsChanged() {}

  override def onMachineConnect(node: Node) = onConnect(node)

  override def onMachineDisconnect(node: Node) = onDisconnect(node)

  // ----------------------------------------------------------------------- //

  override def node = Option(computer).fold(null: Node)(_.node)

  override def canInteract(player: String) = computer.canInteract(player)

  override def isRunning = computer.isRunning

  override def isPaused = computer.isPaused

  override def start() = computer.start()

  override def pause(seconds: Double) = computer.pause(seconds)

  override def stop() = computer.stop()

  override def signal(name: String, args: AnyRef*) = computer.signal(name, args)

  // ----------------------------------------------------------------------- //

  def update(world: World, player: Entity, slot: Int, selected: Boolean) {
    holder = player
    if (!world.isRemote) {
      computer.node.asInstanceOf[Connector].changeBuffer(500)
      computer.update()
      updateComponents()
    }
  }
}

object Tablet extends Callable[TabletWrapper] with RemovalListener[String, TabletWrapper] with ITickHandler {
  val clientCache = com.google.common.cache.CacheBuilder.newBuilder().
    expireAfterAccess(10, TimeUnit.SECONDS).
    removalListener(this).
    asInstanceOf[CacheBuilder[String, TabletWrapper]].
    build[String, TabletWrapper]()

  val serverCache = com.google.common.cache.CacheBuilder.newBuilder().
    expireAfterAccess(10, TimeUnit.SECONDS).
    removalListener(this).
    asInstanceOf[CacheBuilder[String, TabletWrapper]].
    build[String, TabletWrapper]()

  // To allow access in cache entry init.
  private var currentStack: ItemStack = _

  private var currentHolder: Entity = _

  def get(stack: ItemStack, holder: Entity) = (if (holder.worldObj.isRemote) clientCache else serverCache).synchronized {
    currentStack = stack
    currentHolder = holder
    if (!stack.hasTagCompound) {
      stack.setTagCompound(new NBTTagCompound("tag"))
    }
    if (!stack.getTagCompound.hasKey(Settings.namespace + "tablet")) {
      stack.getTagCompound.setString(Settings.namespace + "tablet", UUID.randomUUID().toString)
    }
    val id = stack.getTagCompound.getString(Settings.namespace + "tablet")
    val wrapper =
      if (holder.worldObj.isRemote)
        clientCache.get(id, this)
      else
        serverCache.get(id, this)
    wrapper.stack = stack
    wrapper.holder = holder
    wrapper
  }

  def call = {
    new TabletWrapper(currentStack, currentHolder)
  }

  def onRemoval(e: RemovalNotification[String, TabletWrapper]) {
    val tablet = e.getValue
    if (tablet.node != null) {
      // Server.
      tablet.stop()
      tablet.node.remove()
      tablet.writeToNBT()
    }
  }

  @ForgeSubscribe
  def onWorldUnload(e: WorldEvent.Unload) {
    clientCache.invalidateAll()
    clientCache.cleanUp()
    serverCache.invalidateAll()
    serverCache.cleanUp()
  }

  override def getLabel = "OpenComputers Tablet Cleanup Ticker"

  override def ticks = util.EnumSet.of(TickType.CLIENT, TickType.SERVER)

  override def tickStart(tickType: util.EnumSet[TickType], tickData: AnyRef*) {
    clientCache.cleanUp()
    serverCache.cleanUp()
  }

  override def tickEnd(tickType: util.EnumSet[TickType], tickData: AnyRef*) {}
}