package li.cil.oc.common.tileentity

import li.cil.oc.server.component
import net.minecraft.nbt.CompoundNBT
import net.minecraft.tileentity.TileEntity

class Transposer extends TileEntity(null) with traits.Environment {
  val transposer = new component.Transposer.Block(this)

  def node = transposer.node

  // Used on client side to check whether to render activity indicators.
  var lastOperation = 0L

  override def loadForServer(nbt: CompoundNBT) {
    super.loadForServer(nbt)
    transposer.loadData(nbt)
  }

  override def saveForServer(nbt: CompoundNBT) {
    super.saveForServer(nbt)
    transposer.saveData(nbt)
  }
}
