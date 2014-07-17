package li.cil.oc.common.block

import java.util

import cpw.mods.fml.common.Optional
import li.cil.oc.common.tileentity
import li.cil.oc.util.Tooltip
import li.cil.oc.{Localization, Settings}
import mcp.mobius.waila.api.{IWailaConfigHandler, IWailaDataAccessor}
import net.minecraft.client.renderer.texture.IIconRegister
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.util.{IIcon}
import net.minecraft.world.World
import net.minecraftforge.common.util.Constants.NBT
import net.minecraftforge.common.util.ForgeDirection

class AccessPoint(val parent: SimpleDelegator) extends SimpleDelegate {
  val unlocalizedName = "WirelessRouter"

  private val icons = Array.fill[IIcon](6)(null)

  // ----------------------------------------------------------------------- //

  override def tooltipLines(stack: ItemStack, player: EntityPlayer, tooltip: util.List[String], advanced: Boolean) {
    tooltip.addAll(Tooltip.get(unlocalizedName))
  }

  @Optional.Method(modid = "Waila")
  override def wailaBody(stack: ItemStack, tooltip: util.List[String], accessor: IWailaDataAccessor, config: IWailaConfigHandler) {
    val nbt = accessor.getNBTData
    val node = nbt.getTagList(Settings.namespace + "componentNodes", NBT.TAG_COMPOUND).getCompoundTagAt(accessor.getSide.ordinal)
    if (node.hasKey("address")) {
      tooltip.add(Localization.Analyzer.Address(node.getString("address")).toString)
    }
    if (nbt.hasKey(Settings.namespace + "strength")) {
      tooltip.add(Localization.Analyzer.WirelessStrength(nbt.getDouble(Settings.namespace + "strength")).toString)
    }
  }

  override def icon(side: ForgeDirection) = Some(icons(side.ordinal))

  override def registerIcons(iconRegister: IIconRegister) = {
    icons(ForgeDirection.DOWN.ordinal) = iconRegister.registerIcon(Settings.resourceDomain + ":generic_top")
    icons(ForgeDirection.UP.ordinal) = iconRegister.registerIcon(Settings.resourceDomain + ":router_wireless_top")

    icons(ForgeDirection.NORTH.ordinal) = iconRegister.registerIcon(Settings.resourceDomain + ":router_side")
    icons(ForgeDirection.SOUTH.ordinal) = icons(ForgeDirection.NORTH.ordinal)
    icons(ForgeDirection.WEST.ordinal) = icons(ForgeDirection.NORTH.ordinal)
    icons(ForgeDirection.EAST.ordinal) = icons(ForgeDirection.NORTH.ordinal)
  }

  // ----------------------------------------------------------------------- //

  override def hasTileEntity = true

  override def createTileEntity(world: World) = Some(new tileentity.WirelessRouter(world.isRemote))
}
