package li.cil.oc.common.tileentity

import li.cil.oc.server.component
import net.minecraft.nbt.CompoundNBT
import net.minecraft.tileentity.TileEntity
import net.minecraft.tileentity.TileEntityType

class Geolyzer(selfType: TileEntityType[_ <: Geolyzer]) extends TileEntity(selfType) with traits.Environment {
  val geolyzer = new component.Geolyzer(this)

  def node = geolyzer.node

  override def loadForServer(nbt: CompoundNBT) {
    super.loadForServer(nbt)
    geolyzer.loadData(nbt)
  }

  override def saveForServer(nbt: CompoundNBT) {
    super.saveForServer(nbt)
    geolyzer.saveData(nbt)
  }
}
