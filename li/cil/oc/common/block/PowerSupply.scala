package li.cil.oc.common.block

import cpw.mods.fml.common.registry.GameRegistry
import li.cil.oc.common.tileentity
import net.minecraft.world.World

/**
 * Created with IntelliJ IDEA.
 * User: lordjoda
 * Date: 30.09.13
 * Time: 20:36
 * To change this template use File | Settings | File Templates.
 */
class PowerSupply(val parent: Delegator) extends Delegate {
  GameRegistry.registerTileEntity(classOf[tileentity.PowerSupply], "oc.powersupply" )
  val unlocalizedName = "PowerSupply"

  override def breakBlock(world: World, x: Int, y: Int, z: Int, blockId: Int, metadata: Int) = {
    world.getBlockTileEntity(x, y, z).asInstanceOf[tileentity.PowerSupply].onUnload()
    super.breakBlock(world, x, y, z, blockId, metadata)
  }
  // ----------------------------------------------------------------------- //
  // Tile entity
  // ----------------------------------------------------------------------- //

  override def hasTileEntity = true

  override def createTileEntity(world: World, metadata: Int) = Some(new tileentity.PowerSupply)
}
