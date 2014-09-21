package li.cil.oc.common.item

import java.util.UUID
import java.util.concurrent.{Callable, TimeUnit}

import com.google.common.cache.{CacheBuilder, RemovalListener, RemovalNotification}
import cpw.mods.fml.common.eventhandler.SubscribeEvent
import cpw.mods.fml.common.gameevent.TickEvent.{ClientTickEvent, ServerTickEvent}
import cpw.mods.fml.relauncher.{Side, SideOnly}
import li.cil.oc.api.driver.Container
import li.cil.oc.api.machine.Owner
import li.cil.oc.api.network.{Connector, Message, Node}
import li.cil.oc.api.{Machine, Rotatable}
import li.cil.oc.common.GuiType
import li.cil.oc.common.inventory.ComponentInventory
import li.cil.oc.util.ItemUtils.TabletData
import li.cil.oc.util.RotationHelper
import li.cil.oc.{OpenComputers, Settings, api}
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.world.World
import net.minecraftforge.common.util.ForgeDirection
import net.minecraftforge.event.world.WorldEvent

class Tablet(val parent: Delegator) extends Delegate {
  // Must be assembled to be usable so we hide it in the item list.
  showInItemList = false

  override def maxStackSize = 1

  private var iconOn: Option[Icon] = None
  private var iconOff: Option[Icon] = None

  @SideOnly(Side.CLIENT)
  override def icon(stack: ItemStack, pass: Int) = Tablet.Client.get(stack) match {
    case Some(wrapper) => if (wrapper.isRunning) iconOn else iconOff
    case _ => super.icon(stack, pass)
  }

  override def registerIcons(iconRegister: IconRegister) = {
    super.registerIcons(iconRegister)

    iconOn = Option(iconRegister.registerIcon(Settings.resourceDomain + ":TabletOn"))
    iconOff = Option(iconRegister.registerIcon(Settings.resourceDomain + ":TabletOff"))
  }

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
    else {
      if (world.isRemote) Tablet.Client.remove(stack)
      else Tablet.Server.remove(stack)
    }
    player.swingItem()
    stack
  }
}

class TabletWrapper(var stack: ItemStack, var holder: EntityPlayer) extends ComponentInventory with Container with Owner with Rotatable {
  lazy val computer = if (holder.worldObj.isRemote) null else Machine.create(this)

  val data = new TabletData()

  def items = data.items

  override def facing = RotationHelper.fromYaw(holder.rotationYaw)

  override def toLocal(value: ForgeDirection) = value // TODO do we care?

  override def toGlobal(value: ForgeDirection) = value // TODO do we care?

  def readFromNBT() {
    if (stack.hasTagCompound) {
      val data = stack.getTagCompound
      if (!world.isRemote) {
        computer.load(data.getCompoundTag(Settings.namespace + "data"))
      }
      load(data)
    }
  }

  def writeToNBT() {
    if (!stack.hasTagCompound) {
      stack.setTagCompound(new NBTTagCompound())
    }
    val data = stack.getTagCompound
    if (!world.isRemote) {
      if (!data.hasKey(Settings.namespace + "data")) {
        data.setTag(Settings.namespace + "data", new NBTTagCompound())
      }
      computer.save(data.getCompoundTag(Settings.namespace + "data"))
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

  override def markDirty() {}

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

  override def isRunning = if (world.isRemote) {
    val computerData = stack.getTagCompound.getCompoundTag(Settings.namespace + "data")
    val state = computerData.getIntArray("state").headOption.getOrElse(0)
    state != 0
  }
  else computer.isRunning

  override def isPaused = computer.isPaused

  override def start() = computer.start()

  override def pause(seconds: Double) = computer.pause(seconds)

  override def stop() = computer.stop()

  override def signal(name: String, args: AnyRef*) = computer.signal(name, args)

  // ----------------------------------------------------------------------- //

  def update(world: World, player: EntityPlayer, slot: Int, selected: Boolean) {
    holder = player
    if (!world.isRemote) {
      computer.node.asInstanceOf[Connector].changeBuffer(500)
      computer.update()
      updateComponents()
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
    Client.clear()
    Server.clear()
  }

  @SubscribeEvent
  def onClientTick(e: ClientTickEvent) {
    Client.cleanUp()
  }

  @SubscribeEvent
  def onServerTick(e: ServerTickEvent) {
    Server.cleanUp()
  }

  abstract class Cache extends Callable[TabletWrapper] with RemovalListener[String, TabletWrapper] {
    val cache = com.google.common.cache.CacheBuilder.newBuilder().
      expireAfterAccess(10, TimeUnit.SECONDS).
      removalListener(this).
      asInstanceOf[CacheBuilder[String, TabletWrapper]].
      build[String, TabletWrapper]()

    // To allow access in cache entry init.
    private var currentStack: ItemStack = _

    private var currentHolder: EntityPlayer = _

    def get(stack: ItemStack, holder: EntityPlayer) = {
      if (!stack.hasTagCompound) {
        stack.setTagCompound(new NBTTagCompound())
      }
      if (!stack.getTagCompound.hasKey(Settings.namespace + "tablet")) {
        stack.getTagCompound.setString(Settings.namespace + "tablet", UUID.randomUUID().toString)
      }
      val id = stack.getTagCompound.getString(Settings.namespace + "tablet")
      cache.synchronized {
        currentStack = stack
        currentHolder = holder
        val wrapper = cache.get(id, this)
        wrapper.stack = stack
        wrapper.holder = holder
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
        tablet.writeToNBT()
        tablet.stop()
        tablet.node.remove()
      }
    }

    def remove(stack: ItemStack) {
      cache.synchronized {
        cache.invalidate(stack)
        cache.cleanUp()
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
        import scala.collection.convert.WrapAsScala._
        for (tablet <- cache.asMap.values if tablet.world == world) {
          tablet.writeToNBT()
        }
      }
    }
  }

}
