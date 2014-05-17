package li.cil.oc

import cpw.mods.fml.common.registry.GameRegistry
import li.cil.oc.common.{Loot, item}
import li.cil.oc.util.mods.Mods
import net.minecraft.item.{Item, ItemBlock, ItemStack}
import scala.collection.mutable
import li.cil.oc.api.detail.{ItemAPI, ItemInfo}
import net.minecraft.block.Block
import li.cil.oc.common.recipe.Recipes
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.creativetab.CreativeTabs

object Items extends ItemAPI {
  private val descriptors = mutable.Map.empty[String, ItemInfo]

  private val names = mutable.Map.empty[Any, String]

  override def get(name: String): ItemInfo = descriptors.get(name).orNull

  override def get(stack: ItemStack) = names.get(getBlockOrItem(stack)) match {
    case Some(name) => get(name)
    case _ => null
  }

  def registerBlock[T <: common.block.Delegate](delegate: T, name: String) = {
    descriptors += name -> new ItemInfo {
      override def block = delegate.parent

      override def item = null

      override def createItemStack(size: Int) = delegate.createItemStack(size)
    }
    names += delegate -> name
    delegate
  }

  def registerBlock(instance: Block, name: String) = {
    descriptors += name -> new ItemInfo {
      override def block = instance

      override def item = null

      override def createItemStack(size: Int) = new ItemStack(instance, size)
    }
    names += instance -> name
    instance
  }

  def registerItem[T <: common.item.Delegate](delegate: T, name: String) = {
    descriptors += name -> new ItemInfo {
      override def block = null

      override def item = delegate.parent

      override def createItemStack(size: Int) = delegate.createItemStack(size)
    }
    names += delegate -> name
    delegate
  }

  def registerItem(instance: Item, name: String) = {
    descriptors += name -> new ItemInfo {
      override def block = null

      override def item = instance

      override def createItemStack(size: Int) = new ItemStack(instance, size)
    }
    names += instance -> name
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

    Recipes.addItem(new item.Memory(multi, 0), "ram1", "oc:ram1")
    Recipes.addItem(new item.Memory(multi, 2), "ram3", "oc:ram3")
    Recipes.addItem(new item.Memory(multi, 3), "ram4", "oc:ram4")

    Recipes.addItem(new item.FloppyDisk(multi), "floppy", "oc:floppy")
    Recipes.addItem(new item.HardDiskDrive(multi, 0), "hdd1", "oc:hdd1")
    Recipes.addItem(new item.HardDiskDrive(multi, 1), "hdd2", "oc:hdd2")
    Recipes.addItem(new item.HardDiskDrive(multi, 2), "hdd3", "oc:hdd3")

    Recipes.addItem(new item.GraphicsCard(multi, 0), "graphicsCard1", "oc:graphicsCard1")
    Recipes.addItem(new item.GraphicsCard(multi, 1), "graphicsCard2", "oc:graphicsCard2")
    Recipes.addItem(new item.GraphicsCard(multi, 2), "graphicsCard3", "oc:graphicsCard3")
    Recipes.addItem(new item.NetworkCard(multi), "lanCard", "oc:lanCard")
    Recipes.addItem(new item.RedstoneCard(multi), "redstoneCard", "oc:redstoneCard")
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
    Recipes.addItem(new item.Microchip(multi, 0), "chip1", "oc:circuitChip1")
    Recipes.addItem(new item.Microchip(multi, 1), "chip2", "oc:circuitChip2")
    Recipes.addItem(new item.Microchip(multi, 2), "chip3", "oc:circuitChip3")
    Recipes.addItem(new item.ALU(multi), "alu", "oc:materialALU")
    Recipes.addItem(new item.ControlUnit(multi), "cu", "oc:materialCU")
    Recipes.addItem(new item.CPU(multi, 0), "cpu1", "oc:cpu1")

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

    Recipes.addItem(new item.Memory(multi, 4), "ram5", "oc:ram5")
    Recipes.addItem(new item.Memory(multi, 5), "ram6", "oc:ram6")

    // v1.2.0
    Recipes.addItem(new item.Server(multi, 2), "server3", "oc:server3")
    Recipes.addItem(new item.Terminal(multi), "terminal", "oc:terminal")
    Recipes.addItem(new item.CPU(multi, 1), "cpu2", "oc:cpu2")
    Recipes.addItem(new item.CPU(multi, 2), "cpu3", "oc:cpu3")
    Recipes.addItem(new item.InternetCard(multi), "internetCard", "oc:internetCard")
    Recipes.addItem(new item.Server(multi, 0), "server1", "oc:server1")
    Recipes.addItem(new item.Server(multi, 1), "server2", "oc:server2")

    // v1.2.3
    registerItem(new item.FloppyDisk(multi) {
      showInItemList = false
    }, "lootDisk")

    // v1.2.6
    Recipes.addItem(new item.Interweb(multi), "interweb", "oc:materialInterweb")
    Recipes.addItem(new item.UpgradeAngel(multi), "angelUpgrade", "oc:angelUpgrade")
    Recipes.addItem(new item.Memory(multi, 1), "ram2", "oc:ram2")

    // v1.3.0
    Recipes.addItem(new item.LinkedCard(multi), "linkedCard", "oc:linkedCard")
    Recipes.addItem(new item.UpgradeExperience(multi), "experienceUpgrade", "oc:experienceUpgrade")
    Recipes.addItem(new item.UpgradeInventory(multi), "inventoryUpgrade", "oc:inventoryUpgrade")
    Recipes.addItem(new item.UpgradeContainerUpgrade(multi, 0), "upgradeContainer1", "oc:upgradeContainer1")
    Recipes.addItem(new item.UpgradeContainerUpgrade(multi, 1), "upgradeContainer2", "oc:upgradeContainer2")
    Recipes.addItem(new item.UpgradeContainerUpgrade(multi, 2), "upgradeContainer3", "oc:upgradeContainer3")
    Recipes.addItem(new item.UpgradeContainerCard(multi, 0), "cardContainer1", "oc:cardContainer1")
    Recipes.addItem(new item.UpgradeContainerCard(multi, 1), "cardContainer2", "oc:cardContainer2")
    Recipes.addItem(new item.UpgradeContainerCard(multi, 2), "cardContainer3", "oc:cardContainer3")

    // Special case loot disk because this one's craftable and having it have
    // the same item damage would confuse NEI and the item costs computation.
    Recipes.addItem(new item.FloppyDisk(multi) {
      override def createItemStack(amount: Int) = {
        val data = new NBTTagCompound()
        data.setString(Settings.namespace + "fs.label", "openos")

        val nbt = new NBTTagCompound("tag")
        nbt.setTag(Settings.namespace + "data", data)
        nbt.setString(Settings.namespace + "lootPath", "OpenOS")

        val stack = super.createItemStack(amount)
        stack.setTagCompound(nbt)

        stack
      }
    }, "openOS")
  }
}