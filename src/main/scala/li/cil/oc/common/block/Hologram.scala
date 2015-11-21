package li.cil.oc.common.block

import java.util

import cpw.mods.fml.relauncher.Side
import cpw.mods.fml.relauncher.SideOnly
import li.cil.oc.Settings
import li.cil.oc.common.tileentity
import li.cil.oc.integration.coloredlights.ModColoredLights
import li.cil.oc.util.Rarity
import li.cil.oc.util.Tooltip
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.world.IBlockAccess
import net.minecraft.world.World
import net.minecraftforge.common.util.ForgeDirection

class Hologram(val tier: Int) extends SimpleBlock with traits.SpecialBlock {
  if (Settings.get.hologramLight) {
    ModColoredLights.setLightLevel(this, 15, 15, 15)
  }
  setBlockBounds(0, 0, 0, 1, 0.5f, 1)

  // ----------------------------------------------------------------------- //

  override protected def customTextures = Array(
    None,
    Some("HologramTop" + tier),
    Some("HologramSide"),
    Some("HologramSide"),
    Some("HologramSide"),
    Some("HologramSide")
  )

  override def isBlockSolid(world: IBlockAccess, x: Int, y: Int, z: Int, side: ForgeDirection) = side == ForgeDirection.DOWN

  @SideOnly(Side.CLIENT)
  override def shouldSideBeRendered(world: IBlockAccess, x: Int, y: Int, z: Int, side: ForgeDirection) = {
    super.shouldSideBeRendered(world, x, y, z, side) || side == ForgeDirection.UP
  }

  override def isSideSolid(world: IBlockAccess, x: Int, y: Int, z: Int, side: ForgeDirection) = side == ForgeDirection.DOWN

  // ----------------------------------------------------------------------- //

  override def rarity(stack: ItemStack) = Rarity.byTier(tier)

  override protected def tooltipBody(metadata: Int, stack: ItemStack, player: EntityPlayer, tooltip: util.List[String], advanced: Boolean) {
    tooltip.addAll(Tooltip.get(getClass.getSimpleName + tier))
  }

  // ----------------------------------------------------------------------- //

  override def hasTileEntity(metadata: Int) = true

  override def createTileEntity(world: World, metadata: Int) = new tileentity.Hologram(tier)
}
