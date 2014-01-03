package li.cil.oc

import cpw.mods.fml.common.registry.GameRegistry
import li.cil.oc.common.item
import net.minecraft.item.ItemStack
import net.minecraftforge.oredict.OreDictionary

object Items {
  var multi: item.Delegator = _

  // ----------------------------------------------------------------------- //
  // Tools
  var analyzer: item.Analyzer = _

  // ----------------------------------------------------------------------- //
  // Memory
  var ram1, ram2, ram3: item.Memory = _

  // ----------------------------------------------------------------------- //
  // Storage
  var floppyDisk: item.FloppyDisk = _
  var hdd1, hdd2, hdd3: item.HardDiskDrive = _

  // ----------------------------------------------------------------------- //
  // Cards
  var abstractBus: item.AbstractBusCard = _
  var gpu1, gpu2, gpu3: item.GraphicsCard = _
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
  var cpu: item.CPU = _
  var cu: item.ControlUnit = _

  var rawCircuitBoard: item.RawCircuitBoard = _
  var circuitBoard: item.CircuitBoard = _
  var pcb: item.PrintedCircuitBoard = _
  var card: item.CardBase = _

  def init() {
    multi = new item.Delegator(Settings.get.itemId)

    GameRegistry.registerItem(multi, Settings.namespace + "item")

    analyzer = new item.Analyzer(multi)

    ram1 = new item.Memory(multi, 0)
    ram2 = new item.Memory(multi, 1)
    ram3 = new item.Memory(multi, 2)

    floppyDisk = new item.FloppyDisk(multi)
    hdd1 = new item.HardDiskDrive(multi, 0)
    hdd2 = new item.HardDiskDrive(multi, 1)
    hdd3 = new item.HardDiskDrive(multi, 2)

    gpu1 = new item.GraphicsCard(multi, 0)
    gpu2 = new item.GraphicsCard(multi, 1)
    gpu3 = new item.GraphicsCard(multi, 2)
    lan = new item.NetworkCard(multi)
    rs = new item.RedstoneCard(multi)
    wlan = new item.WirelessNetworkCard(multi)

    upgradeCrafting = new item.UpgradeCrafting(multi)
    upgradeGenerator = new item.UpgradeGenerator(multi)

    ironNugget = new item.IronNugget(multi)
    cuttingWire = new item.CuttingWire(multi)
    acid = new item.Acid(multi)
    disk = new item.Disk(multi)

    buttonGroup = new item.ButtonGroup(multi)
    arrowKeys = new item.ArrowKeys(multi)
    numPad = new item.NumPad(multi)

    transistor = new item.Transistor(multi)
    chip1 = new item.Microchip(multi, 0)
    chip2 = new item.Microchip(multi, 1)
    chip3 = new item.Microchip(multi, 2)
    alu = new item.ALU(multi)
    cu = new item.ControlUnit(multi)
    cpu = new item.CPU(multi)

    rawCircuitBoard = new item.RawCircuitBoard(multi)
    circuitBoard = new item.CircuitBoard(multi)
    pcb = new item.PrintedCircuitBoard(multi)
    card = new item.CardBase(multi)

    // v1.1.0
    upgradeSolarGenerator = new item.UpgradeSolarGenerator(multi)
    upgradeSign = new item.UpgradeSign(multi)
    upgradeNavigation = new item.UpgradeNavigation(multi)

    abstractBus = new item.AbstractBusCard(multi)

    // ----------------------------------------------------------------------- //

    registerExclusive("nuggetIron", ironNugget.createItemStack())
    register("oc:craftingCircuitBoardRaw", rawCircuitBoard.createItemStack())
    register("oc:craftingCircuitBoard", circuitBoard.createItemStack())
    register("oc:craftingCircuitBoardPrinted", pcb.createItemStack())
    register("oc:craftingWire", cuttingWire.createItemStack())
    register("oc:circuitBasic", chip1.createItemStack())
    register("oc:circuitAdvanced", chip2.createItemStack())
    register("oc:circuitElite", chip3.createItemStack())
    register("oc:craftingTransistor", transistor.createItemStack())
    register("oc:craftingCU", cu.createItemStack())
    register("oc:craftingALU", alu.createItemStack())
    register("oc:craftingCPU", cpu.createItemStack())
    register("oc:componentCardRedstone", rs.createItemStack())
    register("oc:componentCardLan", lan.createItemStack())
    register("oc:craftingGPUBasic", gpu1.createItemStack())
    register("oc:craftingGPUAdvanced", gpu2.createItemStack())
    register("oc:craftingGPUElite", gpu3.createItemStack())
    register("oc:craftingRAMBasic", ram1.createItemStack())
    register("oc:craftingRAMAdvanced", ram2.createItemStack())
    register("oc:craftingRAMElite", ram3.createItemStack())
    register("oc:craftingHDDBasic", hdd1.createItemStack())
    register("oc:craftingHDDAdvanced", hdd2.createItemStack())
    register("oc:craftingHDDElite", hdd3.createItemStack())
    register("oc:craftingButtonGroup", buttonGroup.createItemStack())
    register("oc:craftingArrowKey", arrowKeys.createItemStack())
    register("oc:craftingNumPad", numPad.createItemStack())
    register("oc:craftingDisk", disk.createItemStack())
    register("oc:craftingAcid", acid.createItemStack())
    register("oc:craftingGenerator", upgradeGenerator.createItemStack())
  }

  def register(name: String, item: ItemStack) {
    if (!OreDictionary.getOres(name).contains(item)) {
      OreDictionary.registerOre(name, item)
    }
  }

  def registerExclusive(name: String, item: ItemStack) {
    if (OreDictionary.getOres(name).isEmpty) {
      OreDictionary.registerOre(name, item)
    }
  }
}