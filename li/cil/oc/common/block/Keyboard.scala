package li.cil.oc.common.block

import cpw.mods.fml.common.registry.GameRegistry
import li.cil.oc.common.tileentity
import net.minecraft.world.World

class Keyboard(val parent: Delegator) extends Delegate {
  GameRegistry.registerTileEntity(classOf[tileentity.Keyboard], "oc.keyboard")

  val unlocalizedName = "Keyboard"

  // ----------------------------------------------------------------------- //
  // Tile entity
  // ----------------------------------------------------------------------- //

  override def hasTileEntity = true

  override def createTileEntity(world: World, metadata: Int) = Some(new tileentity.Keyboard)
}