package li.cil.oc.integration.appeng

import appeng.api.AEApi
import appeng.api.config.Actionable
import appeng.api.config.FuzzyMode
import appeng.api.config.Settings
import appeng.api.config.Upgrades
import appeng.api.networking.security.MachineSource
import appeng.parts.automation.PartExportBus
import appeng.util.Platform
import li.cil.oc.api.driver
import li.cil.oc.api.driver.NamedBlock
import li.cil.oc.api.internal.Database
import li.cil.oc.api.machine.Arguments
import li.cil.oc.api.machine.Callback
import li.cil.oc.api.machine.Context
import li.cil.oc.api.network.Component
import li.cil.oc.integration.ManagedTileEntityEnvironment
import li.cil.oc.util.BlockPosition
import li.cil.oc.util.ExtendedArguments._
import li.cil.oc.util.InventoryUtils
import li.cil.oc.util.ResultWrapper._
import net.minecraft.world.World
import net.minecraftforge.common.util.ForgeDirection

import scala.collection.convert.WrapAsScala._

object DriverExportBus extends driver.Block {
  type ExportBusTile = appeng.api.parts.IPartHost

  override def worksWith(world: World, x: Int, y: Int, z: Int) =
    world.getTileEntity(x, y, z) match {
      case container: ExportBusTile => ForgeDirection.VALID_DIRECTIONS.map(container.getPart).exists(_.isInstanceOf[PartExportBus])
      case _ => false
    }

  override def createEnvironment(world: World, x: Int, y: Int, z: Int) = new Environment(world.getTileEntity(x, y, z).asInstanceOf[ExportBusTile])

  class Environment(host: ExportBusTile) extends ManagedTileEntityEnvironment[ExportBusTile](host, "me_exportbus") with NamedBlock {
    override def preferredName = "me_exportbus"

    override def priority = 0

    @Callback(doc = "function(side:number, [ slot:number]):boolean -- Get the configuration of the export bus pointing in the specified direction.")
    def getConfiguration(context: Context, args: Arguments): Array[AnyRef] = {
      val side = args.checkSide(0, ForgeDirection.VALID_DIRECTIONS: _*)
      host.getPart(side) match {
        case export: PartExportBus =>
          val config = export.getInventoryByName("config")
          val slot = args.optSlot(config, 2, 0)
          val stack = config.getStackInSlot(slot)
          result(stack)
        case _ => result(Unit, "no export bus")
      }
    }

    @Callback(doc = "function(side:number[, slot:number][, database:address, entry:number]):boolean -- Configure the export bus pointing in the specified direction to export item stacks matching the specified descriptor.")
    def setConfiguration(context: Context, args: Arguments): Array[AnyRef] = {
      val side = args.checkSide(0, ForgeDirection.VALID_DIRECTIONS: _*)
      host.getPart(side) match {
        case export: PartExportBus =>
          val config = export.getInventoryByName("config")
          val slot = if (args.count > 3 || args.count < 3) args.optSlot(config, 1, 0) else 0
          val stack = if (args.count > 2) {
            val (address, entry) =
              if (args.count > 3) (args.checkString(2), args.checkInteger(3))
              else (args.checkString(1), args.checkInteger(2))
            node.network.node(address) match {
              case component: Component => component.host match {
                case database: Database => database.getStackInSlot(entry - 1)
                case _ => throw new IllegalArgumentException("not a database")
              }
              case _ => throw new IllegalArgumentException("no such component")
            }
          }
          else null
          config.setInventorySlotContents(slot, stack)
          context.pause(0.5)
          result(true)
        case _ => result(Unit, "no export bus")
      }
    }

    @Callback(doc = "function(side:number, slot:number):boolean -- Make the export bus facing the specified direction perform a single export operation into the specified slot.")
    def exportIntoSlot(context: Context, args: Arguments): Array[AnyRef] = {
      val side = args.checkSide(0, ForgeDirection.VALID_DIRECTIONS: _*)
      host.getPart(side) match {
        case export: PartExportBus =>
          InventoryUtils.inventoryAt(BlockPosition(host.getLocation).offset(side)) match {
            case Some(inventory) =>
              val targetSlot = args.checkSlot(inventory, 1)
              val config = export.getInventoryByName("config")
              val itemStorage = export.getProxy.getStorage.getItemInventory
              var count = export.getInstalledUpgrades(Upgrades.SPEED) match {
                case 1 => 8
                case 2 => 32
                case 3 => 64
                case 4 => 96
                case _ => 1
              }
              // We need reflection here to avoid compiling against the return type,
              // which has changed in rv2-beta-20 or so.
              val fuzzyMode = export.getConfigManager.
                getClass.getMethod("getSetting", classOf[Enum[_]]).
                invoke(export.getConfigManager, Settings.FUZZY_MODE).asInstanceOf[FuzzyMode]
              val source = new MachineSource(export)
              var didSomething = false
              for (slot <- 0 until config.getSizeInventory if count > 0) {
                val filter = AEApi.instance.storage.createItemStack(config.getStackInSlot(slot))
                val stacks =
                  if (export.getInstalledUpgrades(Upgrades.FUZZY) > 0)
                    itemStorage.getStorageList.findFuzzy(filter, fuzzyMode).toSeq
                  else
                    Seq(itemStorage.getStorageList.findPrecise(filter))
                for (ais <- stacks if count > 0 && ais != null) {
                  val is = ais.getItemStack
                  is.stackSize = count
                  if (InventoryUtils.insertIntoInventorySlot(is, inventory, Option(side.getOpposite), targetSlot, count, simulate = true)) {
                    ais.setStackSize(count - is.stackSize)
                    val eais = Platform.poweredExtraction(export.getProxy.getEnergy, itemStorage, ais, source)
                    if (eais != null) {
                      val eis = eais.getItemStack
                      count -= eis.stackSize
                      didSomething = true
                      InventoryUtils.insertIntoInventorySlot(eis, inventory, Option(side.getOpposite), targetSlot)
                      if (eis.stackSize > 0) {
                        eais.setStackSize(eis.stackSize)
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
        case _ => result(Unit, "no export bus")
      }
    }
  }
}
