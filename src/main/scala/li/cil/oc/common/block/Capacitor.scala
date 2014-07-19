package li.cil.oc.common.block

import java.util

import cpw.mods.fml.common.Optional
import li.cil.oc.common.tileentity
import li.cil.oc.util.mods.Mods
import li.cil.oc.{Localization, Settings}
import mcp.mobius.waila.api.{IWailaConfigHandler, IWailaDataAccessor}
import net.minecraft.block.Block
import net.minecraft.item.ItemStack
import net.minecraft.world.{IBlockAccess, World}

class Capacitor(val parent: SimpleDelegator) extends SimpleDelegate {
  override protected def customTextures = Array(
    None,
    Some("CapacitorTop"),
    Some("CapacitorSide"),
    Some("CapacitorSide"),
    Some("CapacitorSide"),
    Some("CapacitorSide")
  )

  @Optional.Method(modid = Mods.IDs.Waila)
  override def wailaBody(stack: ItemStack, tooltip: util.List[String], accessor: IWailaDataAccessor, config: IWailaConfigHandler) {
    val node = accessor.getNBTData.getCompoundTag(Settings.namespace + "node")
    if (node.hasKey("buffer")) {
      tooltip.add(Localization.Analyzer.StoredEnergy(node.getDouble("buffer").toInt.toString).getUnformattedTextForChat)
    }
  }

  override def luminance(world: IBlockAccess, x: Int, y: Int, z: Int) = 5

  // ----------------------------------------------------------------------- //

  override def hasTileEntity = true

  override def createTileEntity(world: World) = Some(new tileentity.Capacitor())

  // ----------------------------------------------------------------------- //

  override def neighborBlockChanged(world: World, x: Int, y: Int, z: Int, block: Block) =
    world.getTileEntity(x, y, z) match {
      case capacitor: tileentity.Capacitor => capacitor.recomputeCapacity()
      case _ =>
    }
}
