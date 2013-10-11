package li.cil.oc.common.block

import cpw.mods.fml.common.registry.GameRegistry
import li.cil.oc.common.tileentity
import net.minecraft.world.{IBlockAccess, World}
import net.minecraft.util.Icon
import net.minecraftforge.common.ForgeDirection
import net.minecraft.client.renderer.texture.IconRegister
import li.cil.oc.Config

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

  private val icons = Array.fill[Icon](6)(null)

  override def icon(side: ForgeDirection) = Some(icons(side.ordinal))

  override def registerIcons(iconRegister: IconRegister) = {
    icons(ForgeDirection.DOWN.ordinal) = iconRegister.registerIcon(Config.resourceDomain + ":computer_top")
    icons(ForgeDirection.UP.ordinal) = icons(ForgeDirection.DOWN.ordinal)

    icons(ForgeDirection.NORTH.ordinal) = iconRegister.registerIcon(Config.resourceDomain + ":power_converter")
    icons(ForgeDirection.SOUTH.ordinal) = icons(ForgeDirection.NORTH.ordinal)
    icons(ForgeDirection.WEST.ordinal) = icons(ForgeDirection.NORTH.ordinal)
    icons(ForgeDirection.EAST.ordinal) = icons(ForgeDirection.NORTH.ordinal)
  }

  // ----------------------------------------------------------------------- //
  // Tile entity
  // ----------------------------------------------------------------------- //

  override def hasTileEntity = true

  override def createTileEntity(world: World, metadata: Int) = Some(new tileentity.PowerSupply)

  // ----------------------------------------------------------------------- //

  override def getValidRotations(world: World, x: Int, y: Int, z: Int) = validRotations

  /** Avoid creating new arrays. */
  private val validRotations = Array(
    ForgeDirection.SOUTH,
    ForgeDirection.WEST,
    ForgeDirection.NORTH,
    ForgeDirection.EAST)
}
