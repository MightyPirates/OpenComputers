package li.cil.oc.common

object Slot {
  val None = "none"

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

  def fromApi(slotType: li.cil.oc.api.driver.Slot) = slotType match {
    case li.cil.oc.api.driver.Slot.Card => Card
    case li.cil.oc.api.driver.Slot.Disk => Floppy
    case li.cil.oc.api.driver.Slot.HardDiskDrive => HDD
    case li.cil.oc.api.driver.Slot.Memory => Memory
    case li.cil.oc.api.driver.Slot.Processor => CPU
    case li.cil.oc.api.driver.Slot.Tool => Tool
    case li.cil.oc.api.driver.Slot.Upgrade => Upgrade
    case li.cil.oc.api.driver.Slot.UpgradeContainer => Container
    case _ => None
  }
}
