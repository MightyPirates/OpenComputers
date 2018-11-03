package li.cil.oc.common.tileentity.traits

import java.util
import li.cil.oc.Settings
import li.cil.oc.common.EventHandler
import li.cil.oc.integration.util.BundledRedstone
import li.cil.oc.server.{PacketSender => ServerPacketSender}
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.EnumFacing
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly

case class RedstoneChangedEventArgs (side: EnumFacing, oldValue: Int, newValue: Int, color: Int = -1)

trait RedstoneAware extends RotationAware {
  protected[tileentity] val _input: Array[Int] = Array.fill(6)(-1)

  protected[tileentity] val _output: Array[Int] = Array.fill(6)(0)

  protected var _isOutputEnabled: Boolean = false

  protected var shouldUpdateInput = true

  def isOutputEnabled: Boolean = _isOutputEnabled

  def setOutputEnabled(value: Boolean): RedstoneAware = {
    if (value != _isOutputEnabled) {
      _isOutputEnabled = value
      if (!value) {
        for (i <- _output.indices) {
          _output(i) = 0
        }
      }
      onRedstoneOutputEnabledChanged()
    }
    this
  }

  protected def getObjectFuzzy(map: util.Map[_, _], key: Int): Option[AnyRef] = {
    val refMap: util.Map[AnyRef, AnyRef] = map.asInstanceOf[util.Map[AnyRef, AnyRef]]
    if (refMap.containsKey(key))
      Option(refMap.get(key))
    else if (refMap.containsKey(new Integer(key)))
      Option(refMap.get(new Integer(key)))
    else if (refMap.containsKey(new Integer(key) * 1.0))
      Option(refMap.get(new Integer(key) * 1.0))
    else if (refMap.containsKey(key * 1.0))
      Option(refMap.get(key * 1.0))
    else
      None
  }

  protected def valueToInt(value: AnyRef): Option[Int] = {
    value match {
      case Some(num: Number) => Option(num.intValue)
      case _ => None
    }
  }

  def getInput: Array[Int] = _input.map(math.max(_, 0))

  def getInput(side: EnumFacing): Int = _input(side.ordinal) max 0

  def setInput(side: EnumFacing, newInput: Int): Unit = {
    val oldInput = _input(side.ordinal())
    _input(side.ordinal()) = newInput
    if (oldInput >= 0 && newInput != oldInput) {
      onRedstoneInputChanged(RedstoneChangedEventArgs(side, oldInput, newInput))
    }
  }

  def setInput(values: Array[Int]): Unit = {
    for (side <- EnumFacing.values) {
      val value = if (side.ordinal <= values.length) values(side.ordinal) else 0
      setInput(side, value)
    }
  }

  def maxInput: Int = _input.map(math.max(_, 0)).max

  def getOutput: Array[Int] = EnumFacing.values.map{ side: EnumFacing => _output(toLocal(side).ordinal) }

  def getOutput(side: EnumFacing) = _output(toLocal(side).ordinal())

  def setOutput(side: EnumFacing, value: Int): Boolean = {
    if (value == getOutput(side)) return false
    _output(toLocal(side).ordinal()) = value
    onRedstoneOutputChanged(side)
    true
  }

  def setOutput(values: util.Map[_, _]): Boolean = {
    var changed: Boolean = false
    EnumFacing.values.foreach(side => {
      val sideIndex = toLocal(side).ordinal
      // due to a bug in our jnlua layer, I cannot loop the map
      valueToInt(getObjectFuzzy(values, sideIndex)) match {
        case Some(num: Int) if setOutput(side, num) => changed = true
        case _ =>
      }
    })
    changed
  }

  def checkRedstoneInputChanged() {
    if (this.isInstanceOf[Tickable]) {
      shouldUpdateInput = isServer
    } else {
      EnumFacing.values().foreach(updateRedstoneInput)
    }
  }

  // ----------------------------------------------------------------------- //

  override def updateEntity() {
    super.updateEntity()
    if (isServer) {
      if (shouldUpdateInput) {
        shouldUpdateInput = false
        EnumFacing.values().foreach(updateRedstoneInput)
      }
    }
  }

  override def validate(): Unit = {
    super.validate()
    if (!this.isInstanceOf[Tickable]) {
      EventHandler.scheduleServer(() => EnumFacing.values().foreach(updateRedstoneInput))
    }
  }

  def updateRedstoneInput(side: EnumFacing): Unit = setInput(side, BundledRedstone.computeInput(position, side))

  // ----------------------------------------------------------------------- //

  override def readFromNBTForServer(nbt: NBTTagCompound): Unit = {
    super.readFromNBTForServer(nbt)

    val input = nbt.getIntArray(Settings.namespace + "rs.input")
    input.copyToArray(_input, 0, input.length min _input.length)
    val output = nbt.getIntArray(Settings.namespace + "rs.output")
    output.copyToArray(_output, 0, output.length min _output.length)
  }

  override def writeToNBTForServer(nbt: NBTTagCompound): Unit = {
    super.writeToNBTForServer(nbt)

    nbt.setIntArray(Settings.namespace + "rs.input", _input)
    nbt.setIntArray(Settings.namespace + "rs.output", _output)
  }

  @SideOnly(Side.CLIENT)
  override def readFromNBTForClient(nbt: NBTTagCompound) {
    super.readFromNBTForClient(nbt)
    _isOutputEnabled = nbt.getBoolean("isOutputEnabled")
    nbt.getIntArray("output").copyToArray(_output)
  }

  override def writeToNBTForClient(nbt: NBTTagCompound) {
    super.writeToNBTForClient(nbt)
    nbt.setBoolean("isOutputEnabled", _isOutputEnabled)
    nbt.setIntArray("output", _output)
  }

  // ----------------------------------------------------------------------- //

  protected def onRedstoneInputChanged(args: RedstoneChangedEventArgs) {}

  protected def onRedstoneOutputEnabledChanged() {
    if (getWorld != null) {
      getWorld.notifyNeighborsOfStateChange(getPos, getBlockType, true)
      if (isServer) ServerPacketSender.sendRedstoneState(this)
      else getWorld.notifyBlockUpdate(getPos, getWorld.getBlockState(getPos), getWorld.getBlockState(getPos), 3)
    }
  }

  protected def onRedstoneOutputChanged(side: EnumFacing) {
    val blockPos = getPos.offset(side)
    getWorld.neighborChanged(blockPos, getBlockType, blockPos)
    getWorld.notifyNeighborsOfStateExcept(blockPos, getWorld.getBlockState(blockPos).getBlock, side.getOpposite)

    if (isServer) ServerPacketSender.sendRedstoneState(this)
    else getWorld.notifyBlockUpdate(getPos, getWorld.getBlockState(getPos), getWorld.getBlockState(getPos), 3)
  }
}
