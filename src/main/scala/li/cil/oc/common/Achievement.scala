package li.cil.oc.common

import li.cil.oc.Constants
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
    2, 0, Items.get(Constants.ItemName.Transistor).createItemStack(1), null, Constants.ItemName.Transistor).setIndependent()
  val Disassembler = new Achievement("oc.disassembler", "oc.disassembler",
    2, 2, Items.get(Constants.BlockName.Disassembler).createItemStack(1), Transistor, Constants.BlockName.Disassembler)
  val Microchip = new Achievement("oc.chip", "oc.chip",
    4, 0, Items.get(Constants.ItemName.Chip1).createItemStack(1), Transistor, Constants.ItemName.Chip1, Constants.ItemName.Chip2, Constants.ItemName.Chip3)
  val Capacitor = new Achievement("oc.capacitor", "oc.capacitor",
    6, -1, Items.get(Constants.BlockName.Capacitor).createItemStack(1), Microchip, Constants.BlockName.Capacitor)
  val Assembler = new Achievement("oc.assembler", "oc.assembler",
    8, -2, Items.get(Constants.BlockName.Assembler).createItemStack(1), Capacitor, Constants.BlockName.Assembler)
  val Microcontroller = new Achievement("oc.microcontroller", "oc.microcontroller",
    10, -2, Items.get(Constants.BlockName.Microcontroller).createItemStack(1), Assembler)
  val Robot = new Achievement("oc.robot", "oc.robot",
    10, -3, Items.get(Constants.BlockName.Robot).createItemStack(1), Assembler)
  val Drone = new Achievement("oc.drone", "oc.drone",
    10, -4, Items.get(Constants.ItemName.Drone).createItemStack(1), Assembler)
  val Tablet = new Achievement("oc.tablet", "oc.tablet",
    10, -5, Items.get(Constants.ItemName.Tablet).createItemStack(1), Assembler)
  val Charger = new Achievement("oc.charger", "oc.charger",
    8, -1, Items.get(Constants.BlockName.Charger).createItemStack(1), Capacitor, Constants.BlockName.Charger)
  val CPU = new Achievement("oc.cpu", "oc.cpu",
    6, 0, Items.get(Constants.ItemName.CPUTier1).createItemStack(1), Microchip, Constants.ItemName.CPUTier1, Constants.ItemName.CPUTier2, Constants.ItemName.CPUTier3)
  val MotionSensor = new Achievement("oc.motionSensor", "oc.motionSensor",
    8, 0, Items.get(Constants.BlockName.MotionSensor).createItemStack(1), CPU, Constants.BlockName.MotionSensor)
  val Geolyzer = new Achievement("oc.geolyzer", "oc.geolyzer",
    8, 1, Items.get(Constants.BlockName.Geolyzer).createItemStack(1), CPU, Constants.BlockName.Geolyzer)
  val RedstoneIO = new Achievement("oc.redstoneIO", "oc.redstoneIO",
    8, 2, Items.get(Constants.BlockName.Redstone).createItemStack(1), CPU, Constants.BlockName.Redstone)
  val EEPROM = new Achievement("oc.eeprom", "oc.eeprom",
    6, 3, Items.get(Constants.ItemName.EEPROM).createItemStack(1), Microchip, Constants.ItemName.EEPROM)
  val Memory = new Achievement("oc.ram", "oc.ram",
    6, 4, Items.get(Constants.ItemName.RAMTier1).createItemStack(1), Microchip, Constants.ItemName.RAMTier1, Constants.ItemName.RAMTier2, Constants.ItemName.RAMTier3, Constants.ItemName.RAMTier4, Constants.ItemName.RAMTier5, Constants.ItemName.RAMTier6)
  val HDD = new Achievement("oc.hdd", "oc.hdd",
    6, 5, Items.get(Constants.ItemName.HDDTier1).createItemStack(1), Microchip, Constants.ItemName.HDDTier1, Constants.ItemName.HDDTier2, Constants.ItemName.HDDTier3)
  val Case = new Achievement("oc.case", "oc.case",
    6, 6, Items.get(Constants.BlockName.CaseTier1).createItemStack(1), Microchip, Constants.BlockName.CaseTier1, Constants.BlockName.CaseTier2, Constants.BlockName.CaseTier3)
  val Rack = new Achievement("oc.rack", "oc.rack",
    8, 6, Items.get(Constants.BlockName.ServerRack).createItemStack(1), Case, Constants.BlockName.ServerRack)
  val Server = new Achievement("oc.server", "oc.server",
    10, 6, Items.get(Constants.ItemName.ServerTier1).createItemStack(1), Rack, Constants.ItemName.ServerTier1, Constants.ItemName.ServerTier2, Constants.ItemName.ServerTier3)
  val Screen = new Achievement("oc.screen", "oc.screen",
    6, 7, Items.get(Constants.BlockName.ScreenTier1).createItemStack(1), Microchip, Constants.BlockName.ScreenTier1, Constants.BlockName.ScreenTier2, Constants.BlockName.ScreenTier3)
  val Keyboard = new Achievement("oc.keyboard", "oc.keyboard",
    8, 7, Items.get(Constants.BlockName.Keyboard).createItemStack(1), Screen, Constants.BlockName.Keyboard)
  val Hologram = new Achievement("oc.hologram", "oc.hologram",
    8, 8, Items.get(Constants.BlockName.HologramTier1).createItemStack(1), Screen, Constants.BlockName.HologramTier1, Constants.BlockName.HologramTier2)
  val DiskDrive = new Achievement("oc.diskDrive", "oc.diskDrive",
    6, 9, Items.get(Constants.BlockName.DiskDrive).createItemStack(1), Microchip, Constants.BlockName.DiskDrive)
  val Floppy = new Achievement("oc.floppy", "oc.floppy",
    8, 9, Items.get(Constants.ItemName.Floppy).createItemStack(1), DiskDrive, Constants.ItemName.Floppy)
  val OpenOS = new Achievement("oc.openOS", "oc.openOS",
    10, 9, Items.createOpenOS(), Floppy)
  val Raid = new Achievement("oc.raid", "oc.raid",
    8, 10, Items.get(Constants.BlockName.Raid).createItemStack(1), DiskDrive, Constants.BlockName.Raid)

  val Card = new Achievement("oc.card", "oc.card",
    0, -2, Items.get(Constants.ItemName.Card).createItemStack(1), null, Constants.ItemName.Card).setIndependent()
  val RedstoneCard = new Achievement("oc.redstoneCard", "oc.redstoneCard",
    -2, -4, Items.get(Constants.ItemName.RedstoneCardTier1).createItemStack(1), Card, Constants.ItemName.RedstoneCardTier1, Constants.ItemName.RedstoneCardTier2)
  val GraphicsCard = new Achievement("oc.graphicsCard", "oc.graphicsCard",
    0, -5, Items.get(Constants.ItemName.GraphicsCardTier1).createItemStack(1), Card, Constants.ItemName.GraphicsCardTier1, Constants.ItemName.GraphicsCardTier2, Constants.ItemName.GraphicsCardTier3)
  val NetworkCard = new Achievement("oc.networkCard", "oc.networkCard",
    2, -4, Items.get(Constants.ItemName.NetworkCard).createItemStack(1), Card, Constants.ItemName.NetworkCard)
  val WirelessNetworkCard = new Achievement("oc.wirelessNetworkCard", "oc.wirelessNetworkCard",
    2, -6, Items.get(Constants.ItemName.WirelessNetworkCard).createItemStack(1), NetworkCard, Constants.ItemName.WirelessNetworkCard)

  val Cable = new Achievement("oc.cable", "oc.cable",
    -2, 0, Items.get(Constants.BlockName.Cable).createItemStack(1), null, Constants.BlockName.Cable).setIndependent()
  val PowerDistributor = new Achievement("oc.powerDistributor", "oc.powerDistributor",
    -4, -1, Items.get(Constants.BlockName.PowerDistributor).createItemStack(1), Cable, Constants.BlockName.PowerDistributor)
  val Switch = new Achievement("oc.switch", "oc.switch",
    -4, 0, Items.get(Constants.BlockName.Switch).createItemStack(1), Cable, "switch", Constants.BlockName.Switch)
  val Adapter = new Achievement("oc.adapter", "oc.adapter",
    -4, 1, Items.get(Constants.BlockName.Adapter).createItemStack(1), Cable, Constants.BlockName.Adapter)

  def init() {
    // Missing @Override causes ambiguity, so cast is required; still a virtual call,
    // so Achievement.registerStat is still the method that's really being called.
    All.foreach(_.asInstanceOf[StatBase].registerStat())
    AchievementPage.registerAchievementPage(new AchievementPage("OpenComputers", All: _*))
  }

  def onAssemble(stack: ItemStack, player: EntityPlayer): Unit = {
    val descriptor = Items.get(stack)
    if (descriptor == Items.get(Constants.BlockName.Microcontroller)) player.addStat(Microcontroller, 1)
    if (descriptor == Items.get(Constants.BlockName.Robot)) player.addStat(Robot, 1)
    if (descriptor == Items.get(Constants.ItemName.Drone)) player.addStat(Drone, 1)
    if (descriptor == Items.get(Constants.ItemName.Tablet)) player.addStat(Tablet, 1)
  }

  def onCraft(stack: ItemStack, player: EntityPlayer): Unit = {
    CraftingMap.get(Items.get(stack)).foreach(player.addStat(_, 1))

    if (ItemStack.areItemStacksEqual(stack, Items.createOpenOS())) {
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
