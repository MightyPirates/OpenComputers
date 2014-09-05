package li.cil.oc.common.block

import java.util

import cpw.mods.fml.common.Optional
import li.cil.oc.{Settings, Localization}
import li.cil.oc.common.tileentity
import li.cil.oc.util.Tooltip
import li.cil.oc.util.mods.Mods
import mcp.mobius.waila.api.{IWailaConfigHandler, IWailaDataAccessor}
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.world.World

class Redstone(val parent: SimpleDelegator) extends RedstoneAware with SimpleDelegate {
  override protected def customTextures = Array(
    Some("RedstoneTop"),
    Some("RedstoneTop"),
    Some("RedstoneSide"),
    Some("RedstoneSide"),
    Some("RedstoneSide"),
    Some("RedstoneSide")
  )

  override def tooltipLines(stack: ItemStack, player: EntityPlayer, tooltip: util.List[String], advanced: Boolean) {
    super.tooltipLines(stack, player, tooltip, advanced)
    if (Mods.RedLogic.isAvailable) {
      tooltip.addAll(Tooltip.get("RedstoneCard.RedLogic"))
    }
    if (Mods.MineFactoryReloaded.isAvailable) {
      tooltip.addAll(Tooltip.get("RedstoneCard.RedNet"))
    }
  }

  @Optional.Method(modid = Mods.IDs.Waila)
  override def wailaBody(stack: ItemStack, tooltip: util.List[String], accessor: IWailaDataAccessor, config: IWailaConfigHandler) {
    val node = accessor.getNBTData.getCompoundTag(Settings.namespace + "redstone").getCompoundTag("node")
    if (node.hasKey("address")) {
      tooltip.add(Localization.Analyzer.Address(node.getString("address")).getUnformattedText)
    }
  }

  override def createTileEntity(world: World) = Some(new tileentity.Redstone())
}
