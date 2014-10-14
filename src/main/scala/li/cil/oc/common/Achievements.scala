package li.cil.oc.common

import li.cil.oc.api.Items
import net.minecraft.stats.{Achievement, AchievementList}
import net.minecraftforge.common.AchievementPage


object Achievements {
  var achievementTransistor: Achievement = null
  var achievementChip1: Achievement = null
  var achievementCPU: Achievement = null
  var achievementRAM: Achievement = null
  var achievementKeyboard: Achievement = null
  var achievementCase1: Achievement = null
  var achievementOpenOS:Achievement = null
  var achievementScreen:Achievement = null
  def init() {

    achievementTransistor = new Achievement("oc.transistor", "oc.transistor", -2, 0, Items.get("transistor").createItemStack(1), AchievementList.acquireIron).registerStat()
    achievementChip1 = new Achievement("oc.chip1", "oc.chip1", 0,0, Items.get("chip1").createItemStack(1), achievementTransistor).registerStat()

    achievementRAM = new Achievement("oc.ram1", "oc.ram1", 2, -1, Items.get("ram1").createItemStack(1), achievementChip1).registerStat()
    achievementCPU = new Achievement("oc.cpu1", "oc.cpu1", 2, -3, Items.get("cpu1").createItemStack(1), achievementChip1).registerStat()

    achievementCase1 = new Achievement("oc.case1", "oc.case1", 2,1, Items.get("case1").createItemStack(1), achievementChip1).registerStat()
    achievementOpenOS = new Achievement("oc.openOS", "oc.openOS", 4, 1, Items.get("openOS").createItemStack(1), achievementCase1).registerStat()
    achievementScreen = new Achievement("oc.screen1", "oc.screen1", 0, 3, Items.get("screen1").createItemStack(1), achievementChip1).registerStat()
    achievementKeyboard = new Achievement("oc.keyboard", "oc.keyboard",-2, 3, Items.get("keyboard").createItemStack(1), achievementScreen).registerStat()

    val page = new AchievementPage("OpenComputers", achievementTransistor, achievementCase1,achievementOpenOS,achievementScreen,achievementRAM,achievementCPU,achievementKeyboard,achievementChip1)
    AchievementPage.registerAchievementPage(page)
  }
}
