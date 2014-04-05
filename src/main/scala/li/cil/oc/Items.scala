package li.cil.oc

import cpw.mods.fml.common.registry.GameRegistry
import li.cil.oc.common.item
import li.cil.oc.util.mods.Mods
import net.minecraft.block.Block
import net.minecraft.item.{Item, ItemStack}
import net.minecraftforge.oredict.OreDictionary
import scala.collection.convert.WrapAsScala._

object Items {
  var multi: item.Delegator = _

  // ----------------------------------------------------------------------- //
  // Tools
  var analyzer: item.Analyzer = _
  var terminal: item.Terminal = _

  // ----------------------------------------------------------------------- //
  // Servers
  var server1, server2, server3: item.Server = _

  // ----------------------------------------------------------------------- //
  // Memory
  var ram1, ram2, ram3, ram4, ram5: item.Memory = _

  // ----------------------------------------------------------------------- //
  // Storage
  var floppyDisk, lootDisk: item.FloppyDisk = _
  var hdd1, hdd2, hdd3: item.HardDiskDrive = _

  // ----------------------------------------------------------------------- //
  // Cards
  var abstractBus: item.AbstractBusCard = _
  var gpu1, gpu2, gpu3: item.GraphicsCard = _
  var internet: item.InternetCard = _
  var lan: item.NetworkCard = _
  var rs: item.RedstoneCard = _
  var wlan: item.WirelessNetworkCard = _

  // ----------------------------------------------------------------------- //
  // Upgrades
  var upgradeCrafting: item.UpgradeCrafting = _
  var upgradeGenerator: item.UpgradeGenerator = _
  var upgradeNavigation: item.UpgradeNavigation = _
  var upgradeSign: item.UpgradeSign = _
  var upgradeSolarGenerator: item.UpgradeSolarGenerator = _
  var upgradeBlockPlacerAir: item.UpgradeBlockPlacerAir = _

  // ----------------------------------------------------------------------- //
  // Crafting
  var ironNugget: item.IronNugget = _
  var cuttingWire: item.CuttingWire = _
  var acid: item.Acid = _
  var disk: item.Disk = _

  var buttonGroup: item.ButtonGroup = _
  var arrowKeys: item.ArrowKeys = _
  var numPad: item.NumPad = _

  var transistor: item.Transistor = _
  var chip1, chip2, chip3: item.Microchip = _
  var alu: item.ALU = _
  var cpu0, cpu1, cpu2: item.CPU = _
  var cu: item.ControlUnit = _

  var rawCircuitBoard: item.RawCircuitBoard = _
  var circuitBoard: item.CircuitBoard = _
  var pcb: item.PrintedCircuitBoard = _
  var card: item.CardBase = _

  var interweb: item.Interweb = _

  def init() {
    multi = new item.Delegator(Settings.get.itemId)

    GameRegistry.registerItem(multi, Settings.namespace + "item")

    analyzer = Recipes.addItemDelegate(new item.Analyzer(multi), "analyzer")

    ram1 = Recipes.addItemDelegate(new item.Memory(multi, 0), "ram1")
    ram2 = Recipes.addItemDelegate(new item.Memory(multi, 1), "ram2")
    ram3 = Recipes.addItemDelegate(new item.Memory(multi, 2), "ram3")

    floppyDisk = Recipes.addItemDelegate(new item.FloppyDisk(multi), "floppy")
    hdd1 = Recipes.addItemDelegate(new item.HardDiskDrive(multi, 0), "hdd1")
    hdd2 = Recipes.addItemDelegate(new item.HardDiskDrive(multi, 1), "hdd2")
    hdd3 = Recipes.addItemDelegate(new item.HardDiskDrive(multi, 2), "hdd3")

    gpu1 = Recipes.addItemDelegate(new item.GraphicsCard(multi, 0), "graphicsCard1")
    gpu2 = Recipes.addItemDelegate(new item.GraphicsCard(multi, 1), "graphicsCard2")
    gpu3 = Recipes.addItemDelegate(new item.GraphicsCard(multi, 2), "graphicsCard3")
    lan = Recipes.addItemDelegate(new item.NetworkCard(multi), "lanCard")
    rs = Recipes.addItemDelegate(new item.RedstoneCard(multi), "redstoneCard")
    wlan = Recipes.addItemDelegate(new item.WirelessNetworkCard(multi), "wlanCard")

    upgradeCrafting = Recipes.addItemDelegate(new item.UpgradeCrafting(multi), "craftingUpgrade")
    upgradeGenerator = Recipes.addItemDelegate(new item.UpgradeGenerator(multi), "generatorUpgrade")
    upgradeBlockPlacerAir = Recipes.addItemDelegate(new item.UpgradeBlockPlacerAir(multi), "blockPlacerAirUpgrade")

    ironNugget = new item.IronNugget(multi)

    cuttingWire = Recipes.addItemDelegate(new item.CuttingWire(multi), "cuttingWire")
    acid = Recipes.addItemDelegate(new item.Acid(multi), "acid")
    disk = Recipes.addItemDelegate(new item.Disk(multi), "disk")

    buttonGroup = Recipes.addItemDelegate(new item.ButtonGroup(multi), "buttonGroup")
    arrowKeys = Recipes.addItemDelegate(new item.ArrowKeys(multi), "arrowKeys")
    numPad = Recipes.addItemDelegate(new item.NumPad(multi), "numPad")

    transistor = Recipes.addItemDelegate(new item.Transistor(multi), "transistor")
    chip1 = Recipes.addItemDelegate(new item.Microchip(multi, 0), "chip1")
    chip2 = Recipes.addItemDelegate(new item.Microchip(multi, 1), "chip2")
    chip3 = Recipes.addItemDelegate(new item.Microchip(multi, 2), "chip3")
    alu = Recipes.addItemDelegate(new item.ALU(multi), "alu")
    cu = Recipes.addItemDelegate(new item.ControlUnit(multi), "cu")
    cpu0 = Recipes.addItemDelegate(new item.CPU(multi, 0), "cpu0")

    rawCircuitBoard = Recipes.addItemDelegate(new item.RawCircuitBoard(multi), "rawCircuitBoard")
    circuitBoard = Recipes.addItemDelegate(new item.CircuitBoard(multi), "circuitBoard")
    pcb = Recipes.addItemDelegate(new item.PrintedCircuitBoard(multi), "printedCircuitBoard")
    card = Recipes.addItemDelegate(new item.CardBase(multi), "card")

    // v1.1.0
    upgradeSolarGenerator = Recipes.addItemDelegate(new item.UpgradeSolarGenerator(multi), "solarGeneratorUpgrade")
    upgradeSign = Recipes.addItemDelegate(new item.UpgradeSign(multi), "signUpgrade")
    upgradeNavigation = Recipes.addItemDelegate(new item.UpgradeNavigation(multi), "navigationUpgrade")

    abstractBus = new item.AbstractBusCard(multi)
    if (Mods.StargateTech2.isAvailable) {
      Recipes.addItemDelegate(abstractBus, "abstractBusCard")
    }

    ram4 = Recipes.addItemDelegate(new item.Memory(multi, 3), "ram4")
    ram5 = Recipes.addItemDelegate(new item.Memory(multi, 4), "ram5")

    // v1.2.0
    server3 = Recipes.addItemDelegate(new item.Server(multi, 2), "server3")
    terminal = Recipes.addItemDelegate(new item.Terminal(multi), "terminal")
    cpu1 = Recipes.addItemDelegate(new item.CPU(multi, 1), "cpu1")
    cpu2 = Recipes.addItemDelegate(new item.CPU(multi, 2), "cpu2")
    internet = Recipes.addItemDelegate(new item.InternetCard(multi), "internetCard")
    server1 = Recipes.addItemDelegate(new item.Server(multi, 0), "server1")
    server2 = Recipes.addItemDelegate(new item.Server(multi, 1), "server2")

    // v1.2.3
    lootDisk = new item.FloppyDisk(multi) {
      showInItemList = false
    }

    // v1.2.6
    interweb = Recipes.addItemDelegate(new item.Interweb(multi), "interweb")

    // Initialize API.
    api.Items.AbstractBusCard = abstractBus.createItemStack()
    api.Items.Acid = acid.createItemStack()
    api.Items.ALU = alu.createItemStack()
    api.Items.Analyzer = analyzer.createItemStack()
    api.Items.ButtonArrows = arrowKeys.createItemStack()
    api.Items.ButtonGroup = buttonGroup.createItemStack()
    api.Items.ButtonNumPad = numPad.createItemStack()
    api.Items.CardBase = card.createItemStack()
    api.Items.CircuitBoard = circuitBoard.createItemStack()
    api.Items.ControlUnit = cu.createItemStack()
    api.Items.CPUTier1 = cpu0.createItemStack()
    api.Items.CPUTier2 = cpu1.createItemStack()
    api.Items.CPUTier3 = cpu2.createItemStack()
    api.Items.CuttingWire = cuttingWire.createItemStack()
    api.Items.DiskPlatter = disk.createItemStack()
    api.Items.FloppyDisk = floppyDisk.createItemStack()
    api.Items.GraphicsCardTier1 = gpu1.createItemStack()
    api.Items.GraphicsCardTier2 = gpu2.createItemStack()
    api.Items.GraphicsCardTier3 = gpu3.createItemStack()
    api.Items.HardDiskTier2 = hdd1.createItemStack()
    api.Items.HardDiskTier3 = hdd2.createItemStack()
    api.Items.HardDriveTier1 = hdd3.createItemStack()
    api.Items.InternetCard = internet.createItemStack()
    api.Items.IronNugget = ironNugget.createItemStack()
    api.Items.MemoryTier1 = ram1.createItemStack()
    api.Items.MemoryTier2 = ram2.createItemStack()
    api.Items.MemoryTier3 = ram3.createItemStack()
    api.Items.MemoryTier4 = ram4.createItemStack()
    api.Items.MemoryTier5 = ram5.createItemStack()
    api.Items.MicrochipTier1 = chip1.createItemStack()
    api.Items.MicroChipTier2 = chip2.createItemStack()
    api.Items.MicroChipTier3 = chip3.createItemStack()
    api.Items.NetworkCard = lan.createItemStack()
    api.Items.PrintedCircuitBoard = pcb.createItemStack()
    api.Items.RawCircuitBoard = rawCircuitBoard.createItemStack()
    api.Items.RedstoneCard = rs.createItemStack()
    api.Items.ServerTier1 = server1.createItemStack()
    api.Items.ServerTier2 = server2.createItemStack()
    api.Items.ServerTier3 = server3.createItemStack()
    api.Items.Terminal = terminal.createItemStack()
    api.Items.Transistor = transistor.createItemStack()
    api.Items.UpgradeCrafting = upgradeCrafting.createItemStack()
    api.Items.UpgradeGenerator = upgradeGenerator.createItemStack()
    api.Items.UpgradeNavigation = upgradeNavigation.createItemStack()
    api.Items.UpgradeSign = upgradeSign.createItemStack()
    api.Items.UpgradeSolarGenerator = upgradeSolarGenerator.createItemStack()
    api.Items.WirelessNetworkCard = wlan.createItemStack()

    // ----------------------------------------------------------------------- //

    registerExclusive("craftingPiston", new ItemStack(Block.pistonBase), new ItemStack(Block.pistonStickyBase))
    registerExclusive("nuggetGold", new ItemStack(Item.goldNugget))
    registerExclusive("nuggetIron", ironNugget.createItemStack())

    register("oc:craftingRAMTier1", ram1.createItemStack())
    register("oc:craftingRAMTier2", ram2.createItemStack())
    register("oc:craftingRAMTier3", ram3.createItemStack())
    register("oc:craftingRAMTier4", ram4.createItemStack())
    register("oc:craftingRAMTier5", ram5.createItemStack())

    register("oc:craftingHDDTier1", hdd1.createItemStack())
    register("oc:craftingHDDTier2", hdd2.createItemStack())
    register("oc:craftingHDDTier3", hdd3.createItemStack())

    register("oc:craftingGPUTier1", gpu1.createItemStack())
    register("oc:craftingGPUTier2", gpu2.createItemStack())
    register("oc:craftingGPUTier3", gpu3.createItemStack())

    register("oc:componentCardLan", lan.createItemStack())
    register("oc:componentCardRedstone", rs.createItemStack())
    register("oc:componentCardWLan", wlan.createItemStack())

    register("oc:craftingGenerator", upgradeGenerator.createItemStack())
    register("oc:craftingSolarGenerator", upgradeSolarGenerator.createItemStack())

    register("oc:craftingWire", cuttingWire.createItemStack())
    register("oc:craftingAcid", acid.createItemStack())
    register("oc:craftingDisk", disk.createItemStack())

    register("oc:craftingButtonGroup", buttonGroup.createItemStack())
    register("oc:craftingArrowKey", arrowKeys.createItemStack())
    register("oc:craftingNumPad", numPad.createItemStack())

    register("oc:craftingTransistor", transistor.createItemStack())
    register("oc:circuitTier1", chip1.createItemStack())
    register("oc:circuitTier2", chip2.createItemStack())
    register("oc:circuitTier3", chip3.createItemStack())
    register("oc:craftingALU", alu.createItemStack())
    register("oc:craftingCU", cu.createItemStack())
    register("oc:craftingCPUTier1", cpu0.createItemStack())
    register("oc:craftingCPUTier2", cpu1.createItemStack())
    register("oc:craftingCPUTier3", cpu2.createItemStack())

    register("oc:craftingCircuitBoardRaw", rawCircuitBoard.createItemStack())
    register("oc:craftingCircuitBoard", circuitBoard.createItemStack())
    register("oc:craftingCircuitBoardPrinted", pcb.createItemStack())
    register("oc:craftingCard", card.createItemStack())

    register("oc:craftingInterweb", interweb.createItemStack())


    if (OreDictionary.getOres("nuggetIron").exists(ironNugget.createItemStack().isItemEqual)) {
      Recipes.addItemDelegate(ironNugget, "nuggetIron")
      Recipes.addItem(Item.ingotIron, "ingotIron")
    }
  }

  def register(name: String, item: ItemStack) {
    if (!OreDictionary.getOres(name).contains(item)) {
      OreDictionary.registerOre(name, item)
    }
  }

  def registerExclusive(name: String, items: ItemStack*) {
    if (OreDictionary.getOres(name).isEmpty) {
      for (item <- items) {
        OreDictionary.registerOre(name, item)
      }
    }
  }
}