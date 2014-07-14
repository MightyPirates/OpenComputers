package li.cil.oc.common.block

import java.util

import cpw.mods.fml.common.Optional
import cpw.mods.fml.relauncher.{Side, SideOnly}
import li.cil.oc.common.tileentity
import li.cil.oc.util.Tooltip
import li.cil.oc.{Localization, Settings}
import mcp.mobius.waila.api.{IWailaConfigHandler, IWailaDataAccessor}
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.{EnumRarity, ItemStack}
import net.minecraft.util.AxisAlignedBB
import net.minecraft.world.{IBlockAccess, World}
import net.minecraftforge.common.util.ForgeDirection

abstract class Hologram(val parent: SpecialDelegator) extends SpecialDelegate {
  val unlocalizedName = "Hologram" + tier

  def tier: Int

  override def rarity = Array(EnumRarity.uncommon, EnumRarity.rare).apply(tier)

  override def tooltipLines(stack: ItemStack, player: EntityPlayer, tooltip: util.List[String], advanced: Boolean) {
    tooltip.addAll(Tooltip.get(unlocalizedName))
  }

  @Optional.Method(modid = "Waila")
  override def wailaBody(stack: ItemStack, tooltip: util.List[String], accessor: IWailaDataAccessor, config: IWailaConfigHandler) {
    val node = accessor.getNBTData.getCompoundTag(Settings.namespace + "node")
    if (node.hasKey("address")) {
      tooltip.add(Localization.Analyzer.Address(node.getString("address")).getUnformattedTextForChat)
    }
  }

  private val icons = Array.fill[Icon](6)(null)

  override def icon(side: ForgeDirection) = Some(icons(side.ordinal()))

  override def luminance(world: IBlockAccess, x: Int, y: Int, z: Int) = 15

  override def isSolid(world: IBlockAccess, x: Int, y: Int, z: Int, side: ForgeDirection) = side == ForgeDirection.DOWN

  @SideOnly(Side.CLIENT)
  override def shouldSideBeRendered(world: IBlockAccess, x: Int, y: Int, z: Int, side: ForgeDirection) = {
    super.shouldSideBeRendered(world, x, y, z, side) || side == ForgeDirection.UP
  }

  override def bounds(world: IBlockAccess, x: Int, y: Int, z: Int) =
    AxisAlignedBB.getBoundingBox(0, 0, 0, 1, 0.5f, 1)

  override def itemBounds() {
    parent.setBlockBounds(AxisAlignedBB.getBoundingBox(0, 0, 0, 1, 0.5f, 1))
  }

  override def registerIcons(iconRegister: IconRegister) = {
    icons(ForgeDirection.DOWN.ordinal) = iconRegister.registerIcon(Settings.resourceDomain + ":generic_top")
    icons(ForgeDirection.UP.ordinal) = iconRegister.registerIcon(Settings.resourceDomain + ":hologram_top" + tier)

    icons(ForgeDirection.NORTH.ordinal) = iconRegister.registerIcon(Settings.resourceDomain + ":hologram_side")
    icons(ForgeDirection.SOUTH.ordinal) = icons(ForgeDirection.NORTH.ordinal)
    icons(ForgeDirection.WEST.ordinal) = icons(ForgeDirection.NORTH.ordinal)
    icons(ForgeDirection.EAST.ordinal) = icons(ForgeDirection.NORTH.ordinal)
  }

  // ----------------------------------------------------------------------- //

  override def hasTileEntity = true

  override def createTileEntity(world: World) = Some(new tileentity.Hologram(tier))
}

object Hologram {

  class Tier1(parent: SpecialDelegator) extends Hologram(parent) {
    def tier = 0
  }

  class Tier2(parent: SpecialDelegator) extends Hologram(parent) {
    def tier = 1
  }

}