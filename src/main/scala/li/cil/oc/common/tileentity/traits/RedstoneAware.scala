package li.cil.oc.common.tileentity.traits

import li.cil.oc.Settings
import li.cil.oc.integration.Mods
import li.cil.oc.server.{PacketSender => ServerPacketSender}
import li.cil.oc.util.BlockPosition
import li.cil.oc.util.ExtendedWorld._
import net.minecraft.block.BlockRedstoneWire
import net.minecraftforge.fml.common.Optional
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly

/* TODO RedLogic
import mods.immibis.redlogic.api.wiring.IConnectable
import mods.immibis.redlogic.api.wiring.IRedstoneEmitter
import mods.immibis.redlogic.api.wiring.IRedstoneUpdatable
import mods.immibis.redlogic.api.wiring.IWire
*/

import net.minecraft.init.Blocks
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.EnumFacing

@Optional.InterfaceList(Array(
  new Optional.Interface(iface = "mods.immibis.redlogic.api.wiring.IConnectable", modid = Mods.IDs.RedLogic),
  new Optional.Interface(iface = "mods.immibis.redlogic.api.wiring.IRedstoneEmitter", modid = Mods.IDs.RedLogic),
  new Optional.Interface(iface = "mods.immibis.redlogic.api.wiring.IRedstoneUpdatable", modid = Mods.IDs.RedLogic)
))
trait RedstoneAware extends RotationAware /* with IConnectable with IRedstoneEmitter with IRedstoneUpdatable TODO RedLogic */ {
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

  def input(side: EnumFacing) = _input(side.ordinal())

  def maxInput = EnumFacing.values.map(input).max

  def output(side: EnumFacing) = _output(toLocal(side).ordinal())

  def output(side: EnumFacing, value: Int): Unit = if (value != output(side)) {
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
        for (side <- EnumFacing.values) {
          updateRedstoneInput(side)
        }
      }
    }
  }

  protected def updateRedstoneInput(side: EnumFacing) {
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

  protected def computeInput(side: EnumFacing) = {
    val blockPos = BlockPosition(x, y, z).offset(side)
    if (!world.blockExists(blockPos)) 0
    else {
      // See BlockRedstoneLogic.getInputStrength() for reference.
      val vanilla = math.max(world.getIndirectPowerLevelTo(blockPos, side),
        if (world.getBlock(blockPos) == Blocks.redstone_wire) world.getBlockMetadata(blockPos).getValue(BlockRedstoneWire.POWER).asInstanceOf[Integer].intValue() else 0)
      val redLogic = 0
      /* TODO RedLogic
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
      */
      math.max(vanilla, redLogic)
    }
  }

  protected def onRedstoneInputChanged(side: EnumFacing) {}

  protected def onRedstoneOutputEnabledChanged() {
    world.notifyNeighborsOfStateChange(getPos, getBlockType)
    if (isServer) ServerPacketSender.sendRedstoneState(this)
    else world.markBlockForUpdate(getPos)
  }

  protected def onRedstoneOutputChanged(side: EnumFacing) {
    val blockPos = getPos.offset(side)
    world.notifyNeighborsOfStateChange(blockPos, getBlockType)
    world.notifyNeighborsOfStateExcept(blockPos, world.getBlockState(blockPos).getBlock, side.getOpposite)

    if (isServer) ServerPacketSender.sendRedstoneState(this)
    else world.markBlockForUpdate(getPos)
  }

  // ----------------------------------------------------------------------- //
  /* TODO RedLogic
    @Optional.Method(modid = Mods.IDs.RedLogic)
    override def connects(wire: IWire, blockFace: Int, fromDirection: Int) = isOutputEnabled

    @Optional.Method(modid = Mods.IDs.RedLogic)
    override def connectsAroundCorner(wire: IWire, blockFace: Int, fromDirection: Int) = false

    @Optional.Method(modid = Mods.IDs.RedLogic)
    override def getEmittedSignalStrength(blockFace: Int, toDirection: Int): Short = _output(toLocal(ForgeDirection.getOrientation(toDirection)).ordinal()).toShort

    @Optional.Method(modid = Mods.IDs.RedLogic)
    override def onRedstoneInputChanged() = checkRedstoneInputChanged()
  */
}
