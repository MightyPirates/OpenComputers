package li.cil.oc.common.tileentity

import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.api.network._
import li.cil.oc.util.ExtendedNBT._
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.EnumFacing
import net.minecraftforge.common.util.Constants.NBT
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly

class PowerDistributor extends traits.Environment with traits.PowerBalancer with traits.NotAnalyzable {
  val node = null

  private val nodes = Array.fill(6)(api.Network.newNode(this, Visibility.None).
    withConnector(Settings.get.bufferDistributor).
    create())

  override protected def isConnected: Boolean = nodes.exists(node => node.address != null && node.network != null)

  // ----------------------------------------------------------------------- //

  @SideOnly(Side.CLIENT)
  override def canConnect(side: EnumFacing) = true

  override def sidedNode(side: EnumFacing): Connector = nodes(side.ordinal)

  // ----------------------------------------------------------------------- //

  private final val ConnectorTag = Settings.namespace + "connector"

  override def readFromNBTForServer(nbt: NBTTagCompound) {
    super.readFromNBTForServer(nbt)
    nbt.getTagList(ConnectorTag, NBT.TAG_COMPOUND).toArray[NBTTagCompound].
      zipWithIndex.foreach {
      case (tag, index) => nodes(index).load(tag)
    }
  }

  override def writeToNBTForServer(nbt: NBTTagCompound) {
    super.writeToNBTForServer(nbt)
    // Side check for Waila (and other mods that may call this client side).
    if (isServer) {
      nbt.setNewTagList(ConnectorTag, nodes.map(connector => {
        val connectorNbt = new NBTTagCompound()
        connector.save(connectorNbt)
        connectorNbt
      }))
    }
  }
}
