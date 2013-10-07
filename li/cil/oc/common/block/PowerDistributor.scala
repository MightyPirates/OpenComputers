package li.cil.oc.common.block

import cpw.mods.fml.common.registry.GameRegistry
import li.cil.oc.common.tileentity
import net.minecraft.world.World


class PowerDistributor (val parent: Delegator) extends Delegate {
  GameRegistry.registerTileEntity(classOf[tileentity.PowerDistributor], "oc.powerdistributor" )
  val unlocalizedName = "PowerDistributor"

  override def breakBlock(world: World, x: Int, y: Int, z: Int, blockId: Int, metadata: Int) = {
    //world.getBlockTileEntity(x, y, z).asInstanceOf[tileentity.PowerDistributer]
    super.breakBlock(world, x, y, z, blockId, metadata)
  }
  // ----------------------------------------------------------------------- //
  // Tile entity
  // ----------------------------------------------------------------------- //

  override def hasTileEntity = true

  override def createTileEntity(world: World, metadata: Int) = Some(new tileentity.PowerDistributor)
}

