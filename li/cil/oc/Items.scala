package li.cil.oc

import cpw.mods.fml.common.registry.GameRegistry
import li.cil.oc.common.item
import net.minecraft.item.{Item, ItemStack}
import net.minecraftforge.oredict.OreDictionary
import li.cil.oc.common.item.Disc

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
  var card: item.Card = null
  var circuitBoardBody: item.CircuitBoard = null
  var printedCircuitBoard: item.PrintedCircuitBoard = null
  var ironCutter: item.CuttingWire = null
  var chip1, chip2, chip3: item.Chip = null
  var numPad: item.NumPad = null
  var arrowKeys: item.ArrowKeys = null
  var buttonGroup: item.ButtonGroup = null
  var cpu: item.CPU = null
  var transistor: item.Transistor = null
  var alu: item.ALU = null
  var cu: item.ControlUnit = null

  var ironNugget: item.IronNugget = null
  var rawCircuitBoard: item.RawCircuitBoard = null
  var disc: item.Disc = null


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

    card = new item.Card(multi)
    circuitBoardBody = new item.CircuitBoard(multi)
    printedCircuitBoard = new item.PrintedCircuitBoard(multi)
    ironCutter = new item.CuttingWire(multi)


    chip1 = new item.Chip(multi, 0)
    chip2 = new item.Chip(multi, 1)
    chip3 = new item.Chip(multi, 2)

    numPad = new item.NumPad(multi)
    arrowKeys = new item.ArrowKeys(multi)
    buttonGroup = new item.ButtonGroup(multi)

    cpu = new item.CPU(multi)
    transistor = new item.Transistor(multi)
    alu = new item.ALU(multi)
    cu = new item.ControlUnit(multi)

    ironNugget = new item.IronNugget(multi)
    OreDictionary.registerOre("nuggetIron", ironNugget.createItemStack())
    OreDictionary.registerOre("potionPoison", new ItemStack(Item.potion, 1, 8196))
    OreDictionary.registerOre("potionPoison", new ItemStack(Item.potion, 1, 8228))
    OreDictionary.registerOre("potionPoison", new ItemStack(Item.potion, 1, 8260))
    OreDictionary.registerOre("potionPoison", new ItemStack(Item.potion, 1, 16388))
    OreDictionary.registerOre("potionPoison", new ItemStack(Item.potion, 1, 16420))
    OreDictionary.registerOre("potionPoison", new ItemStack(Item.potion, 1, 16452))

    rawCircuitBoard = new item.RawCircuitBoard(multi)
    disc = new item.Disc(multi)
  }
}