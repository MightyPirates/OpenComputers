package li.cil.oc.common.block

import java.util

import cpw.mods.fml.relauncher.{Side, SideOnly}
import li.cil.oc.common.tileentity
import li.cil.oc.util.Tooltip
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.{EnumRarity, ItemStack}
import net.minecraft.world.{IBlockAccess, World}
import net.minecraftforge.common.util.ForgeDirection

class Hologram(val tier: Int) extends SimpleBlock with traits.SpecialBlock {
  setLightLevel(1)
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

  // ----------------------------------------------------------------------- //

  override def rarity = Array(EnumRarity.uncommon, EnumRarity.rare).apply(tier)

  override def addInformation(metadata: Int, stack: ItemStack, player: EntityPlayer, tooltip: util.List[String], advanced: Boolean) {
    tooltip.addAll(Tooltip.get(getClass.getSimpleName + tier))
  }

  // ----------------------------------------------------------------------- //

  override def hasTileEntity(metadata: Int) = true

  override def createTileEntity(world: World, metadata: Int) = new tileentity.Hologram(tier)
}
