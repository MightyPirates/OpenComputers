package li.cil.oc.common.block

import java.util

import cpw.mods.fml.common.Optional
import li.cil.oc.common.tileentity
import li.cil.oc.util.mods.Mods
import li.cil.oc.{Localization, Settings}
import mcp.mobius.waila.api.{IWailaConfigHandler, IWailaDataAccessor}
import net.minecraft.item.ItemStack
import net.minecraft.world.World

class MotionSensor(val parent: SimpleDelegator) extends SimpleDelegate {
  override protected def customTextures = Array(
    Some("MotionSensorTop"),
    Some("MotionSensorTop"),
    Some("MotionSensorSide"),
    Some("MotionSensorSide"),
    Some("MotionSensorSide"),
    Some("MotionSensorSide")
  )

  // ----------------------------------------------------------------------- //

  @Optional.Method(modid = Mods.IDs.Waila)
  override def wailaBody(stack: ItemStack, tooltip: util.List[String], accessor: IWailaDataAccessor, config: IWailaConfigHandler) {
    val node = accessor.getNBTData.getCompoundTag(Settings.namespace + "node")
    if (node.hasKey("address")) {
      tooltip.add(Localization.Analyzer.Address(node.getString("address")).getUnformattedText)
    }
  }

  // ----------------------------------------------------------------------- //

  override def hasTileEntity = true

  override def createTileEntity(world: World) = Some(new tileentity.MotionSensor())
}
