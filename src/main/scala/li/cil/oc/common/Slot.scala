package li.cil.oc.common

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
}
