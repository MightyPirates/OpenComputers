package li.cil.oc.integration.appeng

import appeng.api.AEApi
import appeng.api.config.{Actionable, FuzzyMode, Settings, Upgrades}
import appeng.api.implementations.IUpgradeableHost
import appeng.api.implementations.tiles.ISegmentedInventory
import appeng.api.networking.IGridHost
import appeng.api.networking.security.IActionHost
import appeng.api.parts.{IPartHost, PartItemStack}
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
import net.minecraft.util.EnumFacing
import net.minecraft.world.World
import net.minecraft.util.math.BlockPos

object DriverExportBus extends driver.DriverBlock {
  override def worksWith(world: World, pos: BlockPos, side: EnumFacing) =
    world.getTileEntity(pos) match {
      case container: IPartHost => EnumFacing.VALUES.map(container.getPart).filter(p => p != null).map(_.getItemStack(PartItemStack.PICK)).exists(AEUtil.isExportBus)
      case _ => false
    }

  override def createEnvironment(world: World, pos: BlockPos, side: EnumFacing) = new Environment(world.getTileEntity(pos).asInstanceOf[IPartHost])

  final class Environment(val host: IPartHost) extends ManagedTileEntityEnvironment[IPartHost](host, "me_exportbus") with NamedBlock with PartEnvironmentBase {
    override def preferredName = "me_exportbus"

    override def priority = 2

    @Callback(doc = "function(side:number, [ slot:number]):boolean -- Get the configuration of the export bus pointing in the specified direction.")
    def getExportConfiguration(context: Context, args: Arguments): Array[AnyRef] = getPartConfig[ISegmentedInventory](context, args)

    @Callback(doc = "function(side:number[, slot:number][, database:address, entry:number):boolean -- Configure the export bus pointing in the specified direction to export item stacks matching the specified descriptor.")
    def setExportConfiguration(context: Context, args: Arguments): Array[AnyRef] = setPartConfig[ISegmentedInventory](context, args)

    @Callback(doc = "function(side:number, slot:number):boolean -- Make the export bus facing the specified direction perform a single export operation into the specified slot.")
    def exportIntoSlot(context: Context, args: Arguments): Array[AnyRef] = {
      val side = args.checkSideAny(0)
      val part = host.getPart(side)

      if (part != null && AEUtil.isExportBus(part.getItemStack(PartItemStack.PICK))) {
        val export = part.asInstanceOf[ISegmentedInventory with IConfigurableObject with IUpgradeableHost with IActionHost with IGridHost]
        val location = host.getLocation
        InventoryUtils.inventoryAt(new BlockPosition(location.x, location.y, location.z, Some(location.getWorld)).offset(side), side.getOpposite) match {
          case Some(inventory) =>
            val targetSlot = args.checkSlot(inventory, 1)
            val config = export.getInventoryByName("config")
            val itemStorage = AEUtil.getGridStorage(export.getGridNode(AEPartLocation.fromFacing(side)).getGrid).getInventory(AEUtil.itemStorageChannel)
            var count = export.getInstalledUpgrades(Upgrades.SPEED) match {
              case 1 => 8
              case 2 => 32
              case 3 => 64
              case 4 => 96
              case _ => 1
            }
            val fuzzyMode = export.getConfigManager.getSetting(Settings.FUZZY_MODE).asInstanceOf[FuzzyMode]
            val source = new MachineSource(export)
            var didSomething = false
            for (slot <- 0 until config.getSlots if count > 0) {
              val filter = AEUtil.itemStorageChannel.createStack(config.getStackInSlot(slot))
              val stacks =
                if (export.getInstalledUpgrades(Upgrades.FUZZY) > 0)
                  itemStorage.getStorageList.findFuzzy(filter, fuzzyMode).toArray.toSeq
                else
                  Seq(itemStorage.getStorageList.findPrecise(filter))
              for (ais <- stacks.filter(_ != null).map(_.asInstanceOf[IAEItemStack].copy) if count > 0) {
                val is = ais.createItemStack()
                is.setCount(count)
                if (InventoryUtils.insertIntoInventorySlot(is, inventory, targetSlot, count, simulate = true)) {
                  ais.setStackSize(count - is.getCount)
                  val eais = AEUtil.itemStorageChannel.poweredExtraction(AEUtil.getGridEnergy(export.getGridNode(AEPartLocation.fromFacing(side)).getGrid), itemStorage, ais, source)
                  if (eais != null) {
                    val eis = eais.createItemStack()
                    count -= eis.getCount
                    didSomething = true
                    InventoryUtils.insertIntoInventorySlot(eis, inventory, targetSlot)
                    if (eis.getCount > 0) {
                      eais.setStackSize(eis.getCount)
                      itemStorage.injectItems(ais, Actionable.MODULATE, source)
                    }
                  }
                }
              }
            }
            if (didSomething) {
              context.pause(0.25)
            }
            result(didSomething)
          case _ => result(Unit, "no inventory")
        }
      }
      else result(Unit, "no export bus")
    }
  }

  object Provider extends EnvironmentProvider {
    override def getEnvironment(stack: ItemStack): Class[_] =
      if (AEUtil.isExportBus(stack))
        classOf[Environment]
      else null
  }

}
