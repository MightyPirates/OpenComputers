package li.cil.oc

import cpw.mods.fml.common.registry.GameRegistry
import li.cil.oc.common.item
import li.cil.oc.util.mods.Mods
import net.minecraft.item.{Item, ItemBlock, ItemStack}
import scala.collection.mutable
import li.cil.oc.api.detail.{ItemAPI, ItemInfo}
import net.minecraft.block.Block
import li.cil.oc.common.recipe.Recipes

object Items extends ItemAPI {
  private val descriptors = mutable.Map.empty[String, ItemInfo]

  private val names = mutable.Map.empty[Any, String]

  override def get(name: String): ItemInfo = descriptors.get(name).orNull

  override def get(stack: ItemStack) = names.get(getBlockOrItem(stack)) match {
    case Some(name) => get(name)
    case _ => null
  }

  def register(delegate: common.block.Delegate, name: String) {
    descriptors += name -> new ItemInfo {
      override def block = delegate.parent

      override def item = null

      override def createItemStack(size: Int) = delegate.createItemStack(size)
    }
    names += delegate -> name
  }

  def register(instance: Block, name: String) {
    descriptors += name -> new ItemInfo {
      override def block = instance

      override def item = null

      override def createItemStack(size: Int) = new ItemStack(instance, size)
    }
    names += instance -> name
  }

  def register(delegate: common.item.Delegate, name: String) {
    descriptors += name -> new ItemInfo {
      override def block = null

      override def item = delegate.parent

      override def createItemStack(size: Int) = delegate.createItemStack(size)
    }
    names += delegate -> name
  }

  def register(instance: Item, name: String) {
    descriptors += name -> new ItemInfo {
      override def block = null

      override def item = instance

      override def createItemStack(size: Int) = new ItemStack(instance, size)
    }
    names += instance -> name
  }

  private def getBlockOrItem(stack: ItemStack): Any = {
    multi.subItem(stack).getOrElse(
      Blocks.blockSimple.subBlock(stack).getOrElse(
        Blocks.blockSimpleWithRedstone.subBlock(stack).getOrElse(
          Blocks.blockSpecial.subBlock(stack).getOrElse(
            Blocks.blockSpecialWithRedstone.subBlock(stack).getOrElse(stack.getItem match {
              case block: ItemBlock if block.getBlockID >= 0 => net.minecraft.block.Block.blocksList(block.getBlockID)
              case item => item
            })
          )
        )
      )
    )
  }

  // ----------------------------------------------------------------------- //

  var multi: item.Delegator = _

  // ----------------------------------------------------------------------- //
  // Crafting
  var ironNugget: item.IronNugget = _

  def init() {
    multi = new item.Delegator(Settings.get.itemId)

    GameRegistry.registerItem(multi, Settings.namespace + "item")

    Recipes.addItemDelegate(new item.Analyzer(multi), "analyzer", "oc:analyzer")

    Recipes.addItemDelegate(new item.Memory(multi, 0), "ram1", "oc:ram1")
    Recipes.addItemDelegate(new item.Memory(multi, 2), "ram3", "oc:ram3")
    Recipes.addItemDelegate(new item.Memory(multi, 3), "ram4", "oc:ram4")

    Recipes.addItemDelegate(new item.FloppyDisk(multi), "floppy", "oc:floppy")
    Recipes.addItemDelegate(new item.HardDiskDrive(multi, 0), "hdd1", "oc:hdd1")
    Recipes.addItemDelegate(new item.HardDiskDrive(multi, 1), "hdd2", "oc:hdd2")
    Recipes.addItemDelegate(new item.HardDiskDrive(multi, 2), "hdd3", "oc:hdd3")

    Recipes.addItemDelegate(new item.GraphicsCard(multi, 0), "graphicsCard1", "oc:graphicsCard1")
    Recipes.addItemDelegate(new item.GraphicsCard(multi, 1), "graphicsCard2", "oc:graphicsCard2")
    Recipes.addItemDelegate(new item.GraphicsCard(multi, 2), "graphicsCard3", "oc:graphicsCard3")
    Recipes.addItemDelegate(new item.NetworkCard(multi), "lanCard", "oc:lanCard")
    Recipes.addItemDelegate(new item.RedstoneCard(multi), "redstoneCard", "oc:redstoneCard")
    Recipes.addItemDelegate(new item.WirelessNetworkCard(multi), "wlanCard", "oc:wlanCard")

    Recipes.addItemDelegate(new item.UpgradeCrafting(multi), "craftingUpgrade", "oc:craftingUpgrade")
    Recipes.addItemDelegate(new item.UpgradeGenerator(multi), "generatorUpgrade", "oc:generatorUpgrade")

    ironNugget = new item.IronNugget(multi)

    Recipes.addItemDelegate(new item.CuttingWire(multi), "cuttingWire", "oc:materialCuttingWire")
    Recipes.addItemDelegate(new item.Acid(multi), "acid", "oc:materialAcid")
    Recipes.addItemDelegate(new item.Disk(multi), "disk", "oc:materialDisk")

    Recipes.addItemDelegate(new item.ButtonGroup(multi), "buttonGroup", "oc:materialButtonGroup")
    Recipes.addItemDelegate(new item.ArrowKeys(multi), "arrowKeys", "oc:materialArrowKey")
    Recipes.addItemDelegate(new item.NumPad(multi), "numPad", "oc:materialNumPad")

    Recipes.addItemDelegate(new item.Transistor(multi), "transistor", "oc:materialTransistor")
    Recipes.addItemDelegate(new item.Microchip(multi, 0), "chip1", "oc:circuitChip1")
    Recipes.addItemDelegate(new item.Microchip(multi, 1), "chip2", "oc:circuitChip2")
    Recipes.addItemDelegate(new item.Microchip(multi, 2), "chip3", "oc:circuitChip3")
    Recipes.addItemDelegate(new item.ALU(multi), "alu", "oc:materialALU")
    Recipes.addItemDelegate(new item.ControlUnit(multi), "cu", "oc:materialCU")
    Recipes.addItemDelegate(new item.CPU(multi, 0), "cpu1", "oc:cpu1")

    Recipes.addItemDelegate(new item.RawCircuitBoard(multi), "rawCircuitBoard", "oc:materialCircuitBoardRaw")
    Recipes.addItemDelegate(new item.CircuitBoard(multi), "circuitBoard", "oc:materialCircuitBoard")
    Recipes.addItemDelegate(new item.PrintedCircuitBoard(multi), "printedCircuitBoard", "oc:materialCircuitBoardPrinted")
    Recipes.addItemDelegate(new item.CardBase(multi), "card", "oc:materialCard")

    // v1.1.0
    Recipes.addItemDelegate(new item.UpgradeSolarGenerator(multi), "solarGeneratorUpgrade", "oc:solarGeneratorUpgrade")
    Recipes.addItemDelegate(new item.UpgradeSign(multi), "signUpgrade", "oc:signUpgrade")
    Recipes.addItemDelegate(new item.UpgradeNavigation(multi), "navigationUpgrade", "oc:navigationUpgrade")

    // Always create, to avoid shifting IDs.
    val abstractBus = new item.AbstractBusCard(multi)
    if (Mods.StargateTech2.isAvailable) {
      Recipes.addItemDelegate(abstractBus, "abstractBusCard", "oc:abstractBusCard")
    }

    Recipes.addItemDelegate(new item.Memory(multi, 4), "ram5", "oc:ram5")
    Recipes.addItemDelegate(new item.Memory(multi, 5), "ram6", "oc:ram6")

    // v1.2.0
    Recipes.addItemDelegate(new item.Server(multi, 2), "server3", "oc:server3")
    Recipes.addItemDelegate(new item.Terminal(multi), "terminal", "oc:terminal")
    Recipes.addItemDelegate(new item.CPU(multi, 1), "cpu2", "oc:cpu2")
    Recipes.addItemDelegate(new item.CPU(multi, 2), "cpu3", "oc:cpu3")
    Recipes.addItemDelegate(new item.InternetCard(multi), "internetCard", "oc:internetCard")
    Recipes.addItemDelegate(new item.Server(multi, 0), "server1", "oc:server1")
    Recipes.addItemDelegate(new item.Server(multi, 1), "server2", "oc:server2")

    // v1.2.3
    register(new item.FloppyDisk(multi) {
      showInItemList = false
    }, "lootDisk")

    // v1.2.6
    Recipes.addItemDelegate(new item.Interweb(multi), "interweb", "oc:materialInterweb")
    Recipes.addItemDelegate(new item.UpgradeAngel(multi), "angelUpgrade", "oc:angelUpgrade")
    Recipes.addItemDelegate(new item.Memory(multi, 1), "ram2", "oc:ram2")

    // v1.3.0
    Recipes.addItemDelegate(new item.LinkedCard(multi), "linkedCard", "oc:linkedCard")
  }
}