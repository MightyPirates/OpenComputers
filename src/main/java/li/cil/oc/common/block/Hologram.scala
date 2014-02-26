package li.cil.oc.common.block

import java.util
import li.cil.oc.common.tileentity
import li.cil.oc.util.Tooltip
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.{ItemStack, EnumRarity}
import net.minecraft.world.{World, IBlockAccess}
import net.minecraftforge.common.ForgeDirection
import net.minecraft.util.AxisAlignedBB

class Hologram(val parent: SpecialDelegator) extends SpecialDelegate {
  val unlocalizedName = "Hologram"

  override def rarity = EnumRarity.rare

  override def tooltipLines(stack: ItemStack, player: EntityPlayer, tooltip: util.List[String], advanced: Boolean) {
    tooltip.addAll(Tooltip.get(unlocalizedName))
  }

  override def luminance(world: IBlockAccess, x: Int, y: Int, z: Int) = 15

  override def isSolid(world: IBlockAccess, x: Int, y: Int, z: Int, side: ForgeDirection) = side == ForgeDirection.DOWN

  override def bounds(world: IBlockAccess, x: Int, y: Int, z: Int) =
    AxisAlignedBB.getAABBPool.getAABB(0, 0, 0, 1, 0.2f, 1)

  // ----------------------------------------------------------------------- //

  override def hasTileEntity = true

  override def createTileEntity(world: World) = Some(new tileentity.Hologram())
}
