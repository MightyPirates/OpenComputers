package li.cil.oc.common.tileentity.traits

import cpw.mods.fml.common.Optional
import cpw.mods.fml.relauncher.{Side, SideOnly}
import li.cil.oc.api.network
import li.cil.oc.api.network.ManagedEnvironment
import li.cil.oc.server.{component, PacketSender => ServerPacketSender}
import li.cil.oc.util.mods.{Mods, StargateTech2}
import net.minecraft.nbt.NBTTagCompound
import stargatetech2.api.StargateTechAPI
import stargatetech2.api.bus.{IBusDevice, IBusInterface}

// IMPORTANT: for some reason that is beyond me we cannot implement the
// IBusDevice here directly, since we'll get an error if the interface is not
// provided (i.e. if SGT2 isn't installed), even if we tell FML to strip it.
// Assuming FML properly strips the interface (and it looks like it does, when
// inspecting it in the debugger, i.e. getInterfaces() doesn't contain it), it
// probably is something derping up in the class loader... the thing that
// confuses me the most, though, is that it apparently works for redstone and
// the CC interface, so... yeah. I'm out of ideas.
trait AbstractBusAware extends TileEntity with network.Environment {
  self: IBusDevice =>
  protected var _isAbstractBusAvailable: Boolean = _

  protected lazy val fakeInterface = Array[AnyRef](StargateTechAPI.api.getFactory.getIBusInterface(this, null))

  def installedComponents: Iterable[ManagedEnvironment]

  @Optional.Method(modid = Mods.IDs.StargateTech2)
  def getInterfaces(side: Int): Array[IBusInterface] =
    if (isAbstractBusAvailable) {
      if (isServer) {
        installedComponents.collect {
          case abstractBus: component.AbstractBusCard => abstractBus.busInterface
        }.toArray
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
      if (isServer && Mods.StargateTech2.isAvailable) {
        if (isAbstractBusAvailable) StargateTech2.addDevice(world, x, y, z)
        else StargateTech2.removeDevice(world, x, y, z)
      }
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

  abstract override def onDisconnect(node: network.Node) {
    super.onDisconnect(node)
    if (node == this.node) {
      isAbstractBusAvailable = false
    }
  }
}
