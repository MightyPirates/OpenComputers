package li.cil.oc.common.tileentity.traits

import cpw.mods.fml.common.Optional
import cpw.mods.fml.relauncher.Side
import cpw.mods.fml.relauncher.SideOnly
import li.cil.oc.api.network
import li.cil.oc.api.network.ManagedEnvironment
import li.cil.oc.common.asm.Injectable
import li.cil.oc.integration.Mods
import li.cil.oc.integration.stargatetech2.AbstractBusCard
import li.cil.oc.integration.util.StargateTech2
import li.cil.oc.server.component
import li.cil.oc.server.{PacketSender => ServerPacketSender}
import lordfokas.stargatetech2.api.StargateTechAPI
import lordfokas.stargatetech2.api.bus.IBusDevice
import lordfokas.stargatetech2.api.bus.IBusInterface
import net.minecraft.nbt.NBTTagCompound

@Injectable.Interface(value = "lordfokas.stargatetech2.api.bus.IBusDevice", modid = Mods.IDs.StargateTech2)
trait AbstractBusAware extends TileEntity with network.Environment {
  protected var _isAbstractBusAvailable: Boolean = _

  protected lazy val fakeInterface = Array[AnyRef](StargateTechAPI.api.getFactory.getIBusInterface(this.asInstanceOf[IBusDevice], null))

  def installedComponents: Iterable[ManagedEnvironment]

  @Optional.Method(modid = Mods.IDs.StargateTech2)
  def getInterfaces(side: Int): Array[IBusInterface] =
    if (isAbstractBusAvailable) {
      if (isServer) {
        installedComponents.collect {
          case abstractBus: AbstractBusCard => abstractBus.busInterface
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
      world.notifyBlocksOfNeighborChange(x, y, z, block)
      if (isServer) ServerPacketSender.sendAbstractBusState(this)
      else world.markBlockForUpdate(x, y, z)
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
