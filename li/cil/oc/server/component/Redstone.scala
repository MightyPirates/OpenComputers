package li.cil.oc.server.component

import li.cil.oc.api.network.{Message, Node}
import net.minecraft.nbt.{NBTTagByte, NBTTagList, NBTTagCompound}
import net.minecraftforge.common.ForgeDirection

trait Redstone extends Node {
  private val _output = Array.fill[Byte](6)(0)

  private var _isOutputEnabled = true

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

  // ----------------------------------------------------------------------- //

  override def receive(message: Message) = super.receive(message).orElse {
    message.data match {
      case Array(side: ForgeDirection) if message.name == "redstone.input" && side != ForgeDirection.UNKNOWN =>
        result(input(side))
      case Array(side: ForgeDirection) if message.name == "redstone.output" && side != ForgeDirection.UNKNOWN =>
        result(output(side))
      case Array(side: ForgeDirection, value: Int) if message.name == "redstone.output=" && side != ForgeDirection.UNKNOWN =>
        output(side, value)
        result(true)
      case _ => None
    }
  }

  // ----------------------------------------------------------------------- //

  override def load(nbt: NBTTagCompound) = {
    super.load(nbt)

    if (nbt.hasKey("output")) {
      val outputNbt = nbt.getTagList("output")
      for (i <- 0 until (_output.length min outputNbt.tagCount)) {
        _output(i) = outputNbt.tagAt(i).asInstanceOf[NBTTagByte].data
      }
    }
  }

  override def save(nbt: NBTTagCompound) = {
    super.save(nbt)

    val outputNbt = new NBTTagList()
    for (i <- 0 until _output.length) {
      outputNbt.appendTag(new NBTTagByte(null, _output(i)))
    }
    nbt.setTag("output", outputNbt)
  }

  // ----------------------------------------------------------------------- //

  protected def onRedstoneOutputChanged(side: ForgeDirection) {}
}
