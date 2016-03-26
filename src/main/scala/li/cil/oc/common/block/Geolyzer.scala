package li.cil.oc.common.block

import li.cil.oc.common.tileentity
import li.cil.oc.integration.coloredlights.ModColoredLights
import net.minecraft.block.state.IBlockState
import net.minecraft.world.World

class Geolyzer extends SimpleBlock {
  ModColoredLights.setLightLevel(this, 3, 1, 1)

  // ----------------------------------------------------------------------- //

  override def createNewTileEntity(world: World, metadata: Int) = new tileentity.Geolyzer()
}
