package li.cil.oc.common

import li.cil.oc.api
import net.minecraft.item.ItemStack

object Slot {
  val None = "none"

  val Any = "any"
  val Card = "card"
  val ComponentBus = "component_bus"
  val Container = "container"
  val CPU = "cpu"
  val Floppy = "floppy"
  val HDD = "hdd"
  val Memory = "memory"
  val Tool = "tool"
  val Upgrade = "upgrade"

  val All = Array(Card, ComponentBus, Container, CPU, Floppy, HDD, Memory, Tool, Upgrade)

  def apply(driver: api.driver.Item, stack: ItemStack, f: Option[ItemStack => api.driver.Slot] = scala.None) = f.getOrElse(driver.slot _)(stack) match {
    case li.cil.oc.api.driver.Slot.Card => Card
    case li.cil.oc.api.driver.Slot.Disk => Floppy
    case li.cil.oc.api.driver.Slot.HardDiskDrive => HDD
    case li.cil.oc.api.driver.Slot.Memory => Memory
    case li.cil.oc.api.driver.Slot.Processor => CPU
    case li.cil.oc.api.driver.Slot.Tool => Tool
    case li.cil.oc.api.driver.Slot.Upgrade => Upgrade
    case li.cil.oc.api.driver.Slot.UpgradeContainer => Container
    case _ =>
      val descriptor = api.Items.get(stack)
      if (descriptor == api.Items.get("componentBus1") || descriptor == api.Items.get("componentBus2") || descriptor == api.Items.get("componentBus3")) ComponentBus
      else None
  }
}
