package li.cil.oc.common.tileentity.traits

import li.cil.oc.Settings
import li.cil.oc.integration.Mods
import li.cil.oc.integration.util.BundledRedstone
import li.cil.oc.util.ExtendedNBT._
import mrtjp.projectred.api.IBundledTile
import net.minecraftforge.fml.common.Optional

/* TODO RedLogic
import mods.immibis.redlogic.api.wiring.IBundledEmitter
import mods.immibis.redlogic.api.wiring.IBundledUpdatable
*/

import net.minecraft.nbt.NBTTagCompound
import net.minecraft.nbt.NBTTagIntArray
import net.minecraft.util.EnumFacing
import net.minecraftforge.common.util.Constants.NBT

/* TODO MFR
import powercrystals.minefactoryreloaded.api.rednet.IRedNetNetworkContainer
*/

@Optional.InterfaceList(Array(
  new Optional.Interface(iface = "mods.immibis.redlogic.api.wiring.IBundledEmitter", modid = Mods.IDs.RedLogic),
  new Optional.Interface(iface = "mods.immibis.redlogic.api.wiring.IBundledUpdatable", modid = Mods.IDs.RedLogic),
  new Optional.Interface(iface = "mrtjp.projectred.api.IBundledTile", modid = Mods.IDs.ProjectRedTransmission)
))
trait BundledRedstoneAware extends RedstoneAware with IBundledTile /* with IBundledEmitter with IBundledUpdatable TODO RedLogic, MFR */ {

  protected[tileentity] val _bundledInput = Array.fill(6)(Array.fill(16)(-1))

  protected[tileentity] val _rednetInput = Array.fill(6)(Array.fill(16)(-1))

  protected[tileentity] val _bundledOutput = Array.fill(6)(Array.fill(16)(0))

  // ----------------------------------------------------------------------- //

  override def isOutputEnabled_=(value: Boolean) = {
    if (value != isOutputEnabled) {
      if (!value) {
        for (i <- _bundledOutput.indices) {
          for (j <- _bundledOutput(i).indices) {
            _bundledOutput(i)(j) = 0
          }
        }
      }
    }
    super.isOutputEnabled_=(value)
  }

  def bundledInput(side: EnumFacing) =
    (_bundledInput(side.ordinal()), _rednetInput(side.ordinal())).zipped.map(math.max)

  def bundledInput(side: EnumFacing, newBundledInput: Array[Int]): Unit = {
    for (color <- 0 until 16) {
      updateInput(_bundledInput, side, color, if (newBundledInput == null) 0 else newBundledInput(color))
    }
  }

  def rednetInput(side: EnumFacing, color: Int, value: Int): Unit = updateInput(_rednetInput, side, color, value)

  def updateInput(inputs: Array[Array[Int]], side: EnumFacing, color: Int, newValue: Int): Unit = {
    val oldValue = inputs(side.ordinal())(color)
    if (oldValue != newValue) {
      if (oldValue != -1) {
        onRedstoneInputChanged(RedstoneChangedEventArgs(side, oldValue, newValue, color))
      }
      inputs(side.ordinal())(color) = newValue
    }
  }

  def bundledInput(side: EnumFacing, color: Int) =
    math.max(_bundledInput(side.ordinal())(color), _rednetInput(side.ordinal())(color))

  def bundledOutput(side: EnumFacing) = _bundledOutput(toLocal(side).ordinal())

  def bundledOutput(side: EnumFacing, color: Int): Int = bundledOutput(side)(color)

  def bundledOutput(side: EnumFacing, color: Int, value: Int): Unit = if (value != bundledOutput(side, color)) {
    _bundledOutput(toLocal(side).ordinal())(color) = value

    /* TODO MFR
    if (Mods.MineFactoryReloaded.isAvailable) {
      val blockPos = BlockPosition(x, y, z).offset(side)
      world.getBlock(blockPos) match {
        case block: IRedNetNetworkContainer => block.updateNetwork(world, blockPos.x, blockPos.y, blockPos.z, side.getOpposite)
        case _ =>
      }
    }
    */

    onRedstoneOutputChanged(side)
  }

  // ----------------------------------------------------------------------- //

  override def updateRedstoneInput(side: EnumFacing) {
    super.updateRedstoneInput(side)
    bundledInput(side, BundledRedstone.computeBundledInput(position, side))
  }

  // ----------------------------------------------------------------------- //

  private final val BundledInputTag = Settings.namespace + "rs.bundledInput"
  private final val BundledOutputTag = Settings.namespace + "rs.bundledOutput"
  private final val RednetInputTag = Settings.namespace + "rs.rednetInput"

  override def readFromNBTForServer(nbt: NBTTagCompound) {
    super.readFromNBTForServer(nbt)

    nbt.getTagList(BundledInputTag, NBT.TAG_INT_ARRAY).toArray[NBTTagIntArray].
      map(_.getIntArray).zipWithIndex.foreach {
      case (input, index) if index < _bundledInput.length =>
        val safeLength = input.length min _bundledInput(index).length
        input.copyToArray(_bundledInput(index), 0, safeLength)
      case _ =>
    }
    nbt.getTagList(BundledOutputTag, NBT.TAG_INT_ARRAY).toArray[NBTTagIntArray].
      map(_.getIntArray).zipWithIndex.foreach {
      case (input, index) if index < _bundledOutput.length =>
        val safeLength = input.length min _bundledOutput(index).length
        input.copyToArray(_bundledOutput(index), 0, safeLength)
      case _ =>
    }

    nbt.getTagList(RednetInputTag, NBT.TAG_INT_ARRAY).toArray[NBTTagIntArray].
      map(_.getIntArray).zipWithIndex.foreach {
      case (input, index) if index < _rednetInput.length =>
        val safeLength = input.length min _rednetInput(index).length
        input.copyToArray(_rednetInput(index), 0, safeLength)
      case _ =>
    }
  }

  override def writeToNBTForServer(nbt: NBTTagCompound) {
    super.writeToNBTForServer(nbt)

    nbt.setNewTagList(BundledInputTag, _bundledInput.view)
    nbt.setNewTagList(BundledOutputTag, _bundledOutput.view)

    nbt.setNewTagList(RednetInputTag, _rednetInput.view)
  }

  // ----------------------------------------------------------------------- //

  override protected def onRedstoneOutputEnabledChanged() {
    /* TODO MFR
    if (Mods.MineFactoryReloaded.isAvailable) {
      for (side <- EnumFacing.VALID_DIRECTIONS) {
        val blockPos = BlockPosition(x, y, z).offset(side)
        world.getBlock(blockPos) match {
          case block: IRedNetNetworkContainer => block.updateNetwork(world, x, y, z, side.getOpposite)
          case _ =>
        }
      }
    }
    */
    super.onRedstoneOutputEnabledChanged()
  }

  // ----------------------------------------------------------------------- //
  /* TODO RedLogic
    @Optional.Method(modid = Mods.IDs.RedLogic)
    def getBundledCableStrength(blockFace: Int, toDirection: Int): Array[Byte] = bundledOutput(EnumFacing.getOrientation(toDirection)).map(value => math.min(math.max(value, 0), 255).toByte)

    @Optional.Method(modid = Mods.IDs.RedLogic)
    def onBundledInputChanged() = checkRedstoneInputChanged()
  */
  // ----------------------------------------------------------------------- //

  @Optional.Method(modid = Mods.IDs.ProjectRedTransmission)
  override def canConnectBundled(side: Int): Boolean = isOutputEnabled

  @Optional.Method(modid = Mods.IDs.ProjectRedTransmission)
  override def getBundledSignal(side: Int): Array[Byte] = bundledOutput(EnumFacing.getFront(side)).map(value => math.min(math.max(value, 0), 255).toByte)

}
