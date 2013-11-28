package li.cil.oc.common.tileentity

import li.cil.oc.Settings
import li.cil.oc.api.network._
import li.cil.oc.server.component
import li.cil.oc.util.ExtendedNBT._
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.nbt.NBTTagCompound

class PowerDistributor extends Environment with PowerInformation with Analyzable {
  val distributor = new component.PowerDistributor(this)

  var globalBuffer = 0.0

  var globalBufferSize = 0.0

  def node = distributor.node

  // ----------------------------------------------------------------------- //

  override def onAnalyze(stats: NBTTagCompound, player: EntityPlayer, side: Int, hitX: Float, hitY: Float, hitZ: Float) = {
    stats.setString(Settings.namespace + "text.Analyzer.TotalEnergy", "%.2f/%.2f".format(distributor.globalBuffer, distributor.globalBufferSize))
    node
  }

  // ----------------------------------------------------------------------- //

  override def readFromNBT(nbt: NBTTagCompound) {
    super.readFromNBT(nbt)

    distributor.load(nbt.getCompoundTag(Settings.namespace + "distributor"))
  }

  override def writeToNBT(nbt: NBTTagCompound) {
    super.writeToNBT(nbt)

    nbt.setNewCompoundTag(Settings.namespace + "distributor", distributor.save)
  }

  // ----------------------------------------------------------------------- //

  override def updateEntity() {
    super.updateEntity()
    distributor.update()
  }
}
