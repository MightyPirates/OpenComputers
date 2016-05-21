package li.cil.oc.common.item.data

import java.lang.reflect.Method

import li.cil.oc.Constants
import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.common.IMC
import li.cil.oc.util.ExtendedAABB._
import li.cil.oc.util.ExtendedNBT._
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.math.AxisAlignedBB
import net.minecraftforge.common.util.Constants.NBT

import scala.collection.mutable

class PrintData extends ItemData(Constants.BlockName.Print) {
  def this(stack: ItemStack) {
    this()
    load(stack)
  }

  var label: Option[String] = None
  var tooltip: Option[String] = None
  var isButtonMode = false
  var redstoneLevel = 0
  var pressurePlate = false
  val stateOff = mutable.Set.empty[PrintData.Shape]
  val stateOn = mutable.Set.empty[PrintData.Shape]
  var isBeaconBase = false
  var lightLevel = 0
  var noclipOff = false
  var noclipOn = false

  def complexity = stateOn.size max stateOff.size

  def hasActiveState = stateOn.nonEmpty

  def emitLight = lightLevel > 0

  def emitRedstone = redstoneLevel > 0

  def emitRedstone(state: Boolean): Boolean = if (state) emitRedstoneWhenOn else emitRedstoneWhenOff

  def emitRedstoneWhenOff = emitRedstone && !hasActiveState

  def emitRedstoneWhenOn = emitRedstone && hasActiveState

  def opacity = {
    if (opacityDirty) {
      opacityDirty = false
      opacity_ = PrintData.computeApproximateOpacity(stateOn) min PrintData.computeApproximateOpacity(stateOff)
    }
    opacity_
  }

  // lazily computed and stored, because potentially slow
  private var opacity_ = 0f
  private var opacityDirty = true

  private final val LabelTag = "label"
  private final val TooltipTag = "tooltip"
  private final val IsButtonModeTag = "isButtonMode"
  private final val RedstoneLevelTag = "redstoneLevel"
  private final val RedstoneLevelTagCompat = "emitRedstone"
  private final val PressurePlateTag = "pressurePlate"
  private final val StateOffTag = "stateOff"
  private final val StateOnTag = "stateOn"
  private final val IsBeaconBaseTag = "isBeaconBase"
  private final val LightLevelTag = "lightLevel"
  private final val NoclipOffTag = "noclipOff"
  private final val NoclipOnTag = "noclipOn"

  override def load(nbt: NBTTagCompound): Unit = {
    if (nbt.hasKey(LabelTag)) label = Option(nbt.getString(LabelTag)) else label = None
    if (nbt.hasKey(TooltipTag)) tooltip = Option(nbt.getString(TooltipTag)) else tooltip = None
    isButtonMode = nbt.getBoolean(IsButtonModeTag)
    redstoneLevel = nbt.getInteger(RedstoneLevelTag) max 0 min 15
    if (nbt.getBoolean(RedstoneLevelTagCompat)) redstoneLevel = 15
    pressurePlate = nbt.getBoolean(PressurePlateTag)
    stateOff.clear()
    stateOff ++= nbt.getTagList(StateOffTag, NBT.TAG_COMPOUND).map(PrintData.nbtToShape)
    stateOn.clear()
    stateOn ++= nbt.getTagList(StateOnTag, NBT.TAG_COMPOUND).map(PrintData.nbtToShape)
    isBeaconBase = nbt.getBoolean(IsBeaconBaseTag)
    lightLevel = (nbt.getByte(LightLevelTag) & 0xFF) max 0 min 15
    noclipOff = nbt.getBoolean(NoclipOffTag)
    noclipOn = nbt.getBoolean(NoclipOnTag)

    opacityDirty = true
  }

  override def save(nbt: NBTTagCompound): Unit = {
    label.foreach(nbt.setString(LabelTag, _))
    tooltip.foreach(nbt.setString(TooltipTag, _))
    nbt.setBoolean(IsButtonModeTag, isButtonMode)
    nbt.setInteger(RedstoneLevelTag, redstoneLevel)
    nbt.setBoolean(PressurePlateTag, pressurePlate)
    nbt.setNewTagList(StateOffTag, stateOff.map(PrintData.shapeToNBT))
    nbt.setNewTagList(StateOnTag, stateOn.map(PrintData.shapeToNBT))
    nbt.setBoolean(IsBeaconBaseTag, isBeaconBase)
    nbt.setByte(LightLevelTag, lightLevel.toByte)
    nbt.setBoolean(NoclipOffTag, noclipOff)
    nbt.setBoolean(NoclipOnTag, noclipOn)
  }
}

object PrintData {
  // The following logic is used to approximate the opacity of a print, for
  // which we use the volume as a heuristic. Because computing the actual
  // volume is a) expensive b) not necessarily a good heuristic (e.g. a
  // "dotted grid") we take a shortcut and divide the space into a few
  // sub-sections, for each of which we check if there's anything in it.
  // If so, we consider that area "opaque". To compensate, prints can never
  // be fully light-opaque. This gives a little bit of shading as a nice
  // effect, but avoid it looking derpy when there are only a few sparse
  // shapes in the model.
  private val stepping = 4
  private val step = stepping / 16f
  private val invMaxVolume = 1f / (stepping * stepping * stepping)

  private val inkProviders = mutable.LinkedHashSet.empty[Method]

  def addInkProvider(provider: Method): Unit = inkProviders += provider

  def computeApproximateOpacity(shapes: Iterable[PrintData.Shape]) = {
    var volume = 1f
    if (shapes.nonEmpty) for (x <- 0 until 16 / stepping; y <- 0 until 16 / stepping; z <- 0 until 16 / stepping) {
      val bounds = new AxisAlignedBB(
        x * step, y * step, z * step,
        (x + 1) * step, (y + 1) * step, (z + 1) * step)
      if (!shapes.exists(_.bounds.intersectsWith(bounds))) {
        volume -= invMaxVolume
      }
    }
    volume
  }

  def computeCosts(data: PrintData) = {
    val totalVolume = data.stateOn.foldLeft(0)((acc, shape) => acc + shape.bounds.volume) + data.stateOff.foldLeft(0)((acc, shape) => acc + shape.bounds.volume)
    val totalSurface = data.stateOn.foldLeft(0)((acc, shape) => acc + shape.bounds.surface) + data.stateOff.foldLeft(0)((acc, shape) => acc + shape.bounds.surface)
    val multiplier = if (data.noclipOff || data.noclipOn) Settings.get.noclipMultiplier else 1

    if (totalVolume > 0) {
      val baseMaterialRequired = (totalVolume / 2) max 1
      val materialRequired =
        if (data.redstoneLevel > 0 && data.redstoneLevel < 15) baseMaterialRequired + Settings.get.printCustomRedstone
        else baseMaterialRequired
      val inkRequired = (totalSurface / 6) max 1

      Option(((materialRequired * multiplier).toInt, inkRequired))
    }
    else None
  }

  private val materialPerItem = Settings.get.printMaterialValue

  def materialValue(stack: ItemStack) = {
    if (api.Items.get(stack) == api.Items.get(Constants.ItemName.Chamelium))
      materialPerItem
    else if (api.Items.get(stack) == api.Items.get(Constants.BlockName.Print)) {
      val data = new PrintData(stack)
      computeCosts(data) match {
        case Some((materialRequired, inkRequired)) => (materialRequired * Settings.get.printRecycleRate).toInt
        case _ => 0
      }
    }
    else 0
  }

  def inkValue(stack: ItemStack): Int = {
    for (provider <- inkProviders) {
      val value = IMC.tryInvokeStatic(provider, stack)(0)
      if (value > 0) {
        return value
      }
    }
    0
  }

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
        new AxisAlignedBB(minX, minY, minZ, maxX, maxY, maxZ)
      }
      else {
        val bounds = nbt.getByteArray("bounds").padTo(6, 0.toByte)
        val minX = bounds(0) / 16f
        val minY = bounds(1) / 16f
        val minZ = bounds(2) / 16f
        val maxX = bounds(3) / 16f
        val maxY = bounds(4) / 16f
        val maxZ = bounds(5) / 16f
        new AxisAlignedBB(minX, minY, minZ, maxX, maxY, maxZ)
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