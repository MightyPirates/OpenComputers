package li.cil.oc.common.tileentity.traits

import cpw.mods.fml.common.Optional
import li.cil.oc.Settings
import li.cil.oc.util.ExtendedNBT._
import li.cil.oc.util.mods.{Mods, ProjectRed}
import mods.immibis.redlogic.api.wiring.{IBundledEmitter, IBundledUpdatable, IInsulatedRedstoneWire}
import mrtjp.projectred.api.{IBundledTile, ProjectRedAPI}
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.common.util.Constants.NBT
import net.minecraftforge.common.util.ForgeDirection
import powercrystals.minefactoryreloaded.api.rednet.IRedNetNetworkContainer

@Optional.InterfaceList(Array(
  new Optional.Interface(iface = "mods.immibis.redlogic.api.wiring.IBundledEmitter", modid = Mods.IDs.RedLogic),
  new Optional.Interface(iface = "mods.immibis.redlogic.api.wiring.IBundledUpdatable", modid = Mods.IDs.RedLogic),
  new Optional.Interface(iface = "mrtjp.projectred.api.IBundledTile", modid = Mods.IDs.ProjectRedTransmission)
))
trait BundledRedstoneAware extends RedstoneAware with IBundledEmitter with IBundledUpdatable with IBundledTile {

  protected[tileentity] val _bundledInput = Array.fill(6)(Array.fill(16)(-1))

  protected[tileentity] val _rednetInput = Array.fill(6)(Array.fill(16)(-1))

  protected[tileentity] val _bundledOutput = Array.fill(6)(Array.fill(16)(0))

  // ----------------------------------------------------------------------- //

  override def isOutputEnabled_=(value: Boolean) = {
    if (value != isOutputEnabled) {
      if (!value) {
        for (i <- 0 until _bundledOutput.length) {
          for (j <- 0 until _bundledOutput(i).length) {
            _bundledOutput(i)(j) = 0
          }
        }
      }
    }
    super.isOutputEnabled_=(value)
  }

  def bundledInput(side: ForgeDirection, color: Int) =
    math.max(_bundledInput(side.ordinal())(color), _rednetInput(side.ordinal())(color))

  def rednetInput(side: ForgeDirection, color: Int, value: Int) =
    if (_rednetInput(side.ordinal())(color) != value) {
      if (_rednetInput(side.ordinal())(color) != -1) {
        onRedstoneInputChanged(side)
      }
      _rednetInput(side.ordinal())(color) = value
    }

  def bundledOutput(side: ForgeDirection) = _bundledOutput(toLocal(side).ordinal())

  def bundledOutput(side: ForgeDirection, color: Int): Int = bundledOutput(side)(color)

  def bundledOutput(side: ForgeDirection, color: Int, value: Int): Unit = if (value != bundledOutput(side, color)) {
    _bundledOutput(toLocal(side).ordinal())(color) = value

    if (Mods.MineFactoryReloaded.isAvailable) {
      val nx = x + side.offsetX
      val ny = y + side.offsetY
      val nz = z + side.offsetZ
      world.getBlock(nx, ny, nz) match {
        case block: IRedNetNetworkContainer => block.updateNetwork(world, nx, ny, nz)
        case _ =>
      }
    }

    onRedstoneOutputChanged(side)
  }

  // ----------------------------------------------------------------------- //

  override protected def updateRedstoneInput(side: ForgeDirection) {
    super.updateRedstoneInput(side)
    val oldBundledInput = _bundledInput(side.ordinal())
    val newBundledInput = computeBundledInput(side)
    var changed = false
    if (newBundledInput != null) for (color <- 0 until 16) {
      changed = changed || (oldBundledInput(color) >= 0 && oldBundledInput(color) != newBundledInput(color))
      oldBundledInput(color) = newBundledInput(color)
    }
    else for (color <- 0 until 16) {
      changed = changed || oldBundledInput(color) > 0
      oldBundledInput(color) = 0
    }
    if (changed) {
      onRedstoneInputChanged(side)
    }
  }

  override def readFromNBT(nbt: NBTTagCompound) {
    super.readFromNBT(nbt)

    nbt.getTagList(Settings.namespace + "rs.bundledInput", NBT.TAG_INT_ARRAY).foreach {
      case (list, index) if index < _bundledInput.length =>
        val input = list.func_150306_c(index)
        val safeLength = input.length min _bundledInput(index).length
        input.copyToArray(_bundledInput(index), 0, safeLength)
    }
    nbt.getTagList(Settings.namespace + "rs.bundledOutput", NBT.TAG_INT_ARRAY).foreach {
      case (list, index) if index < _bundledOutput.length =>
        val input = list.func_150306_c(index)
        val safeLength = input.length min _bundledOutput(index).length
        input.copyToArray(_bundledOutput(index), 0, safeLength)
    }

    nbt.getTagList(Settings.namespace + "rs.rednetInput", NBT.TAG_INT_ARRAY).foreach {
      case (list, index) if index < _rednetInput.length =>
        val input = list.func_150306_c(index)
        val safeLength = input.length min _rednetInput(index).length
        input.copyToArray(_rednetInput(index), 0, safeLength)
    }
  }

  override def writeToNBT(nbt: NBTTagCompound) {
    super.writeToNBT(nbt)

    nbt.setNewTagList(Settings.namespace + "rs.bundledInput", _bundledInput.view)
    nbt.setNewTagList(Settings.namespace + "rs.bundledOutput", _bundledOutput.view)

    nbt.setNewTagList(Settings.namespace + "rs.rednetInput", _rednetInput.view)
  }

  // ----------------------------------------------------------------------- //

  protected def computeBundledInput(side: ForgeDirection): Array[Int] = {
    val redLogic = if (Mods.RedLogic.isAvailable) {
      world.getTileEntity(
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
    val projectRed = if (Mods.ProjectRedTransmission.isAvailable && ProjectRed.isAPIAvailable) {
      Option(ProjectRedAPI.transmissionAPI.getBundledInput(world, x, y, z, side.ordinal)).fold(null: Array[Int])(_.map(_ & 0xFF))
    } else null
    (redLogic, projectRed) match {
      case (a: Array[Int], b: Array[Int]) => (a, b).zipped.map((r1, r2) => math.max(r1, r2))
      case (a: Array[Int], _) => a
      case (_, b: Array[Int]) => b
      case _ => null
    }
  }

  override protected def onRedstoneOutputEnabledChanged() {
    if (Mods.MineFactoryReloaded.isAvailable) {
      for (side <- ForgeDirection.VALID_DIRECTIONS) {
        val nx = x + side.offsetX
        val ny = y + side.offsetY
        val nz = z + side.offsetZ
        world.getBlock(nx, ny, nz) match {
          case block: IRedNetNetworkContainer => block.updateNetwork(world, x, y, z)
          case _ =>
        }
      }
    }
    super.onRedstoneOutputEnabledChanged()
  }

  // ----------------------------------------------------------------------- //

  @Optional.Method(modid = Mods.IDs.RedLogic)
  def getBundledCableStrength(blockFace: Int, toDirection: Int): Array[Byte] = bundledOutput(ForgeDirection.getOrientation(toDirection)).map(value => math.min(math.max(value, 0), 255).toByte)

  @Optional.Method(modid = Mods.IDs.RedLogic)
  def onBundledInputChanged() = checkRedstoneInputChanged()

  // ----------------------------------------------------------------------- //

  @Optional.Method(modid = Mods.IDs.ProjectRedTransmission)
  def canConnectBundled(side: Int) = isOutputEnabled

  @Optional.Method(modid = Mods.IDs.ProjectRedTransmission)
  def getBundledSignal(side: Int) = bundledOutput(ForgeDirection.getOrientation(side)).map(value => math.min(math.max(value, 0), 255).toByte)
}
