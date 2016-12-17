package li.cil.oc.common.tileentity

import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.api.network.Visibility
import li.cil.oc.common.EventHandler
import li.cil.oc.server.{PacketSender => ServerPacketSender}
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.EnumFacing
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly

class NetSplitter extends traits.Environment with traits.OpenSides with traits.RedstoneAware with api.network.SidedEnvironment {
  _isOutputEnabled = true

  val node = api.Network.newNode(this, Visibility.None).
    create()

  var isInverted = false

  override def isSideOpen(side: EnumFacing) = if (isInverted) !super.isSideOpen(side) else super.isSideOpen(side)

  override def setSideOpen(side: EnumFacing, value: Boolean) {
    super.setSideOpen(side, value)
    if (isServer) {
      node.remove()
      api.Network.joinOrCreateNetwork(this)
      ServerPacketSender.sendNetSplitterState(this)
      world.playSoundEffect(x + 0.5, y + 0.5, z + 0.5, "tile.piston.out", 0.5f, world.rand.nextFloat() * 0.25f + 0.7f)
      world.notifyNeighborsOfStateChange(getPos, getBlockType)
    }
    else {
      world.markBlockForUpdate(getPos)
    }
  }

  // ----------------------------------------------------------------------- //

  override def sidedNode(side: EnumFacing) = if (isSideOpen(side)) node else null

  @SideOnly(Side.CLIENT)
  override def canConnect(side: EnumFacing) = isSideOpen(side)

  // ----------------------------------------------------------------------- //

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
        world.playSoundEffect(x + 0.5, y + 0.5, z + 0.5, "tile.piston.in", 0.5f, world.rand.nextFloat() * 0.25f + 0.7f)
      }
      else {
        world.markBlockForUpdate(getPos)
      }
    }
  }

  override def readFromNBTForServer(nbt: NBTTagCompound): Unit = {
    super.readFromNBTForServer(nbt)
    isInverted = nbt.getBoolean(Settings.namespace + "isInverted")
  }

  override def writeToNBTForServer(nbt: NBTTagCompound): Unit = {
    super.writeToNBTForServer(nbt)
    nbt.setBoolean(Settings.namespace + "isInverted", isInverted)
  }

  @SideOnly(Side.CLIENT) override
  def readFromNBTForClient(nbt: NBTTagCompound): Unit = {
    super.readFromNBTForClient(nbt)
    isInverted = nbt.getBoolean(Settings.namespace + "isInverted")
  }

  override def writeToNBTForClient(nbt: NBTTagCompound): Unit = {
    super.writeToNBTForClient(nbt)
    nbt.setBoolean(Settings.namespace + "isInverted", isInverted)
  }
}
