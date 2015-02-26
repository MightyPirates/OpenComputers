package li.cil.oc.common

import li.cil.oc.api.detail.ItemInfo
import li.cil.oc.common.init.Items
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.stats.StatBase
import net.minecraft.stats.{Achievement => MCAchievement}
import net.minecraftforge.common.AchievementPage

import scala.collection.mutable

object Achievement {
  val All = mutable.ArrayBuffer.empty[Achievement]

  val CraftingMap = mutable.Map.empty[ItemInfo, MCAchievement]

  val Transistor = new Achievement("oc.transistor", "oc.transistor",
    2, 0, Items.get("transistor").createItemStack(1), null, "transistor").initIndependentStat()
  val Disassembler = new Achievement("oc.disassembler", "oc.disassembler",
    2, 2, Items.get("disassembler").createItemStack(1), Transistor, "disassembler")
  val Microchip = new Achievement("oc.chip", "oc.chip",
    4, 0, Items.get("chip1").createItemStack(1), Transistor, "chip1", "chip2", "chip3")
  val Capacitor = new Achievement("oc.capacitor", "oc.capacitor",
    6, -1, Items.get("capacitor").createItemStack(1), Microchip, "capacitor")
  val Assembler = new Achievement("oc.assembler", "oc.assembler",
    8, -2, Items.get("assembler").createItemStack(1), Capacitor, "assembler")
  val Microcontroller = new Achievement("oc.microcontroller", "oc.microcontroller",
    10, -2, Items.get("microcontroller").createItemStack(1), Assembler)
  val Robot = new Achievement("oc.robot", "oc.robot",
    10, -3, Items.get("robot").createItemStack(1), Assembler)
  val Drone = new Achievement("oc.drone", "oc.drone",
    10, -4, Items.get("drone").createItemStack(1), Assembler)
  val Tablet = new Achievement("oc.tablet", "oc.tablet",
    10, -5, Items.get("tablet").createItemStack(1), Assembler)
  val Charger = new Achievement("oc.charger", "oc.charger",
    8, -1, Items.get("charger").createItemStack(1), Capacitor, "charger")
  val CPU = new Achievement("oc.cpu", "oc.cpu",
    6, 0, Items.get("cpu1").createItemStack(1), Microchip, "cpu1", "cpu2", "cpu3")
  val MotionSensor = new Achievement("oc.motionSensor", "oc.motionSensor",
    8, 0, Items.get("motionSensor").createItemStack(1), CPU, "motionSensor")
  val Geolyzer = new Achievement("oc.geolyzer", "oc.geolyzer",
    8, 1, Items.get("geolyzer").createItemStack(1), CPU, "geolyzer")
  val RedstoneIO = new Achievement("oc.redstoneIO", "oc.redstoneIO",
    8, 2, Items.get("redstone").createItemStack(1), CPU, "redstone")
  val EEPROM = new Achievement("oc.eeprom", "oc.eeprom",
    6, 3, Items.get("eeprom").createItemStack(1), Microchip, "eeprom")
  val Memory = new Achievement("oc.ram", "oc.ram",
    6, 4, Items.get("ram1").createItemStack(1), Microchip, "ram1", "ram2", "ram3", "ram4", "ram5", "ram6")
  val HDD = new Achievement("oc.hdd", "oc.hdd",
    6, 5, Items.get("hdd1").createItemStack(1), Microchip, "hdd1", "hdd2", "hdd3")
  val Case = new Achievement("oc.case", "oc.case",
    6, 6, Items.get("case1").createItemStack(1), Microchip, "case1", "case2", "case3")
  val Rack = new Achievement("oc.rack", "oc.rack",
    8, 6, Items.get("serverRack").createItemStack(1), Case, "serverRack")
  val Server = new Achievement("oc.server", "oc.server",
    10, 6, Items.get("server1").createItemStack(1), Rack, "server1", "server2", "server3")
  val Screen = new Achievement("oc.screen", "oc.screen",
    6, 7, Items.get("screen1").createItemStack(1), Microchip, "screen1", "screen2", "screen3")
  val Keyboard = new Achievement("oc.keyboard", "oc.keyboard",
    8, 7, Items.get("keyboard").createItemStack(1), Screen, "keyboard")
  val Hologram = new Achievement("oc.hologram", "oc.hologram",
    8, 8, Items.get("hologram1").createItemStack(1), Screen, "hologram1", "hologram2")
  val DiskDrive = new Achievement("oc.diskDrive", "oc.diskDrive",
    6, 9, Items.get("diskDrive").createItemStack(1), Microchip, "diskDrive")
  val Floppy = new Achievement("oc.floppy", "oc.floppy",
    8, 9, Items.get("floppy").createItemStack(1), DiskDrive, "floppy")
  val OpenOS = new Achievement("oc.openOS", "oc.openOS",
    10, 9, Items.createOpenOS(), Floppy)
  val Raid = new Achievement("oc.raid", "oc.raid",
    8, 10, Items.get("raid").createItemStack(1), DiskDrive, "raid")

  val Card = new Achievement("oc.card", "oc.card",
    0, -2, Items.get("card").createItemStack(1), null, "card").initIndependentStat()
  val RedstoneCard = new Achievement("oc.redstoneCard", "oc.redstoneCard",
    -2, -4, Items.get("redstoneCard1").createItemStack(1), Card, "redstoneCard1", "redstoneCard2")
  val GraphicsCard = new Achievement("oc.graphicsCard", "oc.graphicsCard",
    0, -5, Items.get("graphicsCard1").createItemStack(1), Card, "graphicsCard1", "graphicsCard2", "graphicsCard3")
  val NetworkCard = new Achievement("oc.networkCard", "oc.networkCard",
    2, -4, Items.get("lanCard").createItemStack(1), Card, "lanCard")
  val WirelessNetworkCard = new Achievement("oc.wirelessNetworkCard", "oc.wirelessNetworkCard",
    2, -6, Items.get("wlanCard").createItemStack(1), NetworkCard, "wlanCard")

  val Cable = new Achievement("oc.cable", "oc.cable",
    -2, 0, Items.get("cable").createItemStack(1), null, "cable").initIndependentStat()
  val PowerDistributor = new Achievement("oc.powerDistributor", "oc.powerDistributor",
    -4, -1, Items.get("powerDistributor").createItemStack(1), Cable, "powerDistributor")
  val Switch = new Achievement("oc.switch", "oc.switch",
    -4, 0, Items.get("switch").createItemStack(1), Cable, "switch", "accessPoint")
  val Adapter = new Achievement("oc.adapter", "oc.adapter",
    -4, 1, Items.get("adapter").createItemStack(1), Cable, "adapter")

  def init() {
    // Missing @Override causes ambiguity, so cast is required; still a virtual call,
    // so Achievement.registerStat is still the method that's really being called.
    All.foreach(_.asInstanceOf[StatBase].registerStat())
    AchievementPage.registerAchievementPage(new AchievementPage("OpenComputers", All: _*))
  }

  def onAssemble(stack: ItemStack, player: EntityPlayer): Unit = {
    val descriptor = Items.get(stack)
    if (descriptor == Items.get("microcontroller")) player.addStat(Microcontroller, 1)
    if (descriptor == Items.get("robot")) player.addStat(Robot, 1)
    if (descriptor == Items.get("drone")) player.addStat(Drone, 1)
    if (descriptor == Items.get("tablet")) player.addStat(Tablet, 1)
  }

  def onCraft(stack: ItemStack, player: EntityPlayer): Unit = {
    CraftingMap.get(Items.get(stack)).foreach(player.addStat(_, 1))

    if (ItemStack.areItemStacksEqual(stack, Items.createOpenOS(stack.stackSize))) {
      player.addStat(OpenOS, 1)
    }
  }
}

class Achievement(name: String, description: String, x: Int, y: Int, stack: ItemStack, parent: MCAchievement, requirements: String*) extends MCAchievement(name, description, x, y, stack, parent) {
  Achievement.All += this

  for (requirement <- requirements) {
    val descriptor = Items.get(requirement)
    if (descriptor != null) {
      Achievement.CraftingMap += descriptor -> this
    }
  }
}
