package li.cil.oc.common.tileentity

import cpw.mods.fml.common.Optional
import cpw.mods.fml.common.Optional.Interface
import li.cil.oc.Config
import li.cil.oc.common.tileentity
import li.cil.oc.util.Persistable
import mods.immibis.redlogic.api.wiring._
import net.minecraft.nbt.{NBTTagByte, NBTTagList, NBTTagCompound}
import net.minecraftforge.common.ForgeDirection

@Optional.InterfaceList(Array(
  new Interface(iface = "mods.immibis.redlogic.api.wiring.IConnectable", modid = "RedLogic"),
  new Interface(iface = "mods.immibis.redlogic.api.wiring.IBundledEmitter", modid = "RedLogic"),
  new Interface(iface = "mods.immibis.redlogic.api.wiring.IBundledUpdatable", modid = "RedLogic"),
  new Interface(iface = "mods.immibis.redlogic.api.wiring.IRedstoneEmitter", modid = "RedLogic"),
  new Interface(iface = "mods.immibis.redlogic.api.wiring.IRedstoneUpdatable", modid = "RedLogic")
))
trait Redstone extends tileentity.Environment with Persistable
with IConnectable with IBundledEmitter with IBundledUpdatable with IRedstoneEmitter with IRedstoneUpdatable {
  private val _input = Array.fill[Byte](6)(-1)

  private val _output = Array.fill[Byte](6)(0)

  private val _bundledInput = Array.fill[Array[Byte]](6)(Array.fill[Byte](16)(-1))

  private val _bundledOutput = Array.fill[Array[Byte]](6)(Array.fill[Byte](16)(0))

  private var _isOutputEnabled = true

  private var _shouldUpdateInput = true

  def isOutputEnabled = _isOutputEnabled

  def isOutputEnabled_=(value: Boolean) = {
    if (value != isOutputEnabled) {
      _isOutputEnabled = value
      if (!isOutputEnabled) {
        for (i <- 0 until _output.length) {
          _output(i) = 0.toByte
        }
        for (i <- 0 until _bundledOutput.length) {
          for (j <- 0 until _bundledOutput(i).length) {
            _bundledOutput(i)(j) = 0.toByte
          }
        }
      }
      onRedstoneOutputChanged(ForgeDirection.UNKNOWN)
    }
    this
  }

  def input(side: ForgeDirection) = (_input(side.ordinal()) & 0xFF).toShort

  def output(side: ForgeDirection) = (_output(side.ordinal()) & 0xFF).toShort

  def output(side: ForgeDirection, value: Short): Unit = if (value != output(side)) {
    _output(side.ordinal()) = (value max 0 min 255).toByte
    onRedstoneOutputChanged(side)
  }

  def bundledInput(side: ForgeDirection, color: Int) = (_bundledInput(side.ordinal())(color) & 0xFF).toShort

  def bundledOutput(side: ForgeDirection, color: Int) =
    (_bundledOutput(side.ordinal())(color) & 0xFF).toShort

  def bundledOutput(side: ForgeDirection, color: Int, value: Short): Unit = if (value != bundledOutput(side, color)) {
    _bundledOutput(side.ordinal())(color) = (value max 0 min 255).toByte
    onRedstoneOutputChanged(side)
  }

  def checkRedstoneInputChanged() {
    _shouldUpdateInput = true
  }

  // ----------------------------------------------------------------------- //

  def updateRedstoneInput() {
    if (_shouldUpdateInput) {
      _shouldUpdateInput = false
      for (side <- ForgeDirection.VALID_DIRECTIONS) {
        val (oldInput, oldBundledInput) = (_input(side.ordinal()), _bundledInput(side.ordinal()))
        val (newInput, newBundledInput) = (computeInput(side), computeBundledInput(side))
        _input(side.ordinal()) = (newInput max 0 min 255).toByte
        var changed = oldInput >= 0 && input(side) != oldInput
        if (newBundledInput != null) for (i <- 0 until 16) {
          changed = changed || oldBundledInput(i) != newBundledInput(i)
          oldBundledInput(i) = newBundledInput(i)
        }
        else for (i <- 0 until 16) {
          changed = changed || oldBundledInput(i) != 0.toByte
          oldBundledInput(i) = 0.toByte
        }
        if (changed) {
          onRedstoneInputChanged(side)
        }
      }
    }
  }

  // ----------------------------------------------------------------------- //

  override def load(nbt: NBTTagCompound) = {
    super.load(nbt)

    val inputNbt = nbt.getTagList(Config.namespace + "redstone.input")
    for (i <- 0 until (_input.length min inputNbt.tagCount)) {
      _input(i) = inputNbt.tagAt(i).asInstanceOf[NBTTagByte].data
    }

    val outputNbt = nbt.getTagList(Config.namespace + "redstone.output")
    for (i <- 0 until (_output.length min outputNbt.tagCount)) {
      _output(i) = outputNbt.tagAt(i).asInstanceOf[NBTTagByte].data
    }

    val bundledInputNbt = nbt.getTagList(Config.namespace + "redstone.bundledInput")
    for (i <- 0 until (_bundledInput.length min bundledInputNbt.tagCount)) {
      val bundleNbt = bundledInputNbt.tagAt(i).asInstanceOf[NBTTagList]
      for (j <- 0 until (_bundledInput(i).length min bundleNbt.tagCount())) {
        _bundledInput(i)(j) = bundleNbt.tagAt(j).asInstanceOf[NBTTagByte].data
      }
    }

    val bundledOutputNbt = nbt.getTagList(Config.namespace + "redstone.bundledOutput")
    for (i <- 0 until (_bundledOutput.length min bundledOutputNbt.tagCount)) {
      val bundleNbt = bundledOutputNbt.tagAt(i).asInstanceOf[NBTTagList]
      for (j <- 0 until (_bundledOutput(i).length min bundleNbt.tagCount())) {
        _bundledOutput(i)(j) = bundleNbt.tagAt(j).asInstanceOf[NBTTagByte].data
      }
    }
  }

  override def save(nbt: NBTTagCompound) = {
    super.save(nbt)

    val inputNbt = new NBTTagList()
    for (i <- 0 until _input.length) {
      inputNbt.appendTag(new NBTTagByte(null, _input(i)))
    }
    nbt.setTag(Config.namespace + "redstone.input", inputNbt)

    val outputNbt = new NBTTagList()
    for (i <- 0 until _output.length) {
      outputNbt.appendTag(new NBTTagByte(null, _output(i)))
    }
    nbt.setTag(Config.namespace + "redstone.output", outputNbt)

    val bundledInputNbt = new NBTTagList()
    for (i <- 0 until _bundledInput.length) {
      val bundleNbt = new NBTTagList()
      for (j <- 0 until _bundledInput(i).length) {
        bundleNbt.appendTag(new NBTTagByte(null, _bundledInput(i)(j)))
      }
      bundledInputNbt.appendTag(bundleNbt)
    }
    nbt.setTag(Config.namespace + "redstone.bundledInput", bundledInputNbt)

    val bundledOutputNbt = new NBTTagList()
    for (i <- 0 until _bundledOutput.length) {
      val bundleNbt = new NBTTagList()
      for (j <- 0 until _bundledOutput(i).length) {
        bundleNbt.appendTag(new NBTTagByte(null, _bundledOutput(i)(j)))
      }
      bundledOutputNbt.appendTag(bundleNbt)
    }
    nbt.setTag(Config.namespace + "redstone.bundledOutput", bundledOutputNbt)
  }

  // ----------------------------------------------------------------------- //

  protected def computeInput(side: ForgeDirection): Int

  protected def computeBundledInput(side: ForgeDirection): Array[Byte]

  protected def onRedstoneInputChanged(side: ForgeDirection) {}

  protected def onRedstoneOutputChanged(side: ForgeDirection) {}

  // ----------------------------------------------------------------------- //

  @Optional.Method(modid = "RedLogic")
  def connects(wire: IWire, blockFace: Int, fromDirection: Int) = true

  @Optional.Method(modid = "RedLogic")
  def connectsAroundCorner(wire: IWire, blockFace: Int, fromDirection: Int) = false

  @Optional.Method(modid = "RedLogic")
  def getBundledCableStrength(blockFace: Int, toDirection: Int): Array[Byte] = _bundledOutput(ForgeDirection.getOrientation(toDirection).getOpposite.ordinal())

  @Optional.Method(modid = "RedLogic")
  def onBundledInputChanged() = checkRedstoneInputChanged()

  @Optional.Method(modid = "RedLogic")
  def getEmittedSignalStrength(blockFace: Int, toDirection: Int): Short = (_output(ForgeDirection.getOrientation(toDirection).getOpposite.ordinal()) & 0xFF).toShort

  @Optional.Method(modid = "RedLogic")
  def onRedstoneInputChanged() = checkRedstoneInputChanged()
}
