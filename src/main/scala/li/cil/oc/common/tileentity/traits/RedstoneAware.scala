package li.cil.oc.common.tileentity.traits

import cpw.mods.fml.common.Optional
import cpw.mods.fml.relauncher.{Side, SideOnly}
import li.cil.oc.Settings
import li.cil.oc.server.{PacketSender => ServerPacketSender}
import li.cil.oc.util.mods.Mods
import mods.immibis.redlogic.api.wiring.{IConnectable, IRedstoneEmitter, IRedstoneUpdatable, IWire}
import net.minecraft.block.Block
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.common.ForgeDirection

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
    if (oldInput >= 0 && input(side) != oldInput) {
      onRedstoneInputChanged(side)
    }
  }

  // ----------------------------------------------------------------------- //

  override def readFromNBT(nbt: NBTTagCompound) = {
    super.readFromNBT(nbt)

    val input = nbt.getIntArray(Settings.namespace + "rs.input")
    input.copyToArray(_input, 0, input.length min _input.length)
    val output = nbt.getIntArray(Settings.namespace + "rs.output")
    output.copyToArray(_output, 0, output.length min _output.length)
  }

  override def writeToNBT(nbt: NBTTagCompound) = {
    super.writeToNBT(nbt)

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
    val (sx, sy, sz) = (x + side.offsetX, y + side.offsetY, z + side.offsetZ)
    // See BlockRedstoneLogic.getInputStrength() for reference.
    val vanilla = math.max(world.getIndirectPowerLevelTo(sx, sy, sz, side.ordinal()),
      if (world.getBlockId(sx, sy, sz) == Block.redstoneWire.blockID) world.getBlockMetadata(sx, sy, sz) else 0)
    val redLogic = if (Mods.RedLogic.isAvailable) {
      world.getBlockTileEntity(sx, sy, sz) match {
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

  protected def onRedstoneInputChanged(side: ForgeDirection) {}

  protected def onRedstoneOutputEnabledChanged() {
    world.notifyBlocksOfNeighborChange(x, y, z, block.blockID)
    if (isServer) ServerPacketSender.sendRedstoneState(this)
    else world.markBlockForRenderUpdate(x, y, z)
  }

  protected def onRedstoneOutputChanged(side: ForgeDirection) {
    val nx = x + side.offsetX
    val ny = y + side.offsetY
    val nz = z + side.offsetZ
    world.notifyBlockOfNeighborChange(nx, ny, nz, block.blockID)
    world.notifyBlocksOfNeighborChange(nx, ny, nz, world.getBlockId(nx, ny, nz), side.getOpposite.ordinal)

    if (isServer) ServerPacketSender.sendRedstoneState(this)
    else world.markBlockForRenderUpdate(x, y, z)
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
