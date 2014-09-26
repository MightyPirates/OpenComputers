package li.cil.oc.common.item

import java.util
import java.util.UUID
import java.util.concurrent.{Callable, TimeUnit}

import com.google.common.cache.{CacheBuilder, RemovalListener, RemovalNotification}
import cpw.mods.fml.common.{ITickHandler, TickType}
import cpw.mods.fml.relauncher.{Side, SideOnly}
import li.cil.oc.api.driver.Container
import li.cil.oc.api.machine.Owner
import li.cil.oc.api.network.{Message, Node}
import li.cil.oc.api.{Machine, Rotatable}
import li.cil.oc.common.GuiType
import li.cil.oc.common.inventory.ComponentInventory
import li.cil.oc.server.component
import li.cil.oc.util.ExtendedNBT._
import li.cil.oc.util.ItemUtils.TabletData
import li.cil.oc.util.{ItemUtils, RotationHelper}
import li.cil.oc.{OpenComputers, Settings, api}
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.world.World
import net.minecraftforge.common.ForgeDirection
import net.minecraftforge.event.ForgeSubscribe
import net.minecraftforge.event.world.WorldEvent

import scala.collection.convert.WrapAsScala._

class Tablet(val parent: Delegator) extends Delegate {
  // Must be assembled to be usable so we hide it in the item list.
  showInItemList = false

  override def maxStackSize = 1

  private var iconOn: Option[Icon] = None
  private var iconOff: Option[Icon] = None

  @SideOnly(Side.CLIENT)
  override def icon(stack: ItemStack, pass: Int) = {
    val nbt = stack.getTagCompound
    if (nbt != null) {
      val data = new ItemUtils.TabletData()
      data.load(nbt)
      if (data.isRunning) iconOn else iconOff
    }
    else super.icon(stack, pass)
  }

  override def registerIcons(iconRegister: IconRegister) = {
    super.registerIcons(iconRegister)

    iconOn = Option(iconRegister.registerIcon(Settings.resourceDomain + ":TabletOn"))
    iconOff = Option(iconRegister.registerIcon(Settings.resourceDomain + ":TabletOff"))
  }

  // ----------------------------------------------------------------------- //

  override def isDamageable = true

  override def damage(stack: ItemStack) = {
    val nbt = stack.getTagCompound
    if (nbt != null) {
      val data = new ItemUtils.TabletData()
      data.load(nbt)
      (data.maxEnergy - data.energy).toInt
    }
    else 100
  }

  override def maxDamage(stack: ItemStack) = {
    val nbt = stack.getTagCompound
    if (nbt != null) {
      val data = new ItemUtils.TabletData()
      data.load(nbt)
      data.maxEnergy.toInt max 1
    }
    else 100
  }

  // ----------------------------------------------------------------------- //

  override def update(stack: ItemStack, world: World, entity: Entity, slot: Int, selected: Boolean) =
    entity match {
      case player: EntityPlayer => Tablet.get(stack, player).update(world, player, slot, selected)
      case _ =>
    }

  override def onItemRightClick(stack: ItemStack, world: World, player: EntityPlayer) = {
    if (!player.isSneaking) {
      if (world.isRemote) {
        player.openGui(OpenComputers, GuiType.Tablet.id, world, 0, 0, 0)
      }
      else {
        Tablet.get(stack, player).start()
      }
    }
    else if (!world.isRemote) Tablet.Server.get(stack, player).computer.stop()
    player.swingItem()
    stack
  }
}

class TabletWrapper(var stack: ItemStack, var holder: EntityPlayer) extends ComponentInventory with Container with Owner with Rotatable {
  lazy val computer = if (holder.worldObj.isRemote) null else Machine.create(this)

  val data = new TabletData()

  val tablet = if (holder.worldObj.isRemote) null else new component.Tablet(this)

  private var isInitialized = !world.isRemote

  def items = data.items

  override def facing = RotationHelper.fromYaw(holder.rotationYaw)

  override def toLocal(value: ForgeDirection) = value // TODO do we care?

  override def toGlobal(value: ForgeDirection) = value // TODO do we care?

  def readFromNBT() {
    if (stack.hasTagCompound) {
      val data = stack.getTagCompound
      load(data)
      if (!world.isRemote) {
        tablet.load(data.getCompoundTag(Settings.namespace + "component"))
        computer.load(data.getCompoundTag(Settings.namespace + "data"))
      }
    }
  }

  def writeToNBT() {
    if (!stack.hasTagCompound) {
      stack.setTagCompound(new NBTTagCompound("tag"))
    }
    val data = stack.getTagCompound
    if (!world.isRemote) {
      if (!data.hasKey(Settings.namespace + "data")) {
        data.setTag(Settings.namespace + "data", new NBTTagCompound())
      }
      data.setNewCompoundTag(Settings.namespace + "component", tablet.save)
      data.setNewCompoundTag(Settings.namespace + "data", computer.save)

      // Force tablets into stopped state to avoid errors when trying to
      // load deleted machine states.
      data.getCompoundTag(Settings.namespace + "data").removeTag("state")
    }
    save(data)
  }

  readFromNBT()
  if (!world.isRemote) {
    api.Network.joinNewNetwork(computer.node)
    computer.stop()
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
      tablet.node.remove()
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

  override def maxComponents = 32

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

  def update(world: World, player: EntityPlayer, slot: Int, selected: Boolean) {
    holder = player
    if (!isInitialized) {
      isInitialized = true
      // This delayed initialization on the client side is required to allow
      // the server to set up the tablet wrapper first (since packets generated
      // in the component setup would otherwise be queued before the events that
      // caused this wrapper's initialization).
      connectComponents()
      components collect {
        case Some(buffer: api.component.TextBuffer) =>
          buffer.setMaximumColorDepth(api.component.TextBuffer.ColorDepth.FourBit)
          buffer.setMaximumResolution(80, 25)
      }
    }
    if (!world.isRemote) {
      computer.update()
      updateComponents()
      data.isRunning = computer.isRunning
      data.energy = tablet.node.globalBuffer()
      data.maxEnergy = tablet.node.globalBufferSize()
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

object Tablet extends ITickHandler {
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

  @ForgeSubscribe
  def onWorldSave(e: WorldEvent.Save) {
    Server.saveAll(e.world)
  }

  @ForgeSubscribe
  def onWorldUnload(e: WorldEvent.Unload) {
    Client.clear()
    Server.clear()
  }

  override def getLabel = "OpenComputers Tablet Cleanup Ticker"

  override def ticks = util.EnumSet.of(TickType.CLIENT, TickType.SERVER)

  override def tickStart(tickType: util.EnumSet[TickType], tickData: AnyRef*) {
    if (tickType.contains(TickType.CLIENT)) Client.cleanUp()
    if (tickType.contains(TickType.SERVER)) Server.cleanUp()
  }

  override def tickEnd(tickType: util.EnumSet[TickType], tickData: AnyRef*) {}

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
        val wrapper = cache.get(id, this)
        wrapper.stack = stack
        wrapper.holder = holder
        wrapper
      }
    }

    def call() = new TabletWrapper(currentStack, currentHolder)

    def onRemoval(e: RemovalNotification[String, TabletWrapper]) {
      val tablet = e.getValue
      if (tablet.node != null) {
        // Server.
        tablet.writeToNBT()
        tablet.computer.stop()
        for (node <- tablet.computer.node.network.nodes) {
          node.remove()
        }
        tablet.writeToNBT()
      }
    }

    def clear() {
      cache.synchronized {
        cache.invalidateAll()
        cache.cleanUp()
      }
    }

    def cleanUp() {
      cache.synchronized(cache.cleanUp())
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