package li.cil.oc.common.tileentity.traits

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

@Optional.InterfaceList(Array(
  new Optional.Interface(iface = "mods.immibis.redlogic.api.wiring.IConnectable", modid = Mods.IDs.RedLogic),
  new Optional.Interface(iface = "mods.immibis.redlogic.api.wiring.IRedstoneEmitter", modid = Mods.IDs.RedLogic),
  new Optional.Interface(iface = "mods.immibis.redlogic.api.wiring.IRedstoneUpdatable", modid = Mods.IDs.RedLogic)
))
trait RedstoneAware extends RotationAware with IConnectable with IRedstoneEmitter with IRedstoneUpdatable {
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

  def input(side: ForgeDirection) = _input(side.ordinal()) max 0

  def input(side: ForgeDirection, newInput: Int): Unit = {
    val oldInput = _input(side.ordinal())
    _input(side.ordinal()) = newInput
    if (oldInput >= 0 && newInput != oldInput) {
      onRedstoneInputChanged(side, oldInput, newInput)
    }
  }

  def maxInput = ForgeDirection.VALID_DIRECTIONS.map(input).max

  def output(side: ForgeDirection) = _output(toLocal(side).ordinal())

  def output(side: ForgeDirection, value: Int): Unit = if (value != output(side)) {
    _output(toLocal(side).ordinal()) = value

    onRedstoneOutputChanged(side)
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

  def updateRedstoneInput(side: ForgeDirection) {
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

  protected def onRedstoneInputChanged(side: ForgeDirection, oldMaxValue: Int, newMaxValue: Int) {}

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
  override def connects(wire: IWire, blockFace: Int, fromDirection: Int) = isOutputEnabled

  @Optional.Method(modid = Mods.IDs.RedLogic)
  override def connectsAroundCorner(wire: IWire, blockFace: Int, fromDirection: Int) = false

  @Optional.Method(modid = Mods.IDs.RedLogic)
  override def getEmittedSignalStrength(blockFace: Int, toDirection: Int): Short = _output(toLocal(ForgeDirection.getOrientation(toDirection)).ordinal()).toShort

  @Optional.Method(modid = Mods.IDs.RedLogic)
  override def onRedstoneInputChanged() = checkRedstoneInputChanged()
}
