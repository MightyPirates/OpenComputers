package li.cil.oc.common.tileentity

import cpw.mods.fml.common.Optional.Interface
import cpw.mods.fml.common.{Loader, Optional}
import li.cil.oc.Config
import li.cil.oc.api.network
import li.cil.oc.server.{PacketSender => ServerPacketSender}
import li.cil.oc.util.ExtendedNBT._
import li.cil.oc.util.Persistable
import mods.immibis.redlogic.api.wiring._
import net.minecraft.block.Block
import net.minecraft.nbt.{NBTTagIntArray, NBTTagCompound}
import net.minecraftforge.common.ForgeDirection
import powercrystals.minefactoryreloaded.api.rednet.IRedNetNetworkContainer

@Optional.InterfaceList(Array(
  new Interface(iface = "mods.immibis.redlogic.api.wiring.IConnectable", modid = "RedLogic"),
  new Interface(iface = "mods.immibis.redlogic.api.wiring.IBundledEmitter", modid = "RedLogic"),
  new Interface(iface = "mods.immibis.redlogic.api.wiring.IBundledUpdatable", modid = "RedLogic"),
  new Interface(iface = "mods.immibis.redlogic.api.wiring.IRedstoneEmitter", modid = "RedLogic"),
  new Interface(iface = "mods.immibis.redlogic.api.wiring.IRedstoneUpdatable", modid = "RedLogic")
))
trait Redstone extends TileEntity with network.Environment with Rotatable with Persistable
with IConnectable with IBundledEmitter with IBundledUpdatable with IRedstoneEmitter with IRedstoneUpdatable {
  private val _input = Array.fill(6)(-1)

  private val _output = Array.fill(6)(0)

  private val _bundledInput = Array.fill(6)(Array.fill(16)(-1))

  private val _rednetInput = Array.fill(6)(Array.fill(16)(-1))

  private val _bundledOutput = Array.fill(6)(Array.fill(16)(0))

  private var _isOutputEnabled = false

  private var shouldUpdateInput = true

  def isOutputEnabled = _isOutputEnabled

  def isOutputEnabled_=(value: Boolean) = {
    if (value != isOutputEnabled) {
      _isOutputEnabled = value
      if (!isOutputEnabled) {
        for (i <- 0 until _output.length) {
          _output(i) = 0
        }
        for (i <- 0 until _bundledOutput.length) {
          for (j <- 0 until _bundledOutput(i).length) {
            _bundledOutput(i)(j) = 0
          }
        }
      }
      onRedstoneOutputChanged(ForgeDirection.UNKNOWN)
    }
    this
  }

  def input(side: ForgeDirection) = _input(side.ordinal())

  def output(side: ForgeDirection) = _output(toLocal(side).ordinal())

  def output(side: ForgeDirection, value: Int): Unit = if (value != output(side)) {
    _output(toLocal(side).ordinal()) = value
    onRedstoneOutputChanged(side)
  }

  def bundledInput(side: ForgeDirection, color: Int) =
    _bundledInput(side.ordinal())(color) max _rednetInput(side.ordinal())(color)

  def rednetInput(side: ForgeDirection, color: Int, value: Int) =
    if (_rednetInput(side.ordinal())(color) != value) {
      onRedstoneInputChanged(side)
      _rednetInput(side.ordinal())(color) = value
    }

  def bundledOutput(side: ForgeDirection) = _bundledOutput(toLocal(side).ordinal())

  def bundledOutput(side: ForgeDirection, color: Int): Int = bundledOutput(side)(color)

  def bundledOutput(side: ForgeDirection, color: Int, value: Int): Unit = if (value != bundledOutput(side, color)) {
    _bundledOutput(toLocal(side).ordinal())(color) = value
    onRedstoneOutputChanged(side)
  }

  def checkRedstoneInputChanged() {
    shouldUpdateInput = true
  }

  // ----------------------------------------------------------------------- //

  def updateRedstoneInput() {
    if (shouldUpdateInput) {
      shouldUpdateInput = false
      for (side <- ForgeDirection.VALID_DIRECTIONS) {
        val (oldInput, oldBundledInput) = (_input(side.ordinal()), _bundledInput(side.ordinal()))
        val (newInput, newBundledInput) = (computeInput(side), computeBundledInput(side))
        _input(side.ordinal()) = newInput
        var changed = oldInput >= 0 && input(side) != oldInput
        if (newBundledInput != null) for (color <- 0 until 16) {
          changed = changed || oldBundledInput(color) != newBundledInput(color)
          oldBundledInput(color) = newBundledInput(color)
        }
        else for (color <- 0 until 16) {
          changed = changed || oldBundledInput(color) != 0
          oldBundledInput(color) = 0
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

    nbt.getIntArray(Config.namespace + "rs.input").copyToArray(_input)
    nbt.getIntArray(Config.namespace + "rs.output").copyToArray(_output)

    nbt.getTagList(Config.namespace + "rs.bundledInput").iterator[NBTTagIntArray].zipWithIndex.foreach {
      case (input, side) => input.intArray.copyToArray(_bundledInput(side))
    }
    nbt.getTagList(Config.namespace + "rs.bundledOutput").iterator[NBTTagIntArray].zipWithIndex.foreach {
      case (input, side) => input.intArray.copyToArray(_bundledOutput(side))
    }

    nbt.getTagList(Config.namespace + "rs.rednetInput").iterator[NBTTagIntArray].zipWithIndex.foreach {
      case (input, side) => input.intArray.copyToArray(_rednetInput(side))
    }
  }

  override def save(nbt: NBTTagCompound) = {
    super.save(nbt)

    nbt.setIntArray(Config.namespace + "rs.input", _input)
    nbt.setIntArray(Config.namespace + "rs.output", _output)

    nbt.setNewTagList(Config.namespace + "rs.bundledInput", _bundledInput.view)
    nbt.setNewTagList(Config.namespace + "rs.bundledOutput", _bundledOutput.view)

    nbt.setNewTagList(Config.namespace + "rs.rednetInput", _rednetInput.view)
  }

  // ----------------------------------------------------------------------- //

  protected def computeInput(side: ForgeDirection) = {
    val vanilla = world.getIndirectPowerLevelTo(
      x + side.offsetX,
      y + side.offsetY,
      z + side.offsetZ,
      side.ordinal())
    val redLogic = if (Loader.isModLoaded("RedLogic")) {
      world.getBlockTileEntity(
        x + side.offsetX,
        y + side.offsetY,
        z + side.offsetZ) match {
        case emitter: IRedstoneEmitter =>
          var strength = 0
          for (i <- -1 to 5) {
            strength = strength max emitter.getEmittedSignalStrength(i, side.getOpposite.ordinal())
          }
          strength
        case _ => 0
      }
    }
    else 0
    vanilla max redLogic
  }

  protected def computeBundledInput(side: ForgeDirection): Array[Int] = {
    if (Loader.isModLoaded("RedLogic")) {
      world.getBlockTileEntity(
        x + side.offsetX,
        y + side.offsetY,
        z + side.offsetZ) match {
        case wire: IInsulatedRedstoneWire =>
          var strength: Array[Int] = null
          for (face <- -1 to 5 if wire.wireConnectsInDirection(face, side.ordinal()) && strength == null) {
            strength = Array.fill(16)(0)
            strength(wire.getInsulatedWireColour) = wire.getEmittedSignalStrength(face, side.ordinal())
          }
          strength
        case emitter: IBundledEmitter =>
          var strength: Array[Int] = null
          for (i <- -1 to 5 if strength == null) {
            strength = Option(emitter.getBundledCableStrength(i, side.getOpposite.ordinal())).fold(null: Array[Int])(_.map(_ & 0xFF))
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
      if (Loader.isModLoaded("MineFactoryReloaded")) {
        for (side <- ForgeDirection.VALID_DIRECTIONS) {
          val (nx, ny, nz) = (x + side.offsetX, y + side.offsetY, z + side.offsetZ)
          Block.blocksList(world.getBlockId(nx, ny, nz)) match {
            case block: IRedNetNetworkContainer => block.updateNetwork(world, x, y, z)
            case _ =>
          }
        }
      }
    }
    else {
      val (nx, ny, nz) = (x + side.offsetX, y + side.offsetY, z + side.offsetZ)
      world.notifyBlockOfNeighborChange(nx, ny, nz, block.blockID)
      world.notifyBlocksOfNeighborChange(nx, ny, nz, world.getBlockId(nx, ny, nz))
      if (Loader.isModLoaded("MineFactoryReloaded")) {
        Block.blocksList(world.getBlockId(nx, ny, nz)) match {
          case block: IRedNetNetworkContainer => block.updateNetwork(world, x, y, z)
          case _ =>
        }
      }
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
  def getBundledCableStrength(blockFace: Int, toDirection: Int): Array[Byte] = _bundledOutput(toLocal(ForgeDirection.getOrientation(toDirection)).ordinal()).map(value => (value max 0 min 255).toByte)

  @Optional.Method(modid = "RedLogic")
  def onBundledInputChanged() = checkRedstoneInputChanged()

  @Optional.Method(modid = "RedLogic")
  def getEmittedSignalStrength(blockFace: Int, toDirection: Int): Short = _output(toLocal(ForgeDirection.getOrientation(toDirection)).ordinal()).toShort

  @Optional.Method(modid = "RedLogic")
  def onRedstoneInputChanged() = checkRedstoneInputChanged()
}
