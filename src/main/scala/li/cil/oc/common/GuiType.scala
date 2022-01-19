package li.cil.oc.common

import li.cil.oc.util.ScalaEnum

import scala.collection.mutable

object GuiType extends ScalaEnum {
  val Categories = mutable.Map.empty[Int, Category.Value]

  sealed trait EnumVal extends Value {
    def id = ordinal
    def subType: GuiType.Category.Value
    Categories += ordinal -> subType
  }

  val Adapter = new EnumVal { def name = "Adapter"; def subType = GuiType.Category.Block }
  val Assembler = new EnumVal { def name = "Assembler"; def subType = GuiType.Category.Block }
  val Case = new EnumVal { def name = "Case"; def subType = GuiType.Category.Block }
  val Charger = new EnumVal { def name = "Charger"; def subType = GuiType.Category.Block }
  val Database = new EnumVal { def name = "Database"; def subType = GuiType.Category.Item }
  val Disassembler = new EnumVal { def name = "Disassembler"; def subType = GuiType.Category.Block }
  val DiskDrive = new EnumVal { def name = "DiskDrive"; def subType = GuiType.Category.Block }
  val DiskDriveMountable = new EnumVal { def name = "DiskDriveMountable"; def subType = GuiType.Category.Item }
  val DiskDriveMountableInRack = new EnumVal { def name = "DiskDriveMountableInRack"; def subType = GuiType.Category.Block }
  val Drive = new EnumVal { def name = "Drive"; def subType = GuiType.Category.Item }
  val Drone = new EnumVal { def name = "Drone"; def subType = GuiType.Category.Entity }
  val Manual = new EnumVal { def name = "Manual"; def subType = GuiType.Category.None }
  val Printer = new EnumVal { def name = "Printer"; def subType = GuiType.Category.Block }
  val Rack = new EnumVal { def name = "Rack"; def subType = GuiType.Category.Block }
  val Raid = new EnumVal { def name = "Raid"; def subType = GuiType.Category.Block }
  val Relay = new EnumVal { def name = "Relay"; def subType = GuiType.Category.Block }
  val Robot = new EnumVal { def name = "Robot"; def subType = GuiType.Category.Block }
  val Screen = new EnumVal { def name = "Screen"; def subType = GuiType.Category.Block }
  val Server = new EnumVal { def name = "Server"; def subType = GuiType.Category.Item }
  val ServerInRack = new EnumVal { def name = "ServerInRack"; def subType = GuiType.Category.Block }
  val Switch = new EnumVal { def name = "Switch"; def subType = GuiType.Category.Block }
  val Tablet = new EnumVal { def name = "Tablet"; def subType = GuiType.Category.Item }
  val TabletInner = new EnumVal { def name = "TabletInner"; def subType = GuiType.Category.Item }
  val Terminal = new EnumVal { def name = "Terminal"; def subType = GuiType.Category.Item }
  val Waypoint = new EnumVal { def name = "Waypoint"; def subType = GuiType.Category.Block }

  object Category extends ScalaEnum {
    sealed trait EnumVal extends Value

    val None = new EnumVal { def name = "None" }
    val Block = new EnumVal { def name = "Block" }
    val Entity = new EnumVal { def name = "Entity" }
    val Item = new EnumVal { def name = "Item" }
  }

  def embedSlot(y: Int, slot: Int) = (y & 0x00FFFFFF) | (slot << 24)

  def extractY(value: Int) = value << 8 >> 8

  def extractSlot(value: Int) = value >>> 24
}
