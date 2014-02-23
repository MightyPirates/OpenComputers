package li.cil.oc.common.block

import java.util
import li.cil.oc.common.tileentity
import li.cil.oc.util.Tooltip
import li.cil.oc.{api, Settings}
import net.minecraft.client.renderer.texture.IIconRegister
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.util.IIcon
import net.minecraft.world.{World, IBlockAccess}
import net.minecraftforge.common.util.ForgeDirection
import net.minecraft.block.Block

class Capacitor(val parent: SimpleDelegator) extends SimpleDelegate {
  val unlocalizedName = "Capacitor"

  private val icons = Array.fill[IIcon](6)(null)

  // ----------------------------------------------------------------------- //

  override def tooltipLines(stack: ItemStack, player: EntityPlayer, tooltip: util.List[String], advanced: Boolean) {
    tooltip.addAll(Tooltip.get(unlocalizedName))
  }

  override def icon(side: ForgeDirection) = Some(icons(side.ordinal()))

  override def registerIcons(iconRegister: IIconRegister) = {
    icons(ForgeDirection.DOWN.ordinal) = iconRegister.registerIcon(Settings.resourceDomain + ":generic_top")
    icons(ForgeDirection.UP.ordinal) = iconRegister.registerIcon(Settings.resourceDomain + ":capacitor_top")

    icons(ForgeDirection.NORTH.ordinal) = iconRegister.registerIcon(Settings.resourceDomain + ":capacitor")
    icons(ForgeDirection.SOUTH.ordinal) = icons(ForgeDirection.NORTH.ordinal)
    icons(ForgeDirection.WEST.ordinal) = icons(ForgeDirection.NORTH.ordinal)
    icons(ForgeDirection.EAST.ordinal) = icons(ForgeDirection.NORTH.ordinal)
  }

  override def luminance(world: IBlockAccess, x: Int, y: Int, z: Int) = 5

  // ----------------------------------------------------------------------- //

  override def hasTileEntity = true

  override def createTileEntity(world: World) = Some(new tileentity.Capacitor)

  // ----------------------------------------------------------------------- //

  override def update(world: World, x: Int, y: Int, z: Int) =
    world.getTileEntity(x, y, z) match {
      case capacitor: tileentity.Capacitor =>
        api.Network.joinOrCreateNetwork(capacitor)
        capacitor.recomputeCapacity(updateSecondGradeNeighbors = true)
      case _ =>
    }

  override def neighborBlockChanged(world: World, x: Int, y: Int, z: Int, block: Block) =
    world.getTileEntity(x, y, z) match {
      case capacitor: tileentity.Capacitor => capacitor.recomputeCapacity()
      case _ =>
    }
}
