package li.cil.oc.server.component

import li.cil.oc.common.tileentity
import net.minecraft.nbt.{NBTTagByte, NBTTagList, NBTTagCompound}
import net.minecraftforge.common.ForgeDirection
import li.cil.oc.util.Persistable

trait Redstone extends tileentity.Environment with Persistable {
  private val _input = Array.fill[Byte](6)(-1)

  private val _output = Array.fill[Byte](6)(0)

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
      }
      onRedstoneOutputChanged(ForgeDirection.UNKNOWN)
    }
    this
  }

  def input(side: ForgeDirection): Int = _input(side.ordinal())

  def output(side: ForgeDirection): Int = _output(side.ordinal())

  def output(side: ForgeDirection, value: Int): Unit = if (value != output(side)) {
    _output(side.ordinal()) = (value max 0 min 15).toByte
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
        val oldInput = _input(side.ordinal())
        val newInput = computeInput(side)
        _input(side.ordinal()) = newInput.toByte
        if (oldInput >= 0 && _input(side.ordinal()) != oldInput) {
          onRedstoneInputChanged(side)
        }
      }
    }
  }

  // ----------------------------------------------------------------------- //

  override def load(nbt: NBTTagCompound) = {
    super.load(nbt)

    if (nbt.hasKey("oc.rs.input")) {
      val inputNbt = nbt.getTagList("oc.rs.input")
      for (i <- 0 until (_input.length min inputNbt.tagCount)) {
        _input(i) = inputNbt.tagAt(i).asInstanceOf[NBTTagByte].data
      }
    }

    if (nbt.hasKey("oc.rs.output")) {
      val outputNbt = nbt.getTagList("oc.rs.output")
      for (i <- 0 until (_output.length min outputNbt.tagCount)) {
        _output(i) = outputNbt.tagAt(i).asInstanceOf[NBTTagByte].data
      }
    }
  }

  override def save(nbt: NBTTagCompound) = {
    super.save(nbt)

    val inputNbt = new NBTTagList()
    for (i <- 0 until _input.length) {
      inputNbt.appendTag(new NBTTagByte(null, _input(i)))
    }
    nbt.setTag("oc.rs.input", inputNbt)

    val outputNbt = new NBTTagList()
    for (i <- 0 until _output.length) {
      outputNbt.appendTag(new NBTTagByte(null, _output(i)))
    }
    nbt.setTag("oc.rs.output", outputNbt)
  }

  // ----------------------------------------------------------------------- //

  protected def computeInput(side: ForgeDirection): Int

  protected def onRedstoneInputChanged(side: ForgeDirection) {}

  protected def onRedstoneOutputChanged(side: ForgeDirection) {}
}
