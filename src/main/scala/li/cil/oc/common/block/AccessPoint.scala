package li.cil.oc.common.block

import java.util

import cpw.mods.fml.common.Optional
import li.cil.oc.common.tileentity
import li.cil.oc.util.mods.Mods
import li.cil.oc.{Localization, Settings}
import mcp.mobius.waila.api.{IWailaConfigHandler, IWailaDataAccessor}
import net.minecraft.item.ItemStack
import net.minecraft.world.World
import net.minecraftforge.common.util.Constants.NBT

class AccessPoint(parent: SimpleDelegator) extends Switch(parent) {
  override protected def customTextures = Array(
    None,
    Some("AccessPointTop"),
    Some("SwitchSide"),
    Some("SwitchSide"),
    Some("SwitchSide"),
    Some("SwitchSide")
  )

  @Optional.Method(modid = Mods.IDs.Waila)
  override def wailaBody(stack: ItemStack, tooltip: util.List[String], accessor: IWailaDataAccessor, config: IWailaConfigHandler) {
    val nbt = accessor.getNBTData
    val node = nbt.getTagList(Settings.namespace + "componentNodes", NBT.TAG_COMPOUND).getCompoundTagAt(accessor.getSide.ordinal)
    if (node.hasKey("address")) {
      tooltip.add(Localization.Analyzer.Address(node.getString("address")).toString)
    }
    if (nbt.hasKey(Settings.namespace + "strength")) {
      tooltip.add(Localization.Analyzer.WirelessStrength(nbt.getDouble(Settings.namespace + "strength")).getUnformattedTextForChat)
    }
  }

  // ----------------------------------------------------------------------- //

  override def createTileEntity(world: World) = Some(new tileentity.AccessPoint())
}
