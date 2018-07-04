package li.cil.oc.common

import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack

//placeholder

object Achievement {
  def init() {
  }

  def onAssemble(stack: ItemStack, player: EntityPlayer): Unit = {

  }

  def onCraft(stack: ItemStack, player: EntityPlayer): Unit = {

  }
}

/*package li.cil.oc.common TODO Advancements

import li.cil.oc.Constants
import li.cil.oc.OpenComputers
import li.cil.oc.api.detail.ItemInfo
import li.cil.oc.common.init.Items
import li.cil.oc.util.StackOption
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.stats.StatBase
import net.minecraft.stats.{Achievement => MCAchievement}
import net.minecraftforge.common.AchievementPage

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

object Achievement {
  val All: ArrayBuffer[MCAchievement] = mutable.ArrayBuffer.empty[MCAchievement]
  val CraftingMap = mutable.Map.empty[ItemInfo, MCAchievement]
  val CustomCraftingMap = mutable.Map.empty[ItemStack, MCAchievement]
  val AssemblingMap = mutable.Map.empty[ItemInfo, MCAchievement]

  val Transistor: MCAchievement = newAchievement("transistor").
    at(2, 0).
    whenCrafting(Constants.ItemName.Transistor).
    add()
  val Disassembler: MCAchievement = newAchievement("disassembler").
    at(2, 2).
    whenCrafting(Constants.BlockName.Disassembler).
    withParent(Transistor).
    add()
  val Microchip: MCAchievement = newAchievement("chip").
    at(4, 0).
    withParent(Transistor).
    whenCrafting(Constants.ItemName.ChipTier1).
    whenCrafting(Constants.ItemName.ChipTier2).
    whenCrafting(Constants.ItemName.ChipTier3).
    add()
  val Capacitor: MCAchievement = newAchievement("capacitor").
    at(6, -1).
    withParent(Microchip).
    whenCrafting(Constants.BlockName.Capacitor).
    add()
  val Assembler: MCAchievement = newAchievement("assembler").
    at(8, -2).
    withParent(Capacitor).
    whenCrafting(Constants.BlockName.Assembler).
    add()
  val Microcontroller: MCAchievement = newAchievement("microcontroller").
    at(10, -2).
    withParent(Assembler).
    whenAssembling(Constants.BlockName.Microcontroller).
    add()
  val Robot: MCAchievement = newAchievement("robot").
    at(10, -3).
    withParent(Assembler).
    whenAssembling(Constants.BlockName.Robot).
    add()
  val Drone: MCAchievement = newAchievement("drone").
    at(10, -4).
    withParent(Assembler).
    whenAssembling(Constants.ItemName.Drone).
    add()
  val Tablet: MCAchievement = newAchievement("tablet").
    at(10, -5).
    withParent(Assembler).
    whenAssembling(Constants.ItemName.Tablet).
    add()
  val Charger: MCAchievement = newAchievement("charger").
    at(8, -1).
    withParent(Capacitor).
    whenCrafting(Constants.BlockName.Charger).
    add()
  val CPU: MCAchievement = newAchievement("cpu").
    at(6, 0).
    withParent(Microchip).
    whenCrafting(Constants.ItemName.CPUTier1).
    whenCrafting(Constants.ItemName.CPUTier2).
    whenCrafting(Constants.ItemName.CPUTier3).
    add()
  val MotionSensor: MCAchievement = newAchievement("motionSensor").
    at(8, 0).
    withParent(CPU).
    whenCrafting(Constants.BlockName.MotionSensor).
    add()
  val Geolyzer: MCAchievement = newAchievement("geolyzer").
    at(8, 1).
    withParent(CPU).
    whenCrafting(Constants.BlockName.Geolyzer).
    add()
  val RedstoneIO: MCAchievement = newAchievement("redstoneIO").
    at(8, 2).
    withParent(CPU).
    whenCrafting(Constants.BlockName.Redstone).
    add()
  val EEPROM: MCAchievement = newAchievement("eeprom").
    at(6, 3).
    withParent(Microchip).
    whenCrafting(Constants.ItemName.EEPROM).
    add()
  val Memory: MCAchievement = newAchievement("ram").
    at(6, 4).
    withParent(Microchip).
    whenCrafting(Constants.ItemName.RAMTier1).
    whenCrafting(Constants.ItemName.RAMTier2).
    whenCrafting(Constants.ItemName.RAMTier3).
    whenCrafting(Constants.ItemName.RAMTier4).
    whenCrafting(Constants.ItemName.RAMTier5).
    whenCrafting(Constants.ItemName.RAMTier6).
    add()
  val HDD: MCAchievement = newAchievement("hdd").
    at(6, 5).
    withParent(Microchip).
    whenCrafting(Constants.ItemName.HDDTier1).
    whenCrafting(Constants.ItemName.HDDTier2).
    whenCrafting(Constants.ItemName.HDDTier3).
    add()
  val Case: MCAchievement = newAchievement("case").
    at(6, 6).
    withParent(Microchip).
    whenCrafting(Constants.BlockName.CaseTier1).
    whenCrafting(Constants.BlockName.CaseTier2).
    whenCrafting(Constants.BlockName.CaseTier3).
    add()
  val Rack: MCAchievement = newAchievement("rack").
    at(8, 6).
    withParent(Case).
    whenCrafting(Constants.BlockName.Rack).
    add()
  val Server: MCAchievement = newAchievement("server").
    at(10, 6).
    withParent(Rack).
    whenCrafting(Constants.ItemName.ServerTier1).
    whenCrafting(Constants.ItemName.ServerTier2).
    whenCrafting(Constants.ItemName.ServerTier3).
    add()
  val Screen: MCAchievement = newAchievement("screen").
    at(6, 7).
    withParent(Microchip).
    whenCrafting(Constants.BlockName.ScreenTier1).
    whenCrafting(Constants.BlockName.ScreenTier2).
    whenCrafting(Constants.BlockName.ScreenTier3).
    add()
  val Keyboard: MCAchievement = newAchievement("keyboard").
    at(8, 7).
    withParent(Screen).
    whenCrafting(Constants.BlockName.Keyboard).
    add()
  val Hologram: MCAchievement = newAchievement("hologram").
    at(8, 8).
    withParent(Screen).
    whenCrafting(Constants.BlockName.HologramTier1).
    whenCrafting(Constants.BlockName.HologramTier2).
    add()
  val DiskDrive: MCAchievement = newAchievement("diskDrive").
    at(6, 9).
    withParent(Microchip).
    whenCrafting(Constants.BlockName.DiskDrive).
    add()
  val Floppy: MCAchievement = newAchievement("floppy").
    at(8, 9).
    withParent(DiskDrive).
    whenCrafting(Constants.ItemName.Floppy).
    add()
  val OpenOS: MCAchievement = newAchievement("openOS").
    at(10, 9).
    withParent(Floppy).
    whenCrafting(Constants.ItemName.OpenOS).
    add()
  val Raid: MCAchievement = newAchievement("raid").
    at(8, 10).
    withParent(DiskDrive).
    whenCrafting(Constants.BlockName.Raid).
    add()

  val Card: MCAchievement = newAchievement("card").
    at(0, -2).
    whenCrafting(Constants.ItemName.Card).
    add()
  val RedstoneCard: MCAchievement = newAchievement("redstoneCard").
    at(-2, -4).
    withParent(Card).
    whenCrafting(Constants.ItemName.RedstoneCardTier1).
    whenCrafting(Constants.ItemName.RedstoneCardTier2).
    add()
  val GraphicsCard: MCAchievement = newAchievement("graphicsCard").
    at(0, -5).
    withParent(Card).
    whenCrafting(Constants.ItemName.GraphicsCardTier1).
    whenCrafting(Constants.ItemName.GraphicsCardTier2).
    whenCrafting(Constants.ItemName.GraphicsCardTier3).
    add()
  val NetworkCard: MCAchievement = newAchievement("networkCard").
    at(2, -4).
    withParent(Card).
    whenCrafting(Constants.ItemName.NetworkCard).
    add()
  val WirelessNetworkCard: MCAchievement = newAchievement("wirelessNetworkCard").
    at(2, -6).
    withParent(NetworkCard).
    whenCrafting(Constants.ItemName.WirelessNetworkCardTier1).
    whenCrafting(Constants.ItemName.WirelessNetworkCardTier2).
    add()

  val Cable: MCAchievement = newAchievement("cable").
    at(-2, 0).
    whenCrafting(Constants.BlockName.Cable).
    add()
  val PowerDistributor: MCAchievement = newAchievement("powerDistributor").
    at(-4, -1).
    withParent(Cable).
    whenCrafting(Constants.BlockName.PowerDistributor).
    add()
  val Switch: MCAchievement = newAchievement("switch").
    at(-4, 0).
    withParent(Cable).
    whenCrafting(Constants.BlockName.Relay).
    add()
  val Adapter: MCAchievement = newAchievement("adapter").
    at(-4, 1).
    withParent(Cable).
    whenCrafting(Constants.BlockName.Adapter).
    add()

  def init() {
    // Missing @Override causes ambiguity, so cast is required; still a virtual call,
    // so Achievement.registerStat is still the method that's really being called.
    All.foreach(_.asInstanceOf[StatBase].registerStat())
    AchievementPage.registerAchievementPage(new AchievementPage(OpenComputers.Name, All: _*))
  }

  def onAssemble(stack: ItemStack, player: EntityPlayer): Unit = {
    AssemblingMap.get(Items.get(stack)).foreach(player.addStat(_, 1))
  }

  def onCraft(stack: ItemStack, player: EntityPlayer): Unit = {
    CraftingMap.get(Items.get(stack)).foreach(player.addStat(_, 1))
    CustomCraftingMap.find(entry => ItemStack.areItemStacksEqual(stack, entry._1)).foreach(entry => player.addStat(entry._2, 1))
  }

  private def newAchievement(name: String) = new AchievementBuilder(name)

  private class AchievementBuilder(val name: String) {
    var x = 0
    var y = 0
    var stack: StackOption = stackFromName(name)
    var parent: Option[MCAchievement] = None
    var crafting = mutable.Set.empty[String]
    var customCrafting = mutable.Set.empty[ItemStack]
    var assembling = mutable.Set.empty[String]

    def at(x: Int, y: Int): AchievementBuilder = {
      this.x = x
      this.y = y
      this
    }

    def withIconOf(stack: ItemStack): AchievementBuilder = {
      this.stack = StackOption(stack)
      this
    }

    def withParent(parent: MCAchievement): AchievementBuilder = {
      this.parent = Option(parent)
      this
    }

    def whenCrafting(name: String): AchievementBuilder = {
      crafting += name
      if (stack.isEmpty) stack = stackFromName(name)
      this
    }

    def whenCrafting(stack: ItemStack): AchievementBuilder = {
      customCrafting += stack
      if (this.stack.isEmpty) this.stack = StackOption(stack)
      this
    }

    def whenAssembling(name: String): AchievementBuilder = {
      assembling += name
      if (stack.isEmpty) stack = stackFromName(name)
      this
    }

    def add(): MCAchievement = {
      val achievement = new MCAchievement("oc." + name, "oc." + name, x, y, stack.orEmpty, parent.orNull)

      if (parent.isEmpty) {
        achievement.asInstanceOf[StatBase].initIndependentStat()
      }

      for (requirement <- crafting) {
        val descriptor = Items.get(requirement)
        if (descriptor != null) {
          Achievement.CraftingMap += descriptor -> achievement
        }
      }

      for (requirement <- customCrafting) {
        if (!requirement.isEmpty) {
          Achievement.CustomCraftingMap += requirement -> achievement
        }
      }

      for (requirement <- assembling) {
        val descriptor = Items.get(requirement)
        if (descriptor != null) {
          Achievement.AssemblingMap += descriptor -> achievement
        }
      }

      Achievement.All += achievement
      achievement
    }

    private def stackFromName(name: String): StackOption = StackOption(Option(Items.get(name)).map(_.createItemStack(1)))
  }

}
*/
