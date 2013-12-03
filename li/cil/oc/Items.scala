package li.cil.oc

import cpw.mods.fml.common.registry.GameRegistry
import li.cil.oc.common.item
import net.minecraftforge.oredict.OreDictionary

object Items {
  var multi: item.Delegator = null

  // ----------------------------------------------------------------------- //
  // Tools
  var analyzer: item.Analyzer = null

  // ----------------------------------------------------------------------- //
  // Memory
  var ram1, ram2, ram3: item.Memory = null

  // ----------------------------------------------------------------------- //
  // Storage
  var floppyDisk: item.FloppyDisk = null
  var hdd1, hdd2, hdd3: item.HardDiskDrive = null

  // ----------------------------------------------------------------------- //
  // Cards
  var gpu1, gpu2, gpu3: item.GraphicsCard = null
  var lan: item.NetworkCard = null
  var rs: item.RedstoneCard = null
  var wlan: item.WirelessNetworkCard = null

  // ----------------------------------------------------------------------- //
  // Upgrades
  var crafting: item.Crafting = null
  var generator: item.Generator = null

  // ----------------------------------------------------------------------- //
  // Crafting
  var acid: item.Acid = null
  var alu: item.ALU = null
  var arrowKeys: item.ArrowKeys = null
  var buttonGroup: item.ButtonGroup = null
  var card: item.CardBase = null
  var chip1, chip2, chip3: item.Microchip = null
  var circuitBoard: item.CircuitBoard = null
  var cpu: item.CPU = null
  var cu: item.ControlUnit = null
  var cuttingWire: item.CuttingWire = null
  var disk: item.Disk = null
  var ironNugget: item.IronNugget = null
  var numPad: item.NumPad = null
  var pcb: item.PrintedCircuitBoard = null
  var rawCircuitBoard: item.RawCircuitBoard = null
  var transistor: item.Transistor = null

  def init() {
    multi = new item.Delegator(Settings.get.itemId)

    GameRegistry.registerItem(multi, Settings.namespace + "item")

    analyzer = new item.Analyzer(multi)
    floppyDisk = new item.FloppyDisk(multi)
    gpu1 = new item.GraphicsCard(multi, 0)
    gpu2 = new item.GraphicsCard(multi, 1)
    gpu3 = new item.GraphicsCard(multi, 2)
    hdd1 = new item.HardDiskDrive(multi, 0)
    hdd2 = new item.HardDiskDrive(multi, 1)
    hdd3 = new item.HardDiskDrive(multi, 2)
    lan = new item.NetworkCard(multi)
    generator = new item.Generator(multi)
    ram1 = new item.Memory(multi, 0)
    ram2 = new item.Memory(multi, 1)
    ram3 = new item.Memory(multi, 2)
    rs = new item.RedstoneCard(multi)
    wlan = new item.WirelessNetworkCard(multi)
    crafting = new item.Crafting(multi)

    acid = new item.Acid(multi)
    alu = new item.ALU(multi)
    arrowKeys = new item.ArrowKeys(multi)
    card = new item.CardBase(multi)
    circuitBoard = new item.CircuitBoard(multi)
    pcb = new item.PrintedCircuitBoard(multi)
    cuttingWire = new item.CuttingWire(multi)

    chip1 = new item.Microchip(multi, 0)
    chip2 = new item.Microchip(multi, 1)
    chip3 = new item.Microchip(multi, 2)

    numPad = new item.NumPad(multi)
    buttonGroup = new item.ButtonGroup(multi)

    cpu = new item.CPU(multi)
    transistor = new item.Transistor(multi)
    cu = new item.ControlUnit(multi)

    ironNugget = new item.IronNugget(multi)
    OreDictionary.registerOre("nuggetIron", ironNugget.createItemStack())

    rawCircuitBoard = new item.RawCircuitBoard(multi)
    disk = new item.Disk(multi)
  }
}