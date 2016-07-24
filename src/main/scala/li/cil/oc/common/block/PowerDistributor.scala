package li.cil.oc.common.block

import li.cil.oc.common.tileentity
import li.cil.oc.integration.coloredlights.ModColoredLights
import net.minecraft.world.World

class PowerDistributor extends SimpleBlock {
  ModColoredLights.setLightLevel(this, 5, 5, 3)

  // ----------------------------------------------------------------------- //

  override def createNewTileEntity(world: World, metadata: Int) = new tileentity.PowerDistributor()
}

