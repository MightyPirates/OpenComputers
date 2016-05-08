package li.cil.oc.common.tileentity

import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.api.network.Visibility
import li.cil.oc.common.EventHandler
import li.cil.oc.server.{PacketSender => ServerPacketSender}
import net.minecraft.init.SoundEvents
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.EnumFacing
import net.minecraft.util.SoundCategory
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly

class NetSplitter extends traits.Environment with traits.RedstoneAware with api.network.SidedEnvironment {
  private final val SideCount = EnumFacing.values().length

  _isOutputEnabled = true

  val node = api.Network.newNode(this, Visibility.None).
    create()

  var isInverted = false

  var openSides = Array.fill(SideCount)(false)

  def compressSides = (EnumFacing.values(), openSides).zipped.foldLeft(0)((acc, entry) => acc | (if (entry._2) 1 << entry._1.ordinal() else 0)).toByte

  def uncompressSides(byte: Byte) = EnumFacing.values().map(d => ((1 << d.ordinal()) & byte) != 0)

  def isSideOpen(side: EnumFacing) = side != null && {
    val isOpen = openSides(side.ordinal())
    if (isInverted) !isOpen else isOpen
  }

  def setSideOpen(side: EnumFacing, value: Boolean): Unit = if (side != null && openSides(side.ordinal()) != value) {
    openSides(side.ordinal()) = value
    if (isServer) {
      node.remove()
      api.Network.joinOrCreateNetwork(this)
      ServerPacketSender.sendNetSplitterState(this)
      world.playSound(x + 0.5, y + 0.5, z + 0.5, SoundEvents.BLOCK_PISTON_EXTEND, SoundCategory.BLOCKS, 0.5f, world.rand.nextFloat() * 0.25f + 0.7f, false)
      world.notifyNeighborsOfStateChange(getPos, getBlockType)
    }
    else {
      world.notifyBlockUpdate(getPos, world.getBlockState(getPos), world.getBlockState(getPos), 3)
    }
  }

  // ----------------------------------------------------------------------- //

  override def sidedNode(side: EnumFacing) = if (isSideOpen(side)) node else null

  @SideOnly(Side.CLIENT)
  override def canConnect(side: EnumFacing) = isSideOpen(side)

  // ----------------------------------------------------------------------- //

  override def canUpdate = false

  override protected def initialize(): Unit = {
    super.initialize()
    EventHandler.scheduleServer(this)
  }

  // ----------------------------------------------------------------------- //

  override protected def onRedstoneInputChanged(side: EnumFacing, oldMaxValue: Int, newMaxValue: Int): Unit = {
    super.onRedstoneInputChanged(side, oldMaxValue, newMaxValue)
    val oldIsInverted = isInverted
    isInverted = newMaxValue > 0
    if (isInverted != oldIsInverted) {
      if (isServer) {
        node.remove()
        api.Network.joinOrCreateNetwork(this)
        ServerPacketSender.sendNetSplitterState(this)
        world.playSound(x + 0.5, y + 0.5, z + 0.5, SoundEvents.BLOCK_PISTON_CONTRACT, SoundCategory.BLOCKS, 0.5f, world.rand.nextFloat() * 0.25f + 0.7f, false)
      }
      else {
        world.notifyBlockUpdate(getPos, getWorld.getBlockState(getPos), getWorld.getBlockState(getPos), 3)
      }
    }
  }

  override def readFromNBTForServer(nbt: NBTTagCompound): Unit = {
    super.readFromNBTForServer(nbt)
    isInverted = nbt.getBoolean(Settings.namespace + "isInverted")
    openSides = uncompressSides(nbt.getByte(Settings.namespace + "openSides"))
  }

  override def writeToNBTForServer(nbt: NBTTagCompound): Unit = {
    super.writeToNBTForServer(nbt)
    nbt.setBoolean(Settings.namespace + "isInverted", isInverted)
    nbt.setByte(Settings.namespace + "openSides", compressSides)
  }

  @SideOnly(Side.CLIENT) override
  def readFromNBTForClient(nbt: NBTTagCompound): Unit = {
    super.readFromNBTForClient(nbt)
    isInverted = nbt.getBoolean(Settings.namespace + "isInverted")
    openSides = uncompressSides(nbt.getByte(Settings.namespace + "openSides"))
  }

  override def writeToNBTForClient(nbt: NBTTagCompound): Unit = {
    super.writeToNBTForClient(nbt)
    nbt.setBoolean(Settings.namespace + "isInverted", isInverted)
    nbt.setByte(Settings.namespace + "openSides", compressSides)
  }
}
