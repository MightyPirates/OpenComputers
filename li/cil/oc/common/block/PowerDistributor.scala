package li.cil.oc.common.block

import li.cil.oc.Config
import li.cil.oc.common.tileentity
import net.minecraft.client.renderer.texture.IconRegister
import net.minecraft.util.Icon
import net.minecraft.world.{IBlockAccess, World}
import net.minecraftforge.common.ForgeDirection

class PowerDistributor(val parent: SimpleDelegator) extends SimpleDelegate {
  val unlocalizedName = "PowerDistributor"

  // ----------------------------------------------------------------------- //

  private val icons = Array.fill[Icon](6)(null)

  override def icon(side: ForgeDirection) = Some(icons(side.ordinal()))

  override def registerIcons(iconRegister: IconRegister) = {
    icons(ForgeDirection.DOWN.ordinal) = iconRegister.registerIcon(Config.resourceDomain + ":case_top")
    icons(ForgeDirection.UP.ordinal) = icons(ForgeDirection.DOWN.ordinal)

    icons(ForgeDirection.NORTH.ordinal) = iconRegister.registerIcon(Config.resourceDomain + ":power_distributor")
    icons(ForgeDirection.SOUTH.ordinal) = icons(ForgeDirection.NORTH.ordinal)
    icons(ForgeDirection.WEST.ordinal) = icons(ForgeDirection.NORTH.ordinal)
    icons(ForgeDirection.EAST.ordinal) = icons(ForgeDirection.NORTH.ordinal)
  }

  override def getLightValue(world: IBlockAccess, x: Int, y: Int, z: Int) = 5

  // ----------------------------------------------------------------------- //
  // Tile entity
  // ----------------------------------------------------------------------- //

  override def hasTileEntity = true

  override def createTileEntity(world: World) = Some(new tileentity.PowerDistributor)
}

