package li.cil.oc

import cpw.mods.fml.common.registry.GameRegistry
import li.cil.oc.api.detail.{ItemAPI, ItemInfo}
import li.cil.oc.common.recipe.Recipes
import li.cil.oc.common.{Loot, Tier, item}
import li.cil.oc.util.Color
import li.cil.oc.util.mods.Mods
import net.minecraft.block.Block
import net.minecraft.creativetab.CreativeTabs
import net.minecraft.item.{Item, ItemBlock, ItemStack}
import net.minecraft.nbt.NBTTagCompound

import scala.collection.mutable

object Items extends ItemAPI {
  private val descriptors = mutable.Map.empty[String, ItemInfo]

  private val names = mutable.Map.empty[Any, String]

  override def get(name: String): ItemInfo = descriptors.get(name).orNull

  override def get(stack: ItemStack) = names.get(getBlockOrItem(stack)) match {
    case Some(name) => get(name)
    case _ => null
  }

  def registerBlock[T <: common.block.Delegate](delegate: T, id: String) = {
    descriptors += id -> new ItemInfo {
      override def name = id

      override def block = delegate.parent

      override def item = null

      override def createItemStack(size: Int) = delegate.createItemStack(size)
    }
    names += delegate -> id
    delegate
  }

  def registerBlock(instance: Block, id: String) = {
    descriptors += id -> new ItemInfo {
      override def name = id

      override def block = instance

      override def item = null

      override def createItemStack(size: Int) = new ItemStack(instance, size)
    }
    names += instance -> id
    instance
  }

  def registerItem[T <: common.item.Delegate](delegate: T, id: String) = {
    descriptors += id -> new ItemInfo {
      override def name = id

      override def block = null

      override def item = delegate.parent

      override def createItemStack(size: Int) = delegate.createItemStack(size)
    }
    names += delegate -> id
    delegate
  }

  def registerItem(instance: Item, id: String) = {
    descriptors += id -> new ItemInfo {
      override def name = id

      override def block = null

      override def item = instance

      override def createItemStack(size: Int) = new ItemStack(instance, size)
    }
    names += instance -> id
    instance
  }

  private def getBlockOrItem(stack: ItemStack): Any = if (stack == null) null
  else {
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
    multi = new item.Delegator(Settings.get.itemId) {
      override def getSubItems(itemId: Int, tab: CreativeTabs, list: java.util.List[_]) {
        // Workaround for MC's untyped lists...
        def add[T](list: java.util.List[T], value: Any) = list.add(value.asInstanceOf[T])
        super.getSubItems(itemId, tab, list)
        Loot.worldDisks.values.foreach(entry => add(list, entry._1))
      }
    }

    GameRegistry.registerItem(multi, Settings.namespace + "item")

    Recipes.addItem(new item.Analyzer(multi), "analyzer", "oc:analyzer")

    Recipes.addItem(new item.Memory(multi, Tier.One), "ram1", "oc:ram1")
    Recipes.addItem(new item.Memory(multi, Tier.Three), "ram3", "oc:ram3")
    Recipes.addItem(new item.Memory(multi, Tier.Four), "ram4", "oc:ram4")

    Recipes.addItem(new item.FloppyDisk(multi), "floppy", "oc:floppy")
    Recipes.addItem(new item.HardDiskDrive(multi, Tier.One), "hdd1", "oc:hdd1")
    Recipes.addItem(new item.HardDiskDrive(multi, Tier.Two), "hdd2", "oc:hdd2")
    Recipes.addItem(new item.HardDiskDrive(multi, Tier.Three), "hdd3", "oc:hdd3")

    Recipes.addItem(new item.GraphicsCard(multi, Tier.One), "graphicsCard1", "oc:graphicsCard1")
    Recipes.addItem(new item.GraphicsCard(multi, Tier.Two), "graphicsCard2", "oc:graphicsCard2")
    Recipes.addItem(new item.GraphicsCard(multi, Tier.Three), "graphicsCard3", "oc:graphicsCard3")
    Recipes.addItem(new item.NetworkCard(multi), "lanCard", "oc:lanCard")
    Recipes.addItem(new item.RedstoneCard(multi, Tier.Two), "redstoneCard2", "oc:redstoneCard2")
    Recipes.addItem(new item.WirelessNetworkCard(multi), "wlanCard", "oc:wlanCard")

    Recipes.addItem(new item.UpgradeCrafting(multi), "craftingUpgrade", "oc:craftingUpgrade")
    Recipes.addItem(new item.UpgradeGenerator(multi), "generatorUpgrade", "oc:generatorUpgrade")

    ironNugget = new item.IronNugget(multi)

    Recipes.addItem(new item.CuttingWire(multi), "cuttingWire", "oc:materialCuttingWire")
    Recipes.addItem(new item.Acid(multi), "acid", "oc:materialAcid")
    Recipes.addItem(new item.Disk(multi), "disk", "oc:materialDisk")

    Recipes.addItem(new item.ButtonGroup(multi), "buttonGroup", "oc:materialButtonGroup")
    Recipes.addItem(new item.ArrowKeys(multi), "arrowKeys", "oc:materialArrowKey")
    Recipes.addItem(new item.NumPad(multi), "numPad", "oc:materialNumPad")

    Recipes.addItem(new item.Transistor(multi), "transistor", "oc:materialTransistor")
    Recipes.addItem(new item.Microchip(multi, Tier.One), "chip1", "oc:circuitChip1")
    Recipes.addItem(new item.Microchip(multi, Tier.Two), "chip2", "oc:circuitChip2")
    Recipes.addItem(new item.Microchip(multi, Tier.Three), "chip3", "oc:circuitChip3")
    Recipes.addItem(new item.ALU(multi), "alu", "oc:materialALU")
    Recipes.addItem(new item.ControlUnit(multi), "cu", "oc:materialCU")
    Recipes.addItem(new item.CPU(multi, Tier.One), "cpu1", "oc:cpu1")

    Recipes.addItem(new item.RawCircuitBoard(multi), "rawCircuitBoard", "oc:materialCircuitBoardRaw")
    Recipes.addItem(new item.CircuitBoard(multi), "circuitBoard", "oc:materialCircuitBoard")
    Recipes.addItem(new item.PrintedCircuitBoard(multi), "printedCircuitBoard", "oc:materialCircuitBoardPrinted")
    Recipes.addItem(new item.CardBase(multi), "card", "oc:materialCard")

    // v1.1.0
    Recipes.addItem(new item.UpgradeSolarGenerator(multi), "solarGeneratorUpgrade", "oc:solarGeneratorUpgrade")
    Recipes.addItem(new item.UpgradeSign(multi), "signUpgrade", "oc:signUpgrade")
    Recipes.addItem(new item.UpgradeNavigation(multi), "navigationUpgrade", "oc:navigationUpgrade")

    // Always create, to avoid shifting IDs.
    val abstractBus = new item.AbstractBusCard(multi)
    if (Mods.StargateTech2.isAvailable) {
      Recipes.addItem(abstractBus, "abstractBusCard", "oc:abstractBusCard")
    }

    Recipes.addItem(new item.Memory(multi, Tier.Five), "ram5", "oc:ram5")
    Recipes.addItem(new item.Memory(multi, Tier.Six), "ram6", "oc:ram6")

    // v1.2.0
    Recipes.addItem(new item.Server(multi, Tier.Three), "server3", "oc:server3")
    Recipes.addItem(new item.Terminal(multi), "terminal", "oc:terminal")
    Recipes.addItem(new item.CPU(multi, Tier.Two), "cpu2", "oc:cpu2")
    Recipes.addItem(new item.CPU(multi, Tier.Three), "cpu3", "oc:cpu3")
    Recipes.addItem(new item.InternetCard(multi), "internetCard", "oc:internetCard")
    Recipes.addItem(new item.Server(multi, Tier.One), "server1", "oc:server1")
    Recipes.addItem(new item.Server(multi, Tier.Two), "server2", "oc:server2")

    // v1.2.3
    registerItem(new item.FloppyDisk(multi) {
      showInItemList = false
    }, "lootDisk")

    // v1.2.6
    Recipes.addItem(new item.Interweb(multi), "interweb", "oc:materialInterweb")
    Recipes.addItem(new item.UpgradeAngel(multi), "angelUpgrade", "oc:angelUpgrade")
    Recipes.addItem(new item.Memory(multi, Tier.Two), "ram2", "oc:ram2")

    // v1.3.0
    Recipes.addItem(new item.LinkedCard(multi), "linkedCard", "oc:linkedCard")
    Recipes.addItem(new item.UpgradeExperience(multi), "experienceUpgrade", "oc:experienceUpgrade")
    Recipes.addItem(new item.UpgradeInventory(multi), "inventoryUpgrade", "oc:inventoryUpgrade")
    Recipes.addItem(new item.UpgradeContainerUpgrade(multi, Tier.One), "upgradeContainer1", "oc:upgradeContainer1")
    Recipes.addItem(new item.UpgradeContainerUpgrade(multi, Tier.Two), "upgradeContainer2", "oc:upgradeContainer2")
    Recipes.addItem(new item.UpgradeContainerUpgrade(multi, Tier.Three), "upgradeContainer3", "oc:upgradeContainer3")
    Recipes.addItem(new item.UpgradeContainerCard(multi, Tier.One), "cardContainer1", "oc:cardContainer1")
    Recipes.addItem(new item.UpgradeContainerCard(multi, Tier.Two), "cardContainer2", "oc:cardContainer2")
    Recipes.addItem(new item.UpgradeContainerCard(multi, Tier.Three), "cardContainer3", "oc:cardContainer3")

    // Special case loot disk because this one's craftable and having it have
    // the same item damage would confuse NEI and the item costs computation.
    Recipes.addItem(new item.FloppyDisk(multi) {
      override def createItemStack(amount: Int) = {
        val data = new NBTTagCompound()
        data.setString(Settings.namespace + "fs.label", "openos")

        val nbt = new NBTTagCompound("tag")
        nbt.setTag(Settings.namespace + "data", data)
        nbt.setString(Settings.namespace + "lootPath", "OpenOS")
        nbt.setInteger(Settings.namespace + "color", Color.dyes.indexOf("dyeGreen"))

        val stack = super.createItemStack(amount)
        stack.setTagCompound(nbt)

        stack
      }
    }, "openOS")

    Recipes.addItem(new item.UpgradeInventoryController(multi), "inventoryControllerUpgrade", "oc:inventoryControllerUpgrade")
    Recipes.addItem(new item.UpgradeChunkloader(multi), "chunkloaderUpgrade", "oc:chunkloaderUpgrade")
    Recipes.addItem(new item.UpgradeBattery(multi, Tier.One), "batteryUpgrade1", "oc:batteryUpgrade1")
    Recipes.addItem(new item.UpgradeBattery(multi, Tier.Two), "batteryUpgrade2", "oc:batteryUpgrade2")
    Recipes.addItem(new item.UpgradeBattery(multi, Tier.Three), "batteryUpgrade3", "oc:batteryUpgrade3")
    Recipes.addItem(new item.RedstoneCard(multi, Tier.One), "redstoneCard1", "oc:redstoneCard1")

    // 1.3.2
    Recipes.addItem(new item.UpgradeTractorBeam(multi), "tractorBeamUpgrade", "oc:tractorBeamUpgrade")

    // Experimental
    registerItem(new item.Tablet(multi), "tablet")

    // 1.3.2 (cont.)
    registerItem(new item.Server(multi, Tier.Four), "serverCreative")

    // 1.3.3
    Recipes.addItem(new item.ComponentBus(multi, Tier.One), "componentBus1", "oc:componentBus1")
    Recipes.addItem(new item.ComponentBus(multi, Tier.Two), "componentBus2", "oc:componentBus2")
    Recipes.addItem(new item.ComponentBus(multi, Tier.Three), "componentBus3", "oc:componentBus3")
    registerItem(new item.DebugCard(multi), "debugCard")
  }
}