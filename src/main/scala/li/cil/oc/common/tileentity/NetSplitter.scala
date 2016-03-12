package li.cil.oc.common.tileentity

import cpw.mods.fml.relauncher.Side
import cpw.mods.fml.relauncher.SideOnly
import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.api.network.Visibility
import li.cil.oc.common.EventHandler
import li.cil.oc.server.{PacketSender => ServerPacketSender}
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.common.util.ForgeDirection

class NetSplitter extends traits.Environment with traits.RedstoneAware with api.network.SidedEnvironment {
  private final val SideCount = ForgeDirection.VALID_DIRECTIONS.length

  _isOutputEnabled = true

  val node = api.Network.newNode(this, Visibility.None).
    create()

  var isInverted = false

  var openSides = Array.fill(SideCount)(false)

  def compressSides = (ForgeDirection.VALID_DIRECTIONS, openSides).zipped.foldLeft(0)((acc, entry) => acc | (if (entry._2) entry._1.flag else 0)).toByte

  def uncompressSides(byte: Byte) = ForgeDirection.VALID_DIRECTIONS.map(d => (d.flag & byte) != 0)

  def isSideOpen(side: ForgeDirection) = side != ForgeDirection.UNKNOWN && {
    val isOpen = openSides(side.ordinal())
    if (isInverted) !isOpen else isOpen
  }

  def setSideOpen(side: ForgeDirection, value: Boolean): Unit = if (side != ForgeDirection.UNKNOWN && openSides(side.ordinal()) != value) {
    openSides(side.ordinal()) = value
    if (isServer) {
      node.remove()
      api.Network.joinOrCreateNetwork(this)
      ServerPacketSender.sendNetSplitterState(this)
      world.playSoundEffect(x + 0.5, y + 0.5, z + 0.5, "tile.piston.out", 0.5f, world.rand.nextFloat() * 0.25f + 0.7f)
      world.notifyBlocksOfNeighborChange(x, y, z, block)
    }
    else {
      world.markBlockForUpdate(x, y, z)
    }
  }

  // ----------------------------------------------------------------------- //

  override def sidedNode(side: ForgeDirection) = if (isSideOpen(side)) node else null

  @SideOnly(Side.CLIENT)
  override def canConnect(side: ForgeDirection) = isSideOpen(side)

  // ----------------------------------------------------------------------- //

  override def canUpdate = false

  override protected def initialize(): Unit = {
    super.initialize()
    EventHandler.scheduleServer(this)
  }

  // ----------------------------------------------------------------------- //

  override protected def onRedstoneInputChanged(side: ForgeDirection, oldMaxValue: Int, newMaxValue: Int): Unit = {
    super.onRedstoneInputChanged(side, oldMaxValue, newMaxValue)
    val oldIsInverted = isInverted
    isInverted = newMaxValue > 0
    if (isInverted != oldIsInverted) {
      if (isServer) {
        node.remove()
        api.Network.joinOrCreateNetwork(this)
        ServerPacketSender.sendNetSplitterState(this)
        world.playSoundEffect(x + 0.5, y + 0.5, z + 0.5, "tile.piston.in", 0.5f, world.rand.nextFloat() * 0.25f + 0.7f)
      }
      else {
        world.markBlockForUpdate(x, y, z)
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
