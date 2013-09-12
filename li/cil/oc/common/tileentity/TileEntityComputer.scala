package li.cil.oc.common.tileentity

import java.util.concurrent.atomic.AtomicBoolean

import li.cil.oc.server.computer.IComputerEnvironment
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.nbt.NBTTagList
import net.minecraft.tileentity.TileEntity
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.ForgeSubscribe
import net.minecraftforge.event.world.ChunkEvent
import net.minecraftforge.event.world.WorldEvent

class TileEntityComputer(isClient: Boolean) extends TileEntity with IComputerEnvironment with ItemComponentProxy with BlockComponentProxy {
  def this() = this(false)
  MinecraftForge.EVENT_BUS.register(this)

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
    computer.readFromNBT(nbt)

    val itemList = nbt.getTagList("inventory");
    for (i <- 0 until itemList.tagCount()) {
      val tag = itemList.tagAt(i).asInstanceOf[NBTTagCompound];
      val slot = tag.getByte("slot");
      if (slot >= 0 && slot < inventory.length) {
        inventory(slot) = ItemStack.loadItemStackFromNBT(tag);
      }
    }
  }

  override def writeToNBT(nbt: NBTTagCompound) = {
    super.writeToNBT(nbt)
    computer.writeToNBT(nbt)

    val itemList = new NBTTagList();
    for (i <- 0 until inventory.length) {
      val stack = inventory(i);
      if (stack != null) {
        val tag = new NBTTagCompound();
        tag.setByte("slot", i.toByte);
        stack.writeToNBT(tag);
        itemList.appendTag(tag);
      }
    }
    nbt.setTag("inventory", itemList);
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
  // Interfaces and updating
  // ----------------------------------------------------------------------- //

  def onNeighborBlockChange(blockId: Int) =
    (0 to 5).foreach(checkBlockChanged(xCoord, yCoord, zCoord, _))

  def isUseableByPlayer(entityplayer: EntityPlayer) =
    world.getBlockTileEntity(xCoord, yCoord, zCoord) == this &&
      entityplayer.getDistanceSq(xCoord + 0.5, yCoord + 0.5, zCoord + 0.5) < 64

  def world = worldObj

  def markAsChanged() = hasChanged.set(true)
}