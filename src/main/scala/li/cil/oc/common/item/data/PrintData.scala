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
  var isButtonMode = false
  var emitRedstone = false
  var pressurePlate = false
  val stateOff = mutable.Set.empty[PrintData.Shape]
  val stateOn = mutable.Set.empty[PrintData.Shape]
  var isBeaconBase = false

  override def load(nbt: NBTTagCompound): Unit = {
    if (nbt.hasKey("label")) label = Option(nbt.getString("label")) else label = None
    if (nbt.hasKey("tooltip")) tooltip = Option(nbt.getString("tooltip")) else tooltip = None
    isButtonMode = nbt.getBoolean("isButtonMode")
    emitRedstone = nbt.getBoolean("emitRedstone")
    pressurePlate = nbt.getBoolean("pressurePlate")
    stateOff.clear()
    stateOff ++= nbt.getTagList("stateOff", NBT.TAG_COMPOUND).map(PrintData.nbtToShape)
    stateOn.clear()
    stateOn ++= nbt.getTagList("stateOn", NBT.TAG_COMPOUND).map(PrintData.nbtToShape)
    isBeaconBase = nbt.getBoolean("isBeaconBase")
  }

  override def save(nbt: NBTTagCompound): Unit = {
    label.foreach(nbt.setString("label", _))
    tooltip.foreach(nbt.setString("tooltip", _))
    nbt.setBoolean("isButtonMode", isButtonMode)
    nbt.setBoolean("emitRedstone", emitRedstone)
    nbt.setBoolean("pressurePlate", pressurePlate)
    nbt.setNewTagList("stateOff", stateOff.map(PrintData.shapeToNBT))
    nbt.setNewTagList("stateOn", stateOn.map(PrintData.shapeToNBT))
    nbt.setBoolean("isBeaconBase", isBeaconBase)
  }

  def createItemStack() = {
    val stack = api.Items.get("print").createItemStack(1)
    save(stack)
    stack
  }
}

object PrintData {
  def nbtToShape(nbt: NBTTagCompound): Shape = {
    val aabb =
      if (nbt.hasKey("minX")) {
        // Compatibility with shapes created with earlier dev-builds.
        val minX = nbt.getByte("minX") / 16f
        val minY = nbt.getByte("minY") / 16f
        val minZ = nbt.getByte("minZ") / 16f
        val maxX = nbt.getByte("maxX") / 16f
        val maxY = nbt.getByte("maxY") / 16f
        val maxZ = nbt.getByte("maxZ") / 16f
        AxisAlignedBB.getBoundingBox(minX, minY, minZ, maxX, maxY, maxZ)
      }
      else {
        val bounds = nbt.getByteArray("bounds").padTo(6, 0.toByte)
        val minX = bounds(0) / 16f
        val minY = bounds(1) / 16f
        val minZ = bounds(2) / 16f
        val maxX = bounds(3) / 16f
        val maxY = bounds(4) / 16f
        val maxZ = bounds(5) / 16f
        AxisAlignedBB.getBoundingBox(minX, minY, minZ, maxX, maxY, maxZ)
      }
    val texture = nbt.getString("texture")
    val tint = if (nbt.hasKey("tint")) Option(nbt.getInteger("tint")) else None
    new Shape(aabb, texture, tint)
  }

  def shapeToNBT(shape: Shape): NBTTagCompound = {
    val nbt = new NBTTagCompound()
    nbt.setByteArray("bounds", Array(
      (shape.bounds.minX * 16).round.toByte,
      (shape.bounds.minY * 16).round.toByte,
      (shape.bounds.minZ * 16).round.toByte,
      (shape.bounds.maxX * 16).round.toByte,
      (shape.bounds.maxY * 16).round.toByte,
      (shape.bounds.maxZ * 16).round.toByte
    ))
    nbt.setString("texture", shape.texture)
    shape.tint.foreach(nbt.setInteger("tint", _))
    nbt
  }

  class Shape(val bounds: AxisAlignedBB, val texture: String, val tint: Option[Int])

}