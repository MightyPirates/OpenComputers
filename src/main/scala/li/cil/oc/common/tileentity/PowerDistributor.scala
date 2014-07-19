package li.cil.oc.common.tileentity

import cpw.mods.fml.relauncher.{Side, SideOnly}
import li.cil.oc.api.network._
import li.cil.oc.util.ExtendedNBT._
import li.cil.oc.{Settings, api}
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.common.util.Constants.NBT
import net.minecraftforge.common.util.ForgeDirection

class PowerDistributor extends traits.Environment with traits.PowerBalancer with traits.NotAnalyzable {
  val node = null

  private val nodes = Array.fill(6)(api.Network.newNode(this, Visibility.Network).
    withConnector(Settings.get.bufferDistributor).
    create())

  override protected def isConnected = nodes.exists(node => node.address != null && node.network != null)

  override def canUpdate = isServer

  // ----------------------------------------------------------------------- //

  @SideOnly(Side.CLIENT)
  override def canConnect(side: ForgeDirection) = true

  override def sidedNode(side: ForgeDirection) = nodes(side.ordinal)

  // ----------------------------------------------------------------------- //

  override def readFromNBT(nbt: NBTTagCompound) {
    super.readFromNBT(nbt)
    nbt.getTagList(Settings.namespace + "connector", NBT.TAG_COMPOUND).foreach {
      case (list, index) => nodes(index).load(list.getCompoundTagAt(index))
    }
  }

  override def writeToNBT(nbt: NBTTagCompound) {
    super.writeToNBT(nbt)
    // Side check for Waila (and other mods that may call this client side).
    if (isServer) {
      nbt.setNewTagList(Settings.namespace + "connector", nodes.map(connector => {
        val connectorNbt = new NBTTagCompound()
        connector.save(connectorNbt)
        connectorNbt
      }))
    }
  }
}
