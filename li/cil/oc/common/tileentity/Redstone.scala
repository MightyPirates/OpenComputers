package li.cil.oc.common.tileentity

import cpw.mods.fml.common.Optional.Interface
import cpw.mods.fml.common.{Loader, Optional}
import li.cil.oc.Config
import li.cil.oc.api.network
import li.cil.oc.server.{PacketSender => ServerPacketSender}
import li.cil.oc.util.ExtendedNBT._
import li.cil.oc.util.Persistable
import mods.immibis.redlogic.api.wiring._
import net.minecraft.nbt.{NBTTagByteArray, NBTTagCompound}
import net.minecraftforge.common.ForgeDirection

@Optional.InterfaceList(Array(
  new Interface(iface = "mods.immibis.redlogic.api.wiring.IConnectable", modid = "RedLogic"),
  new Interface(iface = "mods.immibis.redlogic.api.wiring.IBundledEmitter", modid = "RedLogic"),
  new Interface(iface = "mods.immibis.redlogic.api.wiring.IBundledUpdatable", modid = "RedLogic"),
  new Interface(iface = "mods.immibis.redlogic.api.wiring.IRedstoneEmitter", modid = "RedLogic"),
  new Interface(iface = "mods.immibis.redlogic.api.wiring.IRedstoneUpdatable", modid = "RedLogic")
))
trait Redstone extends TileEntity with network.Environment with Rotatable with Persistable
with IConnectable with IBundledEmitter with IBundledUpdatable with IRedstoneEmitter with IRedstoneUpdatable {
  private val _input = Array.fill[Byte](6)(-1)

  private val _output = Array.fill[Byte](6)(0)

  private val _bundledInput = Array.fill[Array[Byte]](6)(Array.fill[Byte](16)(-1))

  private val _bundledOutput = Array.fill[Array[Byte]](6)(Array.fill[Byte](16)(0))

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
        for (i <- 0 until _bundledOutput.length) {
          for (j <- 0 until _bundledOutput(i).length) {
            _bundledOutput(i)(j) = 0.toByte
          }
        }
      }
      onRedstoneOutputChanged(ForgeDirection.UNKNOWN)
    }
    this
  }

  def input(side: ForgeDirection) = (_input(side.ordinal()) & 0xFF).toShort

  def output(side: ForgeDirection) = (_output(side.ordinal()) & 0xFF).toShort

  def output(side: ForgeDirection, value: Short): Unit = if (value != output(side)) {
    _output(side.ordinal()) = (value max 0 min 255).toByte
    onRedstoneOutputChanged(side)
  }

  def bundledInput(side: ForgeDirection, color: Int) = (_bundledInput(side.ordinal())(color) & 0xFF).toShort

  def bundledOutput(side: ForgeDirection, color: Int) =
    (_bundledOutput(side.ordinal())(color) & 0xFF).toShort

  def bundledOutput(side: ForgeDirection, color: Int, value: Short): Unit = if (value != bundledOutput(side, color)) {
    _bundledOutput(side.ordinal())(color) = (value max 0 min 255).toByte
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
        val (oldInput, oldBundledInput) = (_input(side.ordinal()), _bundledInput(side.ordinal()))
        val (newInput, newBundledInput) = (computeInput(side), computeBundledInput(side))
        _input(side.ordinal()) = (newInput max 0 min 255).toByte
        var changed = oldInput >= 0 && input(side) != oldInput
        if (newBundledInput != null) for (i <- 0 until 16) {
          changed = changed || oldBundledInput(i) != newBundledInput(i)
          oldBundledInput(i) = newBundledInput(i)
        }
        else for (i <- 0 until 16) {
          changed = changed || oldBundledInput(i) != 0.toByte
          oldBundledInput(i) = 0.toByte
        }
        if (changed) {
          onRedstoneInputChanged(side)
        }
      }
    }
  }

  // ----------------------------------------------------------------------- //

  override def load(nbt: NBTTagCompound) = {
    super.load(nbt)

    nbt.getByteArray(Config.namespace + "rs.input").copyToArray(_input)
    nbt.getByteArray(Config.namespace + "rs.output").copyToArray(_output)

    nbt.getTagList(Config.namespace + "rs.bundledInput").iterator[NBTTagByteArray].zipWithIndex.foreach {
      case (input, side) => input.byteArray.copyToArray(_bundledInput(side))
    }
    nbt.getTagList(Config.namespace + "rs.bundledOutput").iterator[NBTTagByteArray].zipWithIndex.foreach {
      case (input, side) => input.byteArray.copyToArray(_bundledOutput(side))
    }
  }

  override def save(nbt: NBTTagCompound) = {
    super.save(nbt)

    nbt.setByteArray(Config.namespace + "rs.input", _input)
    nbt.setByteArray(Config.namespace + "rs.output", _output)

    nbt.setNewTagList(Config.namespace + "rs.bundledInput", _bundledInput.view)
    nbt.setNewTagList(Config.namespace + "rs.bundledOutput", _bundledOutput.view)
  }

  // ----------------------------------------------------------------------- //

  protected def computeInput(side: ForgeDirection) = {
    world.getIndirectPowerLevelTo(
      x + side.offsetX,
      y + side.offsetY,
      z + side.offsetZ,
      side.ordinal())
  }

  protected def computeBundledInput(side: ForgeDirection) = {
    if (Loader.isModLoaded("RedLogic")) {
      world.getBlockTileEntity(
        x + side.offsetX,
        y + side.offsetY,
        z + side.offsetZ) match {
        case wire: IInsulatedRedstoneWire =>
          var strength: Array[Byte] = null
          for (face <- -1 to 5 if wire.wireConnectsInDirection(face, side.ordinal()) && strength == null) {
            strength = Array.fill[Byte](16)(0)
            strength(wire.getInsulatedWireColour) = wire.getEmittedSignalStrength(face, side.ordinal()).toByte
          }
          strength
        case emitter: IBundledEmitter =>
          var strength: Array[Byte] = null
          for (i <- -1 to 5 if strength == null) {
            strength = emitter.getBundledCableStrength(i, side.getOpposite.ordinal())
          }
          strength
        case _ => null
      }
    } else null
  }

  protected def onRedstoneInputChanged(side: ForgeDirection) {}

  protected def onRedstoneOutputChanged(side: ForgeDirection) {
    if (side == ForgeDirection.UNKNOWN) {
      world.notifyBlocksOfNeighborChange(x, y, z, block.blockID)
    }
    else {
      val (nx, ny, nz) = (x + side.offsetX, y + side.offsetY, z + side.offsetZ)
      world.notifyBlockOfNeighborChange(nx, ny, nz, block.blockID)
      world.notifyBlocksOfNeighborChange(nx, ny, nz, world.getBlockId(nx, ny, nz))
    }
    if (isServer) ServerPacketSender.sendRedstoneState(this)
    else world.markBlockForRenderUpdate(x, y, z)
  }

  // ----------------------------------------------------------------------- //

  @Optional.Method(modid = "RedLogic")
  def connects(wire: IWire, blockFace: Int, fromDirection: Int) = true

  @Optional.Method(modid = "RedLogic")
  def connectsAroundCorner(wire: IWire, blockFace: Int, fromDirection: Int) = false

  @Optional.Method(modid = "RedLogic")
  def getBundledCableStrength(blockFace: Int, toDirection: Int): Array[Byte] = _bundledOutput(ForgeDirection.getOrientation(toDirection).ordinal())

  @Optional.Method(modid = "RedLogic")
  def onBundledInputChanged() = checkRedstoneInputChanged()

  @Optional.Method(modid = "RedLogic")
  def getEmittedSignalStrength(blockFace: Int, toDirection: Int): Short = (_output(ForgeDirection.getOrientation(toDirection).ordinal()) & 0xFF).toShort

  @Optional.Method(modid = "RedLogic")
  def onRedstoneInputChanged() = checkRedstoneInputChanged()
}
