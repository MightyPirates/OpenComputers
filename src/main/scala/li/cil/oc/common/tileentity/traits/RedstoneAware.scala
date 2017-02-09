package li.cil.oc.common.tileentity.traits

import li.cil.oc.Settings
import li.cil.oc.common.EventHandler
import li.cil.oc.integration.Mods
import li.cil.oc.integration.util.BundledRedstone
import li.cil.oc.server.{PacketSender => ServerPacketSender}
import net.minecraftforge.fml.common.Optional
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly

/* TODO RedLogic
import mods.immibis.redlogic.api.wiring.IConnectable
import mods.immibis.redlogic.api.wiring.IRedstoneEmitter
import mods.immibis.redlogic.api.wiring.IRedstoneUpdatable
import mods.immibis.redlogic.api.wiring.IWire
*/

import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.EnumFacing

@Optional.InterfaceList(Array(
  new Optional.Interface(iface = "mods.immibis.redlogic.api.wiring.IConnectable", modid = Mods.IDs.RedLogic),
  new Optional.Interface(iface = "mods.immibis.redlogic.api.wiring.IRedstoneEmitter", modid = Mods.IDs.RedLogic),
  new Optional.Interface(iface = "mods.immibis.redlogic.api.wiring.IRedstoneUpdatable", modid = Mods.IDs.RedLogic)
))
trait RedstoneAware extends RotationAware /* with IConnectable with IRedstoneEmitter with IRedstoneUpdatable TODO RedLogic */ {
  protected[tileentity] val _input = Array.fill(6)(-1)

  protected[tileentity] val _output = Array.fill(6)(0)

  protected var _isOutputEnabled = false

  protected var shouldUpdateInput = true

  def isOutputEnabled = _isOutputEnabled

  def isOutputEnabled_=(value: Boolean) = {
    if (value != isOutputEnabled) {
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

  def input(side: EnumFacing) = _input(side.ordinal()) max 0

  def input(side: EnumFacing, newInput: Int): Unit = {
    val oldInput = _input(side.ordinal())
    _input(side.ordinal()) = newInput
    if (oldInput >= 0 && newInput != oldInput) {
      onRedstoneInputChanged(side, oldInput, newInput)
    }
  }

  def maxInput = EnumFacing.values.map(input).max

  def output(side: EnumFacing) = _output(toLocal(side).ordinal())

  def output(side: EnumFacing, value: Int): Unit = if (value != output(side)) {
    _output(toLocal(side).ordinal()) = value

    onRedstoneOutputChanged(side)
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

  def updateRedstoneInput(side: EnumFacing) {
    input(side, BundledRedstone.computeInput(position, side))
  }

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
    isOutputEnabled = nbt.getBoolean("isOutputEnabled")
    nbt.getIntArray("output").copyToArray(_output)
  }

  override def writeToNBTForClient(nbt: NBTTagCompound) {
    super.writeToNBTForClient(nbt)
    nbt.setBoolean("isOutputEnabled", isOutputEnabled)
    nbt.setIntArray("output", _output)
  }

  // ----------------------------------------------------------------------- //

  protected def onRedstoneInputChanged(side: EnumFacing, oldMaxValue: Int, newMaxValue: Int) {}

  protected def onRedstoneOutputEnabledChanged() {
    if (world != null) {
      world.notifyNeighborsOfStateChange(getPos, getBlockType, true)
      if (isServer) ServerPacketSender.sendRedstoneState(this)
      else world.notifyBlockUpdate(getPos, getWorld.getBlockState(getPos), getWorld.getBlockState(getPos), 3)
    }
  }

  protected def onRedstoneOutputChanged(side: EnumFacing) {
    val blockPos = getPos.offset(side)
    world.neighborChanged(blockPos, getBlockType, blockPos)
    world.notifyNeighborsOfStateExcept(blockPos, world.getBlockState(blockPos).getBlock, side.getOpposite)

    if (isServer) ServerPacketSender.sendRedstoneState(this)
    else world.notifyBlockUpdate(getPos, getWorld.getBlockState(getPos), getWorld.getBlockState(getPos), 3)
  }

  // ----------------------------------------------------------------------- //
  /* TODO RedLogic
    @Optional.Method(modid = Mods.IDs.RedLogic)
    override def connects(wire: IWire, blockFace: Int, fromDirection: Int) = isOutputEnabled

    @Optional.Method(modid = Mods.IDs.RedLogic)
    override def connectsAroundCorner(wire: IWire, blockFace: Int, fromDirection: Int) = false

    @Optional.Method(modid = Mods.IDs.RedLogic)
    override def getEmittedSignalStrength(blockFace: Int, toDirection: Int): Short = _output(toLocal(ForgeDirection.getOrientation(toDirection)).ordinal()).toShort

    @Optional.Method(modid = Mods.IDs.RedLogic)
    override def onRedstoneInputChanged() = checkRedstoneInputChanged()
  */
}
