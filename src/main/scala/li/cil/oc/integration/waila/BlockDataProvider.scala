package li.cil.oc.integration.waila

import java.util

import li.cil.oc.Localization
import li.cil.oc.Settings
import li.cil.oc.common.block.AccessPoint
import li.cil.oc.common.block.Assembler
import li.cil.oc.common.block.Capacitor
import li.cil.oc.common.block.Case
import li.cil.oc.common.block.Charger
import li.cil.oc.common.block.DiskDrive
import li.cil.oc.common.block.Geolyzer
import li.cil.oc.common.block.Hologram
import li.cil.oc.common.block.Keyboard
import li.cil.oc.common.block.MotionSensor
import li.cil.oc.common.block.Redstone
import li.cil.oc.common.block.Screen
import li.cil.oc.common.block.SimpleBlock
import li.cil.oc.common.tileentity
import mcp.mobius.waila.api.IWailaConfigHandler
import mcp.mobius.waila.api.IWailaDataAccessor
import mcp.mobius.waila.api.IWailaDataProvider
import mcp.mobius.waila.api.IWailaRegistrar
import net.minecraft.item.ItemStack
import net.minecraftforge.common.util.Constants.NBT

object BlockDataProvider extends IWailaDataProvider {
  def init(registrar: IWailaRegistrar) {
    registrar.registerBodyProvider(BlockDataProvider, classOf[SimpleBlock])

    def registerKeys(clazz: Class[_], names: String*) {
      for (name <- names) {
        registrar.registerSyncedNBTKey(name, clazz)
      }
      registrar.registerSyncedNBTKey("x", clazz)
      registrar.registerSyncedNBTKey("y", clazz)
      registrar.registerSyncedNBTKey("z", clazz)
    }
    registerKeys(classOf[tileentity.Assembler], Settings.namespace + "node")
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

  override def getWailaBody(stack: ItemStack, tooltip: util.List[String], accessor: IWailaDataAccessor, config: IWailaConfigHandler) = {
    accessor.getBlock match {
      case block: AccessPoint =>
        val nbt = accessor.getNBTData
        val node = nbt.getTagList(Settings.namespace + "componentNodes", NBT.TAG_COMPOUND).getCompoundTagAt(accessor.getSide.ordinal)
        if (node.hasKey("address")) {
          tooltip.add(Localization.Analyzer.Address(node.getString("address")).getUnformattedText)
        }
        if (nbt.hasKey(Settings.namespace + "strength")) {
          tooltip.add(Localization.Analyzer.WirelessStrength(nbt.getDouble(Settings.namespace + "strength")).getUnformattedText)
        }
      case block: Capacitor =>
        val node = accessor.getNBTData.getCompoundTag(Settings.namespace + "node")
        if (node.hasKey("buffer")) {
          tooltip.add(Localization.Analyzer.StoredEnergy(node.getDouble("buffer").toInt.toString).getUnformattedText)
        }
      case block: Case =>
        val node = accessor.getNBTData
        if (node.hasKey(Settings.namespace + "address")) {
          tooltip.add(Localization.Analyzer.Address(node.getString(Settings.namespace + "address")).getUnformattedText)
        }
      case block: Charger =>
        accessor.getTileEntity match {
          case charger: tileentity.Charger =>
            tooltip.add(Localization.Analyzer.ChargerSpeed(charger.chargeSpeed).getUnformattedText)
          case _ =>
        }
      case block: DiskDrive =>
        val items = accessor.getNBTData.getTagList(Settings.namespace + "items", NBT.TAG_COMPOUND)
        if (items.tagCount > 0) {
          val node = items.getCompoundTagAt(0).
            getCompoundTag("item").
            getCompoundTag("tag").
            getCompoundTag(Settings.namespace + "data").
            getCompoundTag("node")
          if (node.hasKey("address")) {
            tooltip.add(Localization.Analyzer.Address(node.getString("address")).getUnformattedText)
          }
        }
      case block: Geolyzer =>
        val node = accessor.getNBTData.getCompoundTag("node")
        if (node.hasKey("address")) {
          tooltip.add(Localization.Analyzer.Address(node.getString("address")).getUnformattedText)
        }
      case block: Hologram =>
        val node = accessor.getNBTData.getCompoundTag(Settings.namespace + "node")
        if (node.hasKey("address")) {
          tooltip.add(Localization.Analyzer.Address(node.getString("address")).getUnformattedText)
        }
      case block: MotionSensor =>
        val node = accessor.getNBTData.getCompoundTag(Settings.namespace + "node")
        if (node.hasKey("address")) {
          tooltip.add(Localization.Analyzer.Address(node.getString("address")).getUnformattedText)
        }
      case block: Redstone =>
        val node = accessor.getNBTData.getCompoundTag(Settings.namespace + "redstone").getCompoundTag("node")
        if (node.hasKey("address")) {
          tooltip.add(Localization.Analyzer.Address(node.getString("address")).getUnformattedText)
        }
      case block: Assembler =>
        val node = accessor.getNBTData.getCompoundTag(Settings.namespace + "node")
        if (node.hasKey("address")) {
          tooltip.add(Localization.Analyzer.Address(node.getString("address")).getUnformattedText)
        }
      case block: Screen =>
        val node = accessor.getNBTData.getCompoundTag("node")
        if (node.hasKey("address")) {
          tooltip.add(Localization.Analyzer.Address(node.getString("address")).getUnformattedText)
        }
      case keyboard: Keyboard =>
        val node = accessor.getNBTData.getCompoundTag(Settings.namespace + "keyboard").getCompoundTag("node")
        if (node.hasKey("address")) {
          tooltip.add(Localization.Analyzer.Address(node.getString("address")).getUnformattedText)
        }
      case _ =>
    }
    tooltip
  }

  override def getWailaStack(accessor: IWailaDataAccessor, config: IWailaConfigHandler) = accessor.getStack

  override def getWailaHead(stack: ItemStack, tooltip: util.List[String], accessor: IWailaDataAccessor, config: IWailaConfigHandler) = tooltip

  override def getWailaTail(stack: ItemStack, tooltip: util.List[String], accessor: IWailaDataAccessor, config: IWailaConfigHandler) = tooltip
}