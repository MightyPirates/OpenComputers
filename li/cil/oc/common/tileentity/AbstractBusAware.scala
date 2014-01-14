package li.cil.oc.common.tileentity

import cpw.mods.fml.common.{Loader, Optional}
import cpw.mods.fml.relauncher.{SideOnly, Side}
import li.cil.oc.api.network.Node
import li.cil.oc.server.{PacketSender => ServerPacketSender, component}
import li.cil.oc.util.mods.StargateTech2
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import stargatetech2.api.StargateTechAPI
import stargatetech2.api.bus.{IBusInterface, IBusDevice}

// IMPORTANT: for some reason that is beyond me we cannot implement the
// IBusDevice here directly, since we'll get an error if the interface is not
// provided (i.e. if SGT2 isn't installed), even if we tell FML to strip it.
// Assuming FML properly strips the interface (and it looks like it does, when
// inspecting it in the debugger, i.e. getInterfaces() doesn't contain it), it
// probably is something derping up in the class loader... the thing that
// confuses me the most, though, is that it apparently works for redstone and
// the CC interface, so... yeah. I'm out of ideas.
trait AbstractBusAware extends TileEntity with ComponentInventory { self: IBusDevice =>
  protected var _isAbstractBusAvailable: Boolean = _

  protected lazy val fakeInterface = Array[AnyRef](StargateTechAPI.api.getFactory.getIBusInterface(this, null))

  @Optional.Method(modid = "StargateTech2")
  def getInterfaces(side: Int) =
    if (isAbstractBusAvailable) {
      if (isServer) {
        components collect {
          case Some(abstractBus: component.AbstractBus) => abstractBus.busInterface
        }
      }
      else fakeInterface.map(_.asInstanceOf[IBusInterface])
    }
    else null

  def getWorld = world

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

  // IMPORTANT: all the Loader.isModLoaded checks are there to a void class
  // not found errors! Also, don't try to move them further down in the logic
  // (e.g. into StargateTech2) since that would not help avoid the error anymore.

  protected def addAbstractBus() {
    if (isServer && Loader.isModLoaded("StargateTech2")) {
      StargateTech2.addDevice(world, x, y, z)
    }
  }

  protected def removeAbstractBus() {
    if (isServer && Loader.isModLoaded("StargateTech2")) {
      StargateTech2.removeDevice(world, x, y, z)
    }
  }

  protected def hasAbstractBusCard = Loader.isModLoaded("StargateTech2") && (components exists {
    case Some(_: component.AbstractBus) => true
    case _ => false
  })
}
