package li.cil.oc.common.tileentity

import cpw.mods.fml.common.{Loader, Optional}
import cpw.mods.fml.relauncher.{SideOnly, Side}
import li.cil.oc.api.network.Node
import li.cil.oc.server.{PacketSender => ServerPacketSender, component}
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.common.MinecraftForge
import stargatetech2.api.StargateTechAPI
import stargatetech2.api.bus.{BusEvent, IBusDevice}

@Optional.Interface(iface = "stargatetech2.api.bus.IBusDevice", modid = "StargateTech2")
trait AbstractBusAware extends TileEntity with ComponentInventory with IBusDevice {
  def getInterfaces(side: Int) =
    if (isAbstractBusAvailable) {
      if (isServer) {
        components collect {
          case Some(abstractBus: component.AbstractBus) => abstractBus.busInterface
        }
      }
      else fakeInterface
    }
    else null

  protected var _isAbstractBusAvailable = false

  private lazy val fakeInterface = Array(StargateTechAPI.api.getFactory.getIBusInterface(this, null))

  def getXCoord = x

  def getYCoord = y

  def getZCoord = z

  def isAbstractBusAvailable = _isAbstractBusAvailable

  def isAbstractBusAvailable_=(value: Boolean) = {
    if (value != isAbstractBusAvailable) {
      _isAbstractBusAvailable = value
      if (isAbstractBusAvailable) addAbstractBus()
      else removeAbstractBus()
      world.notifyBlocksOfNeighborChange(x, y, z, block.blockID)
      if (isServer) ServerPacketSender.sendAbstractBusState(this)
      else world.markBlockForRenderUpdate(x, y, z)
    }
    this
  }

  @SideOnly(Side.CLIENT)
  override def readFromNBTForClient(nbt: NBTTagCompound) {
    super.readFromNBTForClient(nbt)
    isAbstractBusAvailable = nbt.getBoolean("isAbstractBusAvailable")
  }

  override def writeToNBTForClient(nbt: NBTTagCompound) {
    super.writeToNBTForClient(nbt)
    nbt.setBoolean("isAbstractBusAvailable", isAbstractBusAvailable)
  }

  override protected def onItemAdded(slot: Int, stack: ItemStack) {
    super.onItemAdded(slot, stack)
    if (isServer) {
      isAbstractBusAvailable = hasAbstractBusCard
    }
  }

  override protected def onItemRemoved(slot: Int, stack: ItemStack) {
    super.onItemRemoved(slot, stack)
    if (isServer) {
      isAbstractBusAvailable = hasAbstractBusCard
    }
  }

  abstract override def onConnect(node: Node) {
    super.onConnect(node)
    isAbstractBusAvailable = hasAbstractBusCard
  }

  abstract override def onDisconnect(node: Node) {
    super.onDisconnect(node)
    removeAbstractBus()
  }

  protected def addAbstractBus() {
    // Mod loaded check to avoid class not found errors.
    if (isServer && Loader.isModLoaded("StargateTech2")) {
      MinecraftForge.EVENT_BUS.post(new BusEvent.AddToNetwork(world, x, y, z))
    }
  }

  protected def removeAbstractBus() {
    // Mod loaded check to avoid class not found errors.
    if (isServer && Loader.isModLoaded("StargateTech2")) {
      MinecraftForge.EVENT_BUS.post(new BusEvent.RemoveFromNetwork(world, x, y, z))
    }
  }

  protected def hasAbstractBusCard = components exists {
    case Some(_: component.AbstractBus) => true
    case _ => false
  }
}
