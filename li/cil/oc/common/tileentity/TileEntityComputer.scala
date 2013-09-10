package li.cil.oc.common.tileentity

import java.util.concurrent.atomic.AtomicBoolean

import li.cil.oc.server.computer.IComputerEnvironment
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.tileentity.TileEntity
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.ForgeSubscribe
import net.minecraftforge.event.world.ChunkEvent
import net.minecraftforge.event.world.WorldEvent

class TileEntityComputer(isClient: Boolean) extends TileEntity with IComputerEnvironment {
  def this() = this(false)
  MinecraftForge.EVENT_BUS.register(this)

  private val hasChanged = new AtomicBoolean()

  // ----------------------------------------------------------------------- //
  // General
  // ----------------------------------------------------------------------- //

  private val computer =
    if (isClient) new li.cil.oc.client.computer.Computer(this)
    else new li.cil.oc.server.computer.Computer(this)

  def turnOn() = computer.start()

  def turnOff() = computer.stop()

  override def readFromNBT(nbt: NBTTagCompound) = {
    super.readFromNBT(nbt)
    computer.readFromNBT(nbt)
  }

  override def writeToNBT(nbt: NBTTagCompound) = {
    super.writeToNBT(nbt)
    computer.writeToNBT(nbt)
  }

  override def updateEntity() = {
    computer.update()
    if (hasChanged.get())
      worldObj.updateTileEntityChunkAndDoNothing(
        this.xCoord, this.yCoord, this.zCoord, this)
  }

  // ----------------------------------------------------------------------- //
  // Event Bus
  // ----------------------------------------------------------------------- //

  @ForgeSubscribe
  def onChunkUnload(e: ChunkEvent.Unload) = {
    MinecraftForge.EVENT_BUS.unregister(this)
    computer.stop()
  }

  @ForgeSubscribe
  def onWorldUnload(e: WorldEvent.Unload) = {
    MinecraftForge.EVENT_BUS.unregister(this)
    computer.stop()
  }

  // ----------------------------------------------------------------------- //
  // IComputerEnvironment
  // ----------------------------------------------------------------------- //

  def world = worldObj

  def markAsChanged() = hasChanged.set(true)
}