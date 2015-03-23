package li.cil.oc.common.item.data

import li.cil.oc.api
import li.cil.oc.util.ExtendedNBT._
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.AxisAlignedBB
import net.minecraftforge.common.util.Constants.NBT

import scala.collection.mutable

class PrintData extends ItemData {
  def this(stack: ItemStack) {
    this()
    load(stack)
  }

  var label: Option[String] = None
  var tooltip: Option[String] = None
  var autoRevert = false
  var emitRedstone = false
  var pressurePlate = false
  val stateOff = mutable.Set.empty[PrintData.Shape]
  val stateOn = mutable.Set.empty[PrintData.Shape]

  override def load(nbt: NBTTagCompound): Unit = {
    if (nbt.hasKey("label")) label = Option(nbt.getString("label"))
    if (nbt.hasKey("tooltip")) tooltip = Option(nbt.getString("tooltip"))
    autoRevert = nbt.getBoolean("autoRevert")
    emitRedstone = nbt.getBoolean("emitRedstone")
    pressurePlate = nbt.getBoolean("pressurePlate")
    stateOff ++= nbt.getTagList("stateOff", NBT.TAG_COMPOUND).map(PrintData.nbtToShape)
    stateOn ++= nbt.getTagList("stateOn", NBT.TAG_COMPOUND).map(PrintData.nbtToShape)
  }

  override def save(nbt: NBTTagCompound): Unit = {
    label.foreach(nbt.setString("label", _))
    tooltip.foreach(nbt.setString("tooltip", _))
    nbt.setBoolean("autoRevert", autoRevert)
    nbt.setBoolean("emitRedstone", emitRedstone)
    nbt.setBoolean("pressurePlate", pressurePlate)
    nbt.setNewTagList("stateOff", stateOff.map(PrintData.shapeToNBT))
    nbt.setNewTagList("stateOn", stateOn.map(PrintData.shapeToNBT))
  }

  def createItemStack() = {
    val stack = api.Items.get("print").createItemStack(1)
    save(stack)
    stack
  }
}

object PrintData {
  def nbtToShape(nbt: NBTTagCompound): Shape = {
    val minX = nbt.getByte("minX") / 16f
    val minY = nbt.getByte("minY") / 16f
    val minZ = nbt.getByte("minZ") / 16f
    val maxX = nbt.getByte("maxX") / 16f
    val maxY = nbt.getByte("maxY") / 16f
    val maxZ = nbt.getByte("maxZ") / 16f
    val texture = nbt.getString("texture")
    new Shape(AxisAlignedBB.getBoundingBox(minX, minY, minZ, maxX, maxY, maxZ), texture)
  }

  def shapeToNBT(shape: Shape): NBTTagCompound = {
    val nbt = new NBTTagCompound()
    nbt.setByte("minX", (shape.bounds.minX * 16).round.toByte)
    nbt.setByte("minY", (shape.bounds.minY * 16).round.toByte)
    nbt.setByte("minZ", (shape.bounds.minZ * 16).round.toByte)
    nbt.setByte("maxX", (shape.bounds.maxX * 16).round.toByte)
    nbt.setByte("maxY", (shape.bounds.maxY * 16).round.toByte)
    nbt.setByte("maxZ", (shape.bounds.maxZ * 16).round.toByte)
    nbt.setString("texture", shape.texture)
    nbt
  }

  class Shape(val bounds: AxisAlignedBB, val texture: String)

}