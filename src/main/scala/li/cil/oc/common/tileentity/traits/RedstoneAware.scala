package li.cil.oc.common.tileentity.traits

import cpw.mods.fml.common.Optional
import cpw.mods.fml.relauncher.Side
import cpw.mods.fml.relauncher.SideOnly
import li.cil.oc.Settings
import li.cil.oc.integration.Mods
import li.cil.oc.server.{PacketSender => ServerPacketSender}
import li.cil.oc.util.BlockPosition
import li.cil.oc.util.ExtendedWorld._
import mods.immibis.redlogic.api.wiring.IConnectable
import mods.immibis.redlogic.api.wiring.IRedstoneEmitter
import mods.immibis.redlogic.api.wiring.IRedstoneUpdatable
import mods.immibis.redlogic.api.wiring.IWire
import net.minecraft.init.Blocks
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
        for (i <- 0 until _output.length) {
          _output(i) = 0
        }
      }
      onRedstoneOutputEnabledChanged()
    }
    this
  }

  def input(side: ForgeDirection) = _input(side.ordinal())

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
        for (side <- ForgeDirection.VALID_DIRECTIONS) {
          updateRedstoneInput(side)
        }
      }
    }
  }

  protected def updateRedstoneInput(side: ForgeDirection) {
    val oldInput = _input(side.ordinal())
    val newInput = computeInput(side)
    _input(side.ordinal()) = newInput
    if (oldInput >= 0 && newInput != oldInput) {
      onRedstoneInputChanged(side, oldInput, newInput)
    }
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

  protected def computeInput(side: ForgeDirection) = {
    val blockPos = BlockPosition(x, y, z).offset(side)
    if (!world.blockExists(blockPos)) 0
    else {
      // See BlockRedstoneLogic.getInputStrength() for reference.
      val vanilla = math.max(world.getIndirectPowerLevelTo(blockPos, side),
        if (world.getBlock(blockPos) == Blocks.redstone_wire) world.getBlockMetadata(blockPos) else 0)
      val redLogic = if (Mods.RedLogic.isAvailable) {
        world.getTileEntity(blockPos) match {
          case emitter: IRedstoneEmitter =>
            var strength = 0
            for (i <- -1 to 5) {
              strength = math.max(strength, emitter.getEmittedSignalStrength(i, side.getOpposite.ordinal()))
            }
            strength
          case _ => 0
        }
      }
      else 0
      math.max(vanilla, redLogic)
    }
  }

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
