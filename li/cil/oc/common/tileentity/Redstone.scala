package li.cil.oc.common.tileentity

import cpw.mods.fml.common.Optional.Interface
import cpw.mods.fml.common.{Loader, Optional}
import li.cil.oc.Settings
import li.cil.oc.api.network
import li.cil.oc.server.{PacketSender => ServerPacketSender}
import li.cil.oc.util.Persistable
import mods.immibis.redlogic.api.wiring._
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.common.ForgeDirection

@Optional.InterfaceList(Array(
  new Interface(iface = "mods.immibis.redlogic.api.wiring.IConnectable", modid = "RedLogic"),
  new Interface(iface = "mods.immibis.redlogic.api.wiring.IBundledEmitter", modid = "RedLogic"),
  new Interface(iface = "mods.immibis.redlogic.api.wiring.IBundledUpdatable", modid = "RedLogic"),
  new Interface(iface = "mods.immibis.redlogic.api.wiring.IRedstoneEmitter", modid = "RedLogic"),
  new Interface(iface = "mods.immibis.redlogic.api.wiring.IRedstoneUpdatable", modid = "RedLogic")
))
trait Redstone extends TileEntity with network.Environment with Rotatable with Persistable with IConnectable with IRedstoneEmitter with IRedstoneUpdatable {
  protected val _input = Array.fill(6)(-1)

  protected val _output = Array.fill(6)(0)

  protected var _isOutputEnabled = false

  protected var shouldUpdateInput = true

  def isOutputEnabled = _isOutputEnabled

  def isOutputEnabled_=(value: Boolean) = {
    if (value != isOutputEnabled) {
      _isOutputEnabled = value
      if (!isOutputEnabled) {
        for (i <- 0 until _output.length) {
          _output(i) = 0
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

  def checkRedstoneInputChanged() {
    shouldUpdateInput = true
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

  override def load(nbt: NBTTagCompound) = {
    super.load(nbt)

    nbt.getIntArray(Settings.namespace + "rs.input").copyToArray(_input)
    nbt.getIntArray(Settings.namespace + "rs.output").copyToArray(_output)
  }

  override def save(nbt: NBTTagCompound) = {
    super.save(nbt)

    nbt.setIntArray(Settings.namespace + "rs.input", _input)
    nbt.setIntArray(Settings.namespace + "rs.output", _output)
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
  def getEmittedSignalStrength(blockFace: Int, toDirection: Int): Short = _output(toLocal(ForgeDirection.getOrientation(toDirection)).ordinal()).toShort

  @Optional.Method(modid = "RedLogic")
  def onRedstoneInputChanged() = checkRedstoneInputChanged()
}
