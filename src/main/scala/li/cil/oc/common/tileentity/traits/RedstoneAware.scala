package li.cil.oc.common.tileentity.traits

import java.util

import cpw.mods.fml.common.Optional
import cpw.mods.fml.relauncher.Side
import cpw.mods.fml.relauncher.SideOnly
import li.cil.oc.Settings
import li.cil.oc.common.EventHandler
import li.cil.oc.integration.Mods
import li.cil.oc.integration.util.BundledRedstone
import li.cil.oc.server.{PacketSender => ServerPacketSender}
import li.cil.oc.util.ExtendedWorld._
import mods.immibis.redlogic.api.wiring.IConnectable
import mods.immibis.redlogic.api.wiring.IRedstoneEmitter
import mods.immibis.redlogic.api.wiring.IRedstoneUpdatable
import mods.immibis.redlogic.api.wiring.IWire
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.common.util.ForgeDirection

case class RedstoneChangedEventArgs (side: ForgeDirection, oldValue: Int, newValue: Int, color: Int = -1)

@Optional.InterfaceList(Array(
  new Optional.Interface(iface = "mods.immibis.redlogic.api.wiring.IConnectable", modid = Mods.IDs.RedLogic),
  new Optional.Interface(iface = "mods.immibis.redlogic.api.wiring.IRedstoneEmitter", modid = Mods.IDs.RedLogic),
  new Optional.Interface(iface = "mods.immibis.redlogic.api.wiring.IRedstoneUpdatable", modid = Mods.IDs.RedLogic)
))
trait RedstoneAware extends RotationAware with IConnectable with IRedstoneEmitter with IRedstoneUpdatable {
  protected[tileentity] val _input: Array[Int] = Array.fill(6)(-1)

  protected[tileentity] val _output: Array[Int] = Array.fill(6)(0)

  protected var _isOutputEnabled: Boolean = false

  def isOutputEnabled: Boolean = _isOutputEnabled

  protected var shouldUpdateInput: Boolean = true

  def setOutputEnabled(value: Boolean): Unit = {
    if (value != _isOutputEnabled) {
      _isOutputEnabled = value
      if (!value) {
        for (i <- _output.indices) {
          _output(i) = 0
        }
      }
      onRedstoneOutputEnabledChanged()
    }
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

  def getInput(side: ForgeDirection): Int = _input(side.ordinal) max 0

  def setInput(side: ForgeDirection, newInput: Int): Unit = {
    val oldInput = _input(side.ordinal())
    _input(side.ordinal()) = newInput
    if (oldInput >= 0 && newInput != oldInput) {
      onRedstoneInputChanged(RedstoneChangedEventArgs(side, oldInput, newInput))
    }
  }

  def setInput(values: Array[Int]): Unit = {
    for (side <- ForgeDirection.VALID_DIRECTIONS) {
      val value = if (side.ordinal <= values.length) values(side.ordinal) else 0
      setInput(side, value)
    }
  }

  def maxInput: Int = _input.map(math.max(_, 0)).max

  def getOutput: Array[Int] = ForgeDirection.VALID_DIRECTIONS.map{ side: ForgeDirection => _output(toLocal(side).ordinal) }

  def getOutput(side: ForgeDirection) = Option(_output) match {
    case Some(output) => output(toLocal(side).ordinal())
    case _ => 0
  }

  def setOutput(side: ForgeDirection, value: Int): Boolean = {
    if (value == getOutput(side)) return false
    _output(toLocal(side).ordinal()) = value
    onRedstoneOutputChanged(side)
    true
  }

  def setOutput(values: util.Map[_, _]): Boolean = {
    var changed: Boolean = false
    ForgeDirection.VALID_DIRECTIONS.foreach(side => {
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
    shouldUpdateInput = isServer
  }

  // ----------------------------------------------------------------------- //

  override def updateEntity() {
    super.updateEntity()
    if (isServer) {
      if (shouldUpdateInput) {
        shouldUpdateInput = false
        ForgeDirection.VALID_DIRECTIONS.foreach(updateRedstoneInput)
      }
    }
  }

  override def validate(): Unit = {
    super.validate()
    if (!canUpdate) {
      EventHandler.scheduleServer(() => ForgeDirection.VALID_DIRECTIONS.foreach(updateRedstoneInput))
    }
  }

  def updateRedstoneInput(side: ForgeDirection): Unit = setInput(side, BundledRedstone.computeInput(position, side))

  // ----------------------------------------------------------------------- //

  override def readFromNBTForServer(nbt: NBTTagCompound) = {
    super.readFromNBTForServer(nbt)

    val input = nbt.getIntArray(Settings.namespace + "rs.input")
    input.copyToArray(_input, 0, input.length min _input.length)
    val output = nbt.getIntArray(Settings.namespace + "rs.output")
    output.copyToArray(_output, 0, output.length min _output.length)
  }

  override def writeToNBTForServer(nbt: NBTTagCompound) = {
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
    world.notifyBlocksOfNeighborChange(position, block)
    if (isServer) ServerPacketSender.sendRedstoneState(this)
    else world.markBlockForUpdate(position)
  }

  protected def onRedstoneOutputChanged(side: ForgeDirection) {
    val blockPos = position.offset(side)
    world.notifyBlockOfNeighborChange(blockPos, block)
    world.notifyBlocksOfNeighborChange(blockPos, world.getBlock(blockPos), side.getOpposite)

    if (isServer) ServerPacketSender.sendRedstoneState(this)
    else world.markBlockForUpdate(position)
  }

  // ----------------------------------------------------------------------- //

  @Optional.Method(modid = Mods.IDs.RedLogic)
  override def connects(wire: IWire, blockFace: Int, fromDirection: Int): Boolean = _isOutputEnabled

  @Optional.Method(modid = Mods.IDs.RedLogic)
  override def connectsAroundCorner(wire: IWire, blockFace: Int, fromDirection: Int) = false

  @Optional.Method(modid = Mods.IDs.RedLogic)
  override def getEmittedSignalStrength(blockFace: Int, toDirection: Int): Short = _output(toLocal(ForgeDirection.getOrientation(toDirection)).ordinal()).toShort

  @Optional.Method(modid = Mods.IDs.RedLogic)
  override def onRedstoneInputChanged(): Unit = checkRedstoneInputChanged()
}
