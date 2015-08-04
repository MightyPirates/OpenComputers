package li.cil.oc.common

import li.cil.oc.api.driver

object Slot {
  val None = driver.item.Slot.None
  val Any = driver.item.Slot.Any
  val Filtered = "filtered"

  val Card = driver.item.Slot.Card
  val ComponentBus = driver.item.Slot.ComponentBus
  val Container = driver.item.Slot.Container
  val CPU = driver.item.Slot.CPU
  val EEPROM = "eeprom"
  val Floppy = driver.item.Slot.Floppy
  val HDD = driver.item.Slot.HDD
  val Memory = driver.item.Slot.Memory
  val RackMountable = driver.item.Slot.RackMountable
  val Tablet = driver.item.Slot.Tablet
  val Tool = "tool"
  val Upgrade = driver.item.Slot.Upgrade

  val All = Array(Card, ComponentBus, Container, CPU, EEPROM, Floppy, HDD, Memory, RackMountable, Tablet, Tool, Upgrade)
}
