package li.cil.oc

import cpw.mods.fml.common.registry.GameRegistry
import li.cil.oc.common.item

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
  var disk: item.Disk = null
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
  var card1,card2,card3: item.Card = null
  var circuitBoardBody: item.PlatineBody = null
  var circuitBoard: item.Platine = null
  var ironCutter: item.IronCutter = null
  var chip1,chip2,chip3 :item.Chip = null


  def init() {
    multi = new item.Delegator(Settings.get.itemId)

    GameRegistry.registerItem(multi, Settings.namespace + "item")

    analyzer = new item.Analyzer(multi)
    disk = new item.Disk(multi)
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

    card1 = new item.Card(multi,0)
    card2 = new item.Card(multi,1)
    card3 = new item.Card(multi,2)
    circuitBoardBody = new item.PlatineBody(multi)
    circuitBoard = new item.Platine(multi)
    ironCutter = new item.IronCutter(multi)


    chip1 = new item.Chip(multi,0)
    chip2 = new item.Chip(multi,1)
    chip3 = new item.Chip(multi,2)
  }
}