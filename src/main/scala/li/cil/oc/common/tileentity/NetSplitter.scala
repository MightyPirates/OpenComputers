package li.cil.oc.common.tileentity

import cpw.mods.fml.relauncher.{Side, SideOnly}
import li.cil.oc.{Settings, api}
import li.cil.oc.api.network.Visibility
import li.cil.oc.common.EventHandler
import li.cil.oc.common.tileentity.traits.RedstoneChangedEventArgs
import li.cil.oc.server.{PacketSender => ServerPacketSender}
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.common.util.ForgeDirection

class NetSplitter extends traits.Environment with traits.OpenSides with traits.RedstoneAware with api.network.SidedEnvironment {

  _isOutputEnabled = true

  val node = api.Network.newNode(this, Visibility.None).
    create()

  var isInverted = false

  override def isSideOpen(side: ForgeDirection) =  if (isInverted) !super.isSideOpen(side) else super.isSideOpen(side)

  override def setSideOpen(side: ForgeDirection, value: Boolean) {
    super.setSideOpen(side, value)
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

  override protected def onRedstoneInputChanged(args: RedstoneChangedEventArgs): Unit = {
    super.onRedstoneInputChanged(args)
    val oldIsInverted = isInverted
    isInverted = args.newValue > 0
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
