package li.cil.oc.util.mods

import java.util

import cpw.mods.fml.common.Optional
import li.cil.oc.Settings
import li.cil.oc.common.block.{Delegator, Keyboard}
import li.cil.oc.common.tileentity
import mcp.mobius.waila.api.{IWailaConfigHandler, IWailaDataAccessor, IWailaDataProvider, IWailaRegistrar}
import net.minecraft.item.ItemStack

object Waila {
  @Optional.Method(modid = Mods.IDs.Waila)
  def init(registrar: IWailaRegistrar) {
    registrar.registerBodyProvider(BlockDataProvider, classOf[Delegator[_]])
    registrar.registerBodyProvider(BlockDataProvider, classOf[Keyboard])

    def registerKeys(clazz: Class[_], names: String*) {
      for (name <- names) {
        registrar.registerSyncedNBTKey(name, clazz)
      }
      registrar.registerSyncedNBTKey("x", clazz)
      registrar.registerSyncedNBTKey("y", clazz)
      registrar.registerSyncedNBTKey("z", clazz)
    }
    registerKeys(classOf[tileentity.RobotAssembler], Settings.namespace + "node")
    registerKeys(classOf[tileentity.AccessPoint], Settings.namespace + "componentNodes", Settings.namespace + "strength")
    registerKeys(classOf[tileentity.Capacitor], Settings.namespace + "node")
    registerKeys(classOf[tileentity.Case], Settings.namespace + "address")
    registerKeys(classOf[tileentity.DiskDrive], Settings.namespace + "items")
    registerKeys(classOf[tileentity.Geolyzer], "node")
    registerKeys(classOf[tileentity.Hologram], Settings.namespace + "node")
    registerKeys(classOf[tileentity.Keyboard], Settings.namespace + "keyboard")
    registerKeys(classOf[tileentity.MotionSensor], Settings.namespace + "node")
    registerKeys(classOf[tileentity.Redstone], Settings.namespace + "redstone")
    registerKeys(classOf[tileentity.Screen], "node")
  }

  def isSavingForTooltip = new Exception().getStackTrace.exists(_.getClassName.startsWith("mcp.mobius.waila"))
}

object BlockDataProvider extends IWailaDataProvider {
  override def getWailaStack(accessor: IWailaDataAccessor, config: IWailaConfigHandler) =
    accessor.getBlock match {
      case delegator: Delegator[_] => delegator.subBlock(accessor.getMetadata).fold(null: ItemStack)(_.createItemStack())
      case keyboard: Keyboard => new ItemStack(keyboard)
      case _ => null
    }

  override def getWailaHead(stack: ItemStack, tooltip: util.List[String], accessor: IWailaDataAccessor, config: IWailaConfigHandler) = tooltip

  override def getWailaBody(stack: ItemStack, tooltip: util.List[String], accessor: IWailaDataAccessor, config: IWailaConfigHandler) = {
    accessor.getBlock match {
      case delegator: Delegator[_] => delegator.subBlock(accessor.getMetadata).foreach(_.wailaBody(stack, tooltip, accessor, config))
      case keyboard: Keyboard => keyboard.wailaBody(stack, tooltip, accessor, config)
      case _ =>
    }
    tooltip
  }

  override def getWailaTail(stack: ItemStack, tooltip: util.List[String], accessor: IWailaDataAccessor, config: IWailaConfigHandler) = tooltip
}