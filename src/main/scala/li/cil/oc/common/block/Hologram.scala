package li.cil.oc.common.block

import java.util
import li.cil.oc.Settings
import li.cil.oc.common.tileentity
import li.cil.oc.util.Tooltip
import net.minecraft.client.renderer.texture.IIconRegister
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.{ItemStack, EnumRarity}
import net.minecraft.world.{World, IBlockAccess}
import net.minecraft.util.{IIcon, AxisAlignedBB}
import net.minecraftforge.common.util.ForgeDirection

class Hologram(val parent: SpecialDelegator) extends SpecialDelegate {
  val unlocalizedName = "Hologram"

  private val icons = Array.fill[IIcon](6)(null)

  override def rarity = EnumRarity.rare

  override def tooltipLines(stack: ItemStack, player: EntityPlayer, tooltip: util.List[String], advanced: Boolean) {
    tooltip.addAll(Tooltip.get(unlocalizedName))
  }

  override def icon(side: ForgeDirection) = Some(icons(side.ordinal()))

  override def luminance(world: IBlockAccess, x: Int, y: Int, z: Int) = 15

  override def isSolid(world: IBlockAccess, x: Int, y: Int, z: Int, side: ForgeDirection) = side == ForgeDirection.DOWN

  override def bounds(world: IBlockAccess, x: Int, y: Int, z: Int) =
    AxisAlignedBB.getAABBPool.getAABB(0, 0, 0, 1, 3 / 16f, 1)

  override def itemBounds() {
    parent.setBlockBounds(AxisAlignedBB.getAABBPool.getAABB(0, 0, 0, 1, 3 / 16f, 1))
  }

  override def registerIcons(iconRegister: IIconRegister) = {
    icons(ForgeDirection.DOWN.ordinal) = iconRegister.registerIcon(Settings.resourceDomain + ":generic_top")
    icons(ForgeDirection.UP.ordinal) = iconRegister.registerIcon(Settings.resourceDomain + ":hologram_top")

    icons(ForgeDirection.NORTH.ordinal) = iconRegister.registerIcon(Settings.resourceDomain + ":hologram_side")
    icons(ForgeDirection.SOUTH.ordinal) = icons(ForgeDirection.NORTH.ordinal)
    icons(ForgeDirection.WEST.ordinal) = icons(ForgeDirection.NORTH.ordinal)
    icons(ForgeDirection.EAST.ordinal) = icons(ForgeDirection.NORTH.ordinal)
  }

  // ----------------------------------------------------------------------- //

  override def hasTileEntity = true

  override def createTileEntity(world: World) = Some(new tileentity.Hologram())
}
