package li.cil.oc.common.block

import li.cil.oc.Settings
import li.cil.oc.common.GuiType
import li.cil.oc.common.tileentity
import net.minecraft.world.World

class Relay extends SimpleBlock with traits.GUI with traits.PowerAcceptor {
  override def guiType = GuiType.Relay

  override def energyThroughput = Settings.get.accessPointRate

  override def createNewTileEntity(world: World, metadata: Int) = new tileentity.Relay()
}
