package li.cil.oc.common

import li.cil.oc.api.Items
import net.minecraft.stats.AchievementList
import net.minecraft.stats.{Achievement => MCAchievement}
import net.minecraftforge.common.AchievementPage

object Achievement {
  lazy val Transistor = new MCAchievement("oc.transistor", "oc.transistor", -2, 0, Items.get("transistor").createItemStack(1), AchievementList.acquireIron)
  lazy val Microchip = new MCAchievement("oc.chip1", "oc.chip1", 0, 0, Items.get("chip1").createItemStack(1), Transistor)
  lazy val CPU = new MCAchievement("oc.ram1", "oc.ram1", 2, -1, Items.get("ram1").createItemStack(1), Microchip)
  lazy val Memory = new MCAchievement("oc.cpu1", "oc.cpu1", 2, -3, Items.get("cpu1").createItemStack(1), Microchip)
  lazy val Case = new MCAchievement("oc.case1", "oc.case1", 2, 1, Items.get("case1").createItemStack(1), Microchip)
  lazy val OpenOS = new MCAchievement("oc.openOS", "oc.openOS", 4, 1, Items.get("openOS").createItemStack(1), Case)
  lazy val Screen = new MCAchievement("oc.screen1", "oc.screen1", 0, 3, Items.get("screen1").createItemStack(1), Microchip)
  lazy val Keyboard = new MCAchievement("oc.keyboard", "oc.keyboard", -2, 3, Items.get("keyboard").createItemStack(1), Screen)

  lazy val All = Array(
    Transistor,
    Microchip,
    CPU,
    Memory,
    Case,
    OpenOS,
    Screen,
    Keyboard
  )

  def init() {
    All.foreach(_.registerStat())
    AchievementPage.registerAchievementPage(new AchievementPage("OpenComputers", All: _*))
  }
}
