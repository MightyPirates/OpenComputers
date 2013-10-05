package li.cil.oc.common.block

import cpw.mods.fml.common.registry.GameRegistry
import li.cil.oc.common.tileentity
import net.minecraft.world.World

/**
 * Created with IntelliJ IDEA.
 * User: lordjoda
 * Date: 03.10.13
 * Time: 19:48
 * To change this template use File | Settings | File Templates.
 */
class PowerDistributer (val parent: Delegator) extends Delegate {
  GameRegistry.registerTileEntity(classOf[tileentity.PowerDistributer], "oc.powerdistributer" )
  val unlocalizedName = "PowerDistributer"

  override def breakBlock(world: World, x: Int, y: Int, z: Int, blockId: Int, metadata: Int) = {
    //world.getBlockTileEntity(x, y, z).asInstanceOf[tileentity.PowerDistributer]
    super.breakBlock(world, x, y, z, blockId, metadata)
  }
  // ----------------------------------------------------------------------- //
  // Tile entity
  // ----------------------------------------------------------------------- //

  override def hasTileEntity = true

  override def createTileEntity(world: World, metadata: Int) = Some(new tileentity.PowerDistributer)
}

