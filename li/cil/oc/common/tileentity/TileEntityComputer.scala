package li.cil.oc.common.tileentity

import java.util.concurrent.atomic.AtomicBoolean

import li.cil.oc.server.computer.IComputerEnvironment
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.tileentity.TileEntity

class TileEntityComputer(isClient: Boolean) extends TileEntity with IComputerEnvironment with ItemComponentProxy with BlockComponentProxy {
  def this() = this(false)

  protected val computer =
    if (isClient) new li.cil.oc.client.computer.Computer(this)
    else new li.cil.oc.server.computer.Computer(this)

  private val hasChanged = new AtomicBoolean()

  // ----------------------------------------------------------------------- //
  // General
  // ----------------------------------------------------------------------- //

  def turnOn() = computer.start()

  def turnOff() = computer.stop()

  override def readFromNBT(nbt: NBTTagCompound) = {
    super.readFromNBT(nbt)
    computer.readFromNBT(nbt.getCompoundTag("computer"))
    readBlocksFromNBT(nbt.getCompoundTag("blocks"))
    readItemsFromNBT(nbt.getCompoundTag("items"))
  }

  override def writeToNBT(nbt: NBTTagCompound) = {
    super.writeToNBT(nbt)

    val computerNbt = new NBTTagCompound
    computer.writeToNBT(computerNbt)
    nbt.setCompoundTag("computer", computerNbt)

    val blocksNbt = new NBTTagCompound
    writeBlocksToNBT(blocksNbt)
    nbt.setCompoundTag("blocks", blocksNbt)

    val itemsNbt = new NBTTagCompound
    writeItemsToNBT(itemsNbt)
    nbt.setCompoundTag("items", itemsNbt)
  }

  override def updateEntity() = {
    computer.update()
    if (hasChanged.get)
      worldObj.updateTileEntityChunkAndDoNothing(
        xCoord, yCoord, zCoord, this)
  }

  // ----------------------------------------------------------------------- //
  // Interfaces and updating
  // ----------------------------------------------------------------------- //

  def onNeighborBlockChange(blockId: Int) =
    (0 to 5).foreach(checkBlockChanged(_))

  def isUseableByPlayer(entityplayer: EntityPlayer) =
    world.getBlockTileEntity(xCoord, yCoord, zCoord) == this &&
      entityplayer.getDistanceSq(xCoord + 0.5, yCoord + 0.5, zCoord + 0.5) < 64

  def world = worldObj

  def coordinates = (xCoord, yCoord, zCoord)

  def driver(id: Int) = itemDriver(id).orElse(blockDriver(id))

  def component(id: Int) = itemComponent(id).orElse(blockComponent(id))

  def markAsChanged() = hasChanged.set(true)
}