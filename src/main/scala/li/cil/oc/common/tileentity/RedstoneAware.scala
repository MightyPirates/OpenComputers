package li.cil.oc.common.tileentity

import cpw.mods.fml.common.{Loader, Optional}
import cpw.mods.fml.relauncher.{SideOnly, Side}
import li.cil.oc.Settings
import li.cil.oc.server.{PacketSender => ServerPacketSender}
import mods.immibis.redlogic.api.wiring._
import net.minecraft.init.Blocks
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.common.util.ForgeDirection

@Optional.InterfaceList(Array(
  new Optional.Interface(iface = "mods.immibis.redlogic.api.wiring.IConnectable", modid = "RedLogic"),
  new Optional.Interface(iface = "mods.immibis.redlogic.api.wiring.IRedstoneEmitter", modid = "RedLogic"),
  new Optional.Interface(iface = "mods.immibis.redlogic.api.wiring.IRedstoneUpdatable", modid = "RedLogic")
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
      onRedstoneOutputChanged(ForgeDirection.UNKNOWN)
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

  def updateRedstoneInput() {
    if (shouldUpdateInput) {
      shouldUpdateInput = false
      for (side <- ForgeDirection.VALID_DIRECTIONS) {
        val oldInput = _input(side.ordinal())
        val newInput = computeInput(side)
        _input(side.ordinal()) = newInput
        if (oldInput >= 0 && input(side) != oldInput) {
          onRedstoneInputChanged(side)
        }
      }
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
      if (world.getBlock(sx, sy, sz) == Blocks.redstone_wire) world.getBlockMetadata(sx, sy, sz) else 0)
    val redLogic = if (Loader.isModLoaded("RedLogic")) {
      world.getTileEntity(sx, sy, sz) match {
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

  protected def onRedstoneOutputChanged(side: ForgeDirection) {
    if (side == ForgeDirection.UNKNOWN) {
      world.notifyBlocksOfNeighborChange(x, y, z, block)
    }
    else {
      val nx = x + side.offsetX
      val ny = y + side.offsetY
      val nz = z + side.offsetZ
      world.notifyBlockOfNeighborChange(nx, ny, nz, block)
      world.notifyBlocksOfNeighborChange(nx, ny, nz, world.getBlock(nx, ny, nz))
    }
    if (isServer) ServerPacketSender.sendRedstoneState(this)
    else world.markBlockForUpdate(x, y, z)
  }

  // ----------------------------------------------------------------------- //

  @Optional.Method(modid = "RedLogic")
  override def connects(wire: IWire, blockFace: Int, fromDirection: Int) = isOutputEnabled

  @Optional.Method(modid = "RedLogic")
  override def connectsAroundCorner(wire: IWire, blockFace: Int, fromDirection: Int) = false

  @Optional.Method(modid = "RedLogic")
  override def getEmittedSignalStrength(blockFace: Int, toDirection: Int): Short = _output(toLocal(ForgeDirection.getOrientation(toDirection)).ordinal()).toShort

  @Optional.Method(modid = "RedLogic")
  override def onRedstoneInputChanged() = checkRedstoneInputChanged()
}
