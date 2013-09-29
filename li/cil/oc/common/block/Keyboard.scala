package li.cil.oc.common.block

import cpw.mods.fml.common.registry.GameRegistry
import li.cil.oc.common.tileentity
import net.minecraft.world.World
import net.minecraftforge.common.ForgeDirection

class Keyboard(val parent: Delegator) extends Delegate {
  GameRegistry.registerTileEntity(classOf[tileentity.Keyboard], "oc.keyboard")

  val unlocalizedName = "Keyboard"

  // ----------------------------------------------------------------------- //
  // Tile entity
  // ----------------------------------------------------------------------- //

  override def hasTileEntity = true

  override def createTileEntity(world: World, metadata: Int) = Some(new tileentity.Keyboard)

  // ----------------------------------------------------------------------- //
  // Block rotation
  // ----------------------------------------------------------------------- //

  override def getValidRotations(world: World, x: Int, y: Int, z: Int) = validRotations

  /** Avoid creating new arrays at the cost of this possibly getting modified. */
  private val validRotations = Array(
    ForgeDirection.SOUTH,
    ForgeDirection.WEST,
    ForgeDirection.NORTH,
    ForgeDirection.EAST)
}