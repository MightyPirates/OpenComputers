package li.cil.oc.server.component

import li.cil.oc.api.network.{Message, Node}
import net.minecraft.nbt.{NBTTagByte, NBTTagList, NBTTagCompound}
import net.minecraftforge.common.ForgeDirection

trait Redstone extends Node {
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

  def input(side: ForgeDirection): Int

  def output(side: ForgeDirection): Int = _output(side.ordinal())

  def output(side: ForgeDirection, value: Int): Unit = if (value != output(side)) {
    _output(side.ordinal()) = (value max 0 min 15).toByte
    onRedstoneOutputChanged(side)
  }

  def checkRedstoneInputChanged() {
    _shouldUpdateInput = true
  }

  // ----------------------------------------------------------------------- //

  override def update() {
    super.update()
    if (_shouldUpdateInput) {
      _shouldUpdateInput = false
      for (side <- ForgeDirection.VALID_DIRECTIONS) {
        val oldInput = _input(side.ordinal())
        val newInput = input(side)
        _input(side.ordinal()) = newInput.toByte
        if (oldInput >= 0 && _input(side.ordinal()) != oldInput) {
          onRedstoneInputChanged(side)
        }
      }
    }
  }

  override def receive(message: Message) = super.receive(message).orElse {
    message.data match {
      case Array(side: ForgeDirection) if message.name == "redstone.input" && side != ForgeDirection.UNKNOWN =>
        result(_input(side.ordinal()))
      case Array(side: ForgeDirection) if message.name == "redstone.output" && side != ForgeDirection.UNKNOWN =>
        result(output(side))
      case Array(side: ForgeDirection, value: Int) if message.name == "redstone.output=" && side != ForgeDirection.UNKNOWN =>
        output(side, value)
        result(true)
      case _ => None
    }
  }

  // ----------------------------------------------------------------------- //

  override abstract def readFromNBT(nbt: NBTTagCompound) = {
    super.readFromNBT(nbt)

    if (nbt.hasKey("redstone.input")) {
      val inputNbt = nbt.getTagList("redstone.input")
      for (i <- 0 until (_input.length min inputNbt.tagCount)) {
        _input(i) = inputNbt.tagAt(i).asInstanceOf[NBTTagByte].data
      }
    }

    if (nbt.hasKey("redstone.output")) {
      val outputNbt = nbt.getTagList("redstone.output")
      for (i <- 0 until (_output.length min outputNbt.tagCount)) {
        _output(i) = outputNbt.tagAt(i).asInstanceOf[NBTTagByte].data
      }
    }
  }

  override abstract def writeToNBT(nbt: NBTTagCompound) = {
    super.writeToNBT(nbt)

    val inputNbt = new NBTTagList()
    for (i <- 0 until _input.length) {
      inputNbt.appendTag(new NBTTagByte(null, _input(i)))
    }
    nbt.setTag("redstone.input", inputNbt)

    val outputNbt = new NBTTagList()
    for (i <- 0 until _output.length) {
      outputNbt.appendTag(new NBTTagByte(null, _output(i)))
    }
    nbt.setTag("redstone.output", outputNbt)
  }

  // ----------------------------------------------------------------------- //

  protected def onRedstoneInputChanged(side: ForgeDirection) {}

  protected def onRedstoneOutputChanged(side: ForgeDirection) {}
}
