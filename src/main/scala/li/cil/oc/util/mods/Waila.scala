package li.cil.oc.util.mods

import cpw.mods.fml.common.Optional
import java.util
import li.cil.oc.common.block.Delegator
import li.cil.oc.common.tileentity
import li.cil.oc.Settings
import mcp.mobius.waila.api.{IWailaDataProvider, IWailaConfigHandler, IWailaDataAccessor, IWailaRegistrar}
import net.minecraft.item.ItemStack

object Waila {
  @Optional.Method(modid = "Waila")
  def init(registrar: IWailaRegistrar) {
    registrar.registerBodyProvider(BlockDataProvider, classOf[Delegator[_]])
    registrar.registerSyncedNBTKey(Settings.namespace + "node", classOf[tileentity.Capacitor])
    registrar.registerSyncedNBTKey(Settings.namespace + "items", classOf[tileentity.DiskDrive])
    registrar.registerSyncedNBTKey(Settings.namespace + "node", classOf[tileentity.Hologram])
    registrar.registerSyncedNBTKey(Settings.namespace + "keyboard", classOf[tileentity.Keyboard])
    registrar.registerSyncedNBTKey("node", classOf[tileentity.Screen])
    registrar.registerSyncedNBTKey(Settings.namespace + "componentNodes", classOf[tileentity.WirelessRouter])
    registrar.registerSyncedNBTKey(Settings.namespace + "strength", classOf[tileentity.WirelessRouter])
  }

  def isSavingForTooltip = new Exception().getStackTrace.exists(_.getClassName.startsWith("mcp.mobius.waila"))
}

object BlockDataProvider extends IWailaDataProvider {
  override def getWailaStack(accessor: IWailaDataAccessor, config: IWailaConfigHandler) =
    accessor.getBlock match {
      case delegator: Delegator[_] => delegator.subBlock(accessor.getMetadata).fold(null: ItemStack)(_.createItemStack())
      case _ => null
    }

  override def getWailaHead(stack: ItemStack, tooltip: util.List[String], accessor: IWailaDataAccessor, config: IWailaConfigHandler) = tooltip

  override def getWailaBody(stack: ItemStack, tooltip: util.List[String], accessor: IWailaDataAccessor, config: IWailaConfigHandler) = {
    accessor.getBlock match {
      case delegator: Delegator[_] => delegator.subBlock(accessor.getMetadata).foreach(_.wailaBody(stack, tooltip, accessor, config))
      case _ =>
    }
    tooltip
  }

  override def getWailaTail(stack: ItemStack, tooltip: util.List[String], accessor: IWailaDataAccessor, config: IWailaConfigHandler) = tooltip
}