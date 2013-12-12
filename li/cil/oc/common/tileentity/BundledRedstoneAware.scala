package li.cil.oc.common.tileentity

import cpw.mods.fml.common.Optional.Interface
import cpw.mods.fml.common.{Optional, Loader}
import li.cil.oc.Settings
import li.cil.oc.util.ExtendedNBT._
import mods.immibis.redlogic.api.wiring.{IInsulatedRedstoneWire, IBundledUpdatable, IBundledEmitter}
import net.minecraft.block.Block
import net.minecraft.nbt.{NBTTagIntArray, NBTTagCompound}
import net.minecraftforge.common.ForgeDirection
import powercrystals.minefactoryreloaded.api.rednet.IRedNetNetworkContainer
import scala.Array

@Optional.InterfaceList(Array(
  new Interface(iface = "mods.immibis.redlogic.api.wiring.IBundledEmitter", modid = "RedLogic"),
  new Interface(iface = "mods.immibis.redlogic.api.wiring.IBundledUpdatable", modid = "RedLogic")
))
trait BundledRedstoneAware extends RedstoneAware with IBundledEmitter with IBundledUpdatable {

  private val _bundledInput = Array.fill(6)(Array.fill(16)(-1))

  private val _rednetInput = Array.fill(6)(Array.fill(16)(-1))

  private val _bundledOutput = Array.fill(6)(Array.fill(16)(0))

  // ----------------------------------------------------------------------- //

  override def isOutputEnabled_=(value: Boolean) = {
    if (value != isOutputEnabled) {
      if (!isOutputEnabled) {
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
    onRedstoneOutputChanged(side)
  }

  // ----------------------------------------------------------------------- //

  override def updateRedstoneInput() {
    if (shouldUpdateInput) {
      for (side <- ForgeDirection.VALID_DIRECTIONS) {
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
    }
    super.updateRedstoneInput()
  }

  override def load(nbt: NBTTagCompound) {
    super.load(nbt)

    nbt.getTagList(Settings.namespace + "rs.bundledInput").iterator[NBTTagIntArray].zipWithIndex.foreach {
      case (input, side) => input.intArray.copyToArray(_bundledInput(side))
    }
    nbt.getTagList(Settings.namespace + "rs.bundledOutput").iterator[NBTTagIntArray].zipWithIndex.foreach {
      case (input, side) => input.intArray.copyToArray(_bundledOutput(side))
    }

    nbt.getTagList(Settings.namespace + "rs.rednetInput").iterator[NBTTagIntArray].zipWithIndex.foreach {
      case (input, side) => input.intArray.copyToArray(_rednetInput(side))
    }
  }

  override def save(nbt: NBTTagCompound) {
    super.save(nbt)

    nbt.setNewTagList(Settings.namespace + "rs.bundledInput", _bundledInput.view)
    nbt.setNewTagList(Settings.namespace + "rs.bundledOutput", _bundledOutput.view)

    nbt.setNewTagList(Settings.namespace + "rs.rednetInput", _rednetInput.view)
  }

  // ----------------------------------------------------------------------- //

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

  override protected def onRedstoneOutputChanged(side: ForgeDirection) {
    if (side == ForgeDirection.UNKNOWN) {
      if (Loader.isModLoaded("MineFactoryReloaded")) {
        for (side <- ForgeDirection.VALID_DIRECTIONS) {
          val nx = x + side.offsetX
          val ny = y + side.offsetY
          val nz = z + side.offsetZ
          Block.blocksList(world.getBlockId(nx, ny, nz)) match {
            case block: IRedNetNetworkContainer => block.updateNetwork(world, x, y, z)
            case _ =>
          }
        }
      }
    }
    else {
      val nx = x + side.offsetX
      val ny = y + side.offsetY
      val nz = z + side.offsetZ
      if (Loader.isModLoaded("MineFactoryReloaded")) {
        Block.blocksList(world.getBlockId(nx, ny, nz)) match {
          case block: IRedNetNetworkContainer => block.updateNetwork(world, x, y, z)
          case _ =>
        }
      }
    }
    super.onRedstoneOutputChanged(side)
  }

  // ----------------------------------------------------------------------- //

  @Optional.Method(modid = "RedLogic")
  def getBundledCableStrength(blockFace: Int, toDirection: Int): Array[Byte] = _bundledOutput(toLocal(ForgeDirection.getOrientation(toDirection)).ordinal()).map(value => math.min(math.max(value, 0), 255).toByte)

  @Optional.Method(modid = "RedLogic")
  def onBundledInputChanged() = checkRedstoneInputChanged()
}
