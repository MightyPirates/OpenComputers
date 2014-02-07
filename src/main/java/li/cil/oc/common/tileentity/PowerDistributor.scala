package li.cil.oc.common.tileentity

import cpw.mods.fml.relauncher.{Side, SideOnly}
import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.api.network._
import li.cil.oc.util.ExtendedNBT._
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.common.ForgeDirection

class PowerDistributor extends Environment with PowerBalancer with Analyzable {
  val node = null

  private val nodes = Array.fill(6)(api.Network.newNode(this, Visibility.Network).
    withConnector(Settings.get.bufferDistributor).
    create())

  @SideOnly(Side.CLIENT)
  override def canConnect(side: ForgeDirection) = true

  override def sidedNode(side: ForgeDirection) = nodes(side.ordinal)

  // ----------------------------------------------------------------------- //

  override def onAnalyze(player: EntityPlayer, side: Int, hitX: Float, hitY: Float, hitZ: Float) = null

  // ----------------------------------------------------------------------- //

  override def readFromNBT(nbt: NBTTagCompound) {
    super.readFromNBT(nbt)
    nbt.getTagList(Settings.namespace + "connector").iterator[NBTTagCompound].zip(nodes).foreach {
      case (connectorNbt, connector) => connector.load(connectorNbt)
    }
  }

  override def writeToNBT(nbt: NBTTagCompound) {
    super.writeToNBT(nbt)
    nbt.setNewTagList(Settings.namespace + "connector", nodes.map(connector => {
      val connectorNbt = new NBTTagCompound()
      connector.save(connectorNbt)
      connectorNbt
    }))
  }
}
