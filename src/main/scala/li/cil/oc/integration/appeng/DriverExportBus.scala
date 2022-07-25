package li.cil.oc.integration.appeng

import appeng.api.config.{Actionable, FuzzyMode, Settings, Upgrades}
import appeng.api.implementations.IUpgradeableHost
import appeng.api.implementations.tiles.ISegmentedInventory
import appeng.api.networking.IGridHost
import appeng.api.networking.security.IActionHost
import appeng.api.parts.{IPartHost, PartItemStack}
import appeng.api.storage.IMEMonitor
import appeng.api.storage.data.IAEItemStack
import appeng.api.util.{AEPartLocation, IConfigurableObject}
import li.cil.oc.api.driver
import li.cil.oc.api.driver.EnvironmentProvider
import li.cil.oc.api.driver.NamedBlock
import li.cil.oc.api.machine.Arguments
import li.cil.oc.api.machine.Callback
import li.cil.oc.api.machine.Context
import li.cil.oc.integration.ManagedTileEntityEnvironment
import li.cil.oc.util.BlockPosition
import li.cil.oc.util.ExtendedArguments._
import li.cil.oc.util.InventoryUtils
import li.cil.oc.util.ResultWrapper._
import net.minecraft.item.ItemStack
import net.minecraft.util.Direction
import net.minecraft.world.World
import net.minecraft.util.math.BlockPos
import net.minecraftforge.items.IItemHandler

object DriverExportBus extends driver.DriverBlock {
  override def worksWith(world: World, pos: BlockPos, side: Direction) =
    world.getBlockEntity(pos) match {
      case container: IPartHost => Direction.values.map(container.getPart).filter(p => p != null).map(_.getItemStack(PartItemStack.PICK)).exists(AEUtil.isExportBus)
      case _ => false
    }

  override def createEnvironment(world: World, pos: BlockPos, side: Direction) = new Environment(world.getBlockEntity(pos).asInstanceOf[IPartHost])

  final class Environment(val host: IPartHost) extends ManagedTileEntityEnvironment[IPartHost](host, "me_exportbus") with NamedBlock with PartEnvironmentBase {
    override def preferredName = "me_exportbus"

    override def priority = 2

    @Callback(doc = "function(side:number, [ slot:number]):boolean -- Get the configuration of the export bus pointing in the specified direction.")
    def getExportConfiguration(context: Context, args: Arguments): Array[AnyRef] = getPartConfig[ISegmentedInventory](context, args)

    @Callback(doc = "function(side:number[, slot:number][, database:address, entry:number):boolean -- Configure the export bus pointing in the specified direction to export item stacks matching the specified descriptor.")
    def setExportConfiguration(context: Context, args: Arguments): Array[AnyRef] = setPartConfig[ISegmentedInventory](context, args)

    def doExport(itemStorage: IMEMonitor[IAEItemStack], ais: IAEItemStack, inventory: IItemHandler, targetSlot: Option[Int], count: Int, source: MachineSource, simulate: Boolean): Boolean = {
      val limit = ais.getStackSize.toInt min count
      ais.setStackSize(limit)
      val itemStack = ais.createItemStack
      if (targetSlot.isDefined) {
        if (!InventoryUtils.insertIntoInventorySlot(itemStack, inventory, targetSlot.get, count, simulate)) {
          return false
        }
      }
      else if (!InventoryUtils.insertIntoInventory(itemStack, inventory, count, simulate)) {
        return false
      }

      if (itemStack.getCount > 0) {
        ais.setStackSize(limit - itemStack.getCount)
      }

      val extracted: IAEItemStack = itemStorage.extractItems(ais, if (simulate) Actionable.SIMULATE else Actionable.MODULATE, source)

      extracted != null
    }

    @Callback(doc = "function(side:number, [slot:number]):boolean -- Make the export bus facing the specified direction perform a single export operation into the specified slot.")
    def exportIntoSlot(context: Context, args: Arguments): Array[AnyRef] = {
      val side = args.checkSideAny(0)
      val part = host.getPart(side)

      if (part == null || !AEUtil.isExportBus(part.getItemStack(PartItemStack.PICK))) {
        return result(Unit, "no export bus")
      }

      val exportBus = part.asInstanceOf[ISegmentedInventory with IConfigurableObject with IUpgradeableHost with IActionHost with IGridHost]
      val location = host.getLocation

      val inventory: IItemHandler = InventoryUtils.inventoryAt(new BlockPosition(location.x, location.y, location.z, Some(location.getWorld)).offset(side), side.getOpposite) match {
        case Some(inv) => inv
        case _ => return result(Unit, "no inventory")
      }

      val targetSlot: Option[Int] = args.optSlot(inventory, 1, -1) match {
        case -1 => None
        case any => Option(any)
      }
      val config = exportBus.getInventoryByName("config")
      val itemStorage = AEUtil.getGridStorage(exportBus.getGridNode(AEPartLocation.fromFacing(side)).getGrid).getInventory(AEUtil.itemStorageChannel)
      var count = exportBus.getInstalledUpgrades(Upgrades.SPEED) match {
        case 1 => 8
        case 2 => 32
        case 3 => 64
        case 4 => 96
        case _ => 1
      }
      val fuzzyMode = exportBus.getConfigManager.getSetting(Settings.FUZZY_MODE).asInstanceOf[FuzzyMode]
      val source = new MachineSource(exportBus)
      val potentialWork = count

      for (slot <- 0 until config.getSlots if count > 0) {
        val filter = AEUtil.itemStorageChannel.createStack(config.getStackInSlot(slot))
        val stacks =
          if (exportBus.getInstalledUpgrades(Upgrades.FUZZY) > 0)
            itemStorage.getStorageList.findFuzzy(filter, fuzzyMode).toArray.toSeq
          else
            Seq(itemStorage.getStorageList.findPrecise(filter))

        for (ais <- stacks.filter(_ != null).map(_.asInstanceOf[IAEItemStack].copy) if count > 0 && ais.getStackSize > 0) {
          if (doExport(itemStorage, ais, inventory, targetSlot, count, source, simulate = true)) {
            if (doExport(itemStorage, ais, inventory, targetSlot, count, source, simulate = false)) {
              count = (count - ais.getStackSize.toInt) max 0
              context.pause(0.25)
            }
          }
        }
      }
      if (potentialWork == count)
        result(Unit, "no items moved")
      else
        result(potentialWork - count)
    }
  }

  object Provider extends EnvironmentProvider {
    override def getEnvironment(stack: ItemStack): Class[_] =
      if (AEUtil.isExportBus(stack))
        classOf[Environment]
      else null
  }

}
