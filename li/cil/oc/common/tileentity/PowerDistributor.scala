package li.cil.oc.common.tileentity

import li.cil.oc.Config
import li.cil.oc.api.network._
import li.cil.oc.client.{PacketSender => ClientPacketSender}
import li.cil.oc.server.component
import li.cil.oc.util.ExtendedNBT._
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.nbt.NBTTagCompound

class PowerDistributor extends Environment with PowerInformation with Analyzable {
  val distributor = new component.PowerDistributor(this)

  def node = distributor.node

  // ----------------------------------------------------------------------- //

  override def onAnalyze(stats: NBTTagCompound, player: EntityPlayer, side: Int, hitX: Float, hitY: Float, hitZ: Float) = {
    stats.setString(Config.namespace + "text.Analyzer.TotalEnergy", "%.2f/%.2f".format(distributor.globalBuffer, distributor.globalBufferSize))
    this
  }

  // ----------------------------------------------------------------------- //

  override def readFromNBT(nbt: NBTTagCompound) {
    super.readFromNBT(nbt)

    distributor.load(nbt.getCompoundTag(Config.namespace + "distributor"))
  }

  override def writeToNBT(nbt: NBTTagCompound) {
    super.writeToNBT(nbt)

    nbt.setNewCompoundTag(Config.namespace + "distributor", distributor.save)
  }

  // ----------------------------------------------------------------------- //

  override def updateEntity() {
    super.updateEntity()
    distributor.update()
  }

  override def validate() {
    super.validate()
    if (isClient) {
      ClientPacketSender.sendPowerStateRequest(this)
    }
  }
}
