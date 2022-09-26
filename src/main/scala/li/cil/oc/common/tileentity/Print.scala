package li.cil.oc.common.tileentity

import java.util

import com.google.common.base.Strings
import li.cil.oc.Constants
import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.common.block.{Print => PrintBlock}
import li.cil.oc.common.item.data.PrintData
import li.cil.oc.common.tileentity.traits.RedstoneChangedEventArgs
import li.cil.oc.util.ExtendedAABB
import li.cil.oc.util.ExtendedAABB._
import li.cil.oc.util.ExtendedNBT._
import net.minecraft.util.SoundEvents
import net.minecraft.nbt.CompoundNBT
import net.minecraft.tileentity.TileEntity
import net.minecraft.tileentity.TileEntityType
import net.minecraft.util.Direction
import net.minecraft.util.SoundCategory
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.RayTraceResult
import net.minecraft.util.math.shapes.IBooleanFunction
import net.minecraft.util.math.shapes.VoxelShape
import net.minecraft.util.math.shapes.VoxelShapes
import net.minecraft.util.math.vector.Vector3d
import net.minecraft.world.server.ServerWorld
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import net.minecraftforge.client.model.data.IModelData
import net.minecraftforge.client.model.data.ModelProperty
import scala.collection.Iterable
import scala.collection.convert.ImplicitConversionsToJava._

class Print(selfType: TileEntityType[_ <: Print], val canToggle: Option[() => Boolean], val scheduleUpdate: Option[Int => Unit], val onStateChange: Option[() => Unit])
  extends TileEntity(selfType) with traits.TileEntity with traits.RedstoneAware with traits.RotatableTile with IModelData {

  def this(selfType: TileEntityType[_ <: Print]) = this(selfType, None, None, None)
  def this(selfType: TileEntityType[_ <: Print], canToggle: () => Boolean, scheduleUpdate: Int => Unit, onStateChange: () => Unit) =
    this(selfType, Option(canToggle), Option(scheduleUpdate), Option(onStateChange))

  _isOutputEnabled = true

  val data = new PrintData()

  var shapeOff = VoxelShapes.block
  var shapeOn = VoxelShapes.block
  var state = false

  def shape = if (state) shapeOn else shapeOff
  def noclip = if (state) data.noclipOn else data.noclipOff
  def shapes = if (state) data.stateOn else data.stateOff

  def activate(): Boolean = {
    if (data.hasActiveState) {
      if (!state || !data.isButtonMode) {
        toggleState()
        return true
      }
    }
    false
  }

  private def buildValueSet(value: Int): util.Map[AnyRef, AnyRef] = {
    val map: util.Map[AnyRef, AnyRef] = new util.HashMap[AnyRef, AnyRef]()
    Direction.values.foreach {
      side => map.put(new java.lang.Integer(side.ordinal), new java.lang.Integer(value))
    }
    map
  }

  def toggleState(): Unit = {
    if (canToggle.fold(true)(_.apply())) {
      state = !state
      getLevel.playSound(null, getBlockPos, SoundEvents.LEVER_CLICK, SoundCategory.BLOCKS, 0.3F, if (state) 0.6F else 0.5F)
      getLevel.sendBlockUpdated(getBlockPos, getLevel.getBlockState(getBlockPos), getLevel.getBlockState(getBlockPos), 3)
      updateRedstone()
      if (state && data.isButtonMode) {
        val block = api.Items.get(Constants.BlockName.Print).block().asInstanceOf[PrintBlock]
        val delay = block.tickRate(getLevel)
        scheduleUpdate match {
          case Some(callback) => callback(delay)
          case _ if !getLevel.isClientSide => getLevel.asInstanceOf[ServerWorld].getBlockTicks.scheduleTick(getBlockPos, block, delay)
          case _ =>
        }
      }
      onStateChange.foreach(_.apply())
    }
  }

  private def convertShape(state: Iterable[PrintData.Shape]): VoxelShape = if (!state.isEmpty) {
    state.foldLeft(VoxelShapes.empty)((curr, s) => {
      val voxel = VoxelShapes.create(s.bounds.rotateTowards(facing))
      VoxelShapes.joinUnoptimized(curr, voxel, IBooleanFunction.OR)
    }).optimize()
  }
  else VoxelShapes.block

  def updateShape(): Unit = {
    shapeOff = convertShape(data.stateOff)
    shapeOn = convertShape(data.stateOn)
  }

  def updateRedstone(): Unit = {
    if (data.emitRedstone) {
      setOutput(buildValueSet(if (data.emitRedstone(state)) data.redstoneLevel else 0))
    }
  }

  override protected def onRedstoneInputChanged(args: RedstoneChangedEventArgs): Unit = {
    val newState = args.newValue > 0
    if (!data.emitRedstone && data.hasActiveState && state != newState) {
      toggleState()
    }
  }

  override protected def onRotationChanged(): Unit = {
    super.onRotationChanged()
    updateShape()
  }

  // ----------------------------------------------------------------------- //

  private final val DataTag = Settings.namespace + "data"
  @Deprecated
  private final val DataTagCompat = "data"
  private final val StateTag = Settings.namespace + "state"
  @Deprecated
  private final val StateTagCompat = "state"

  override def loadForServer(nbt: CompoundNBT): Unit = {
    super.loadForServer(nbt)
    if (nbt.contains(DataTagCompat))
      data.loadData(nbt.getCompound(DataTagCompat))
    else
      data.loadData(nbt.getCompound(DataTag))
    if (nbt.contains(StateTagCompat))
      state = nbt.getBoolean(StateTagCompat)
    else
      state = nbt.getBoolean(StateTag)
    updateShape()
  }

  override def saveForServer(nbt: CompoundNBT): Unit = {
    super.saveForServer(nbt)
    nbt.setNewCompoundTag(DataTag, data.saveData)
    nbt.putBoolean(StateTag, state)
  }

  @OnlyIn(Dist.CLIENT)
  override def loadForClient(nbt: CompoundNBT): Unit = {
    super.loadForClient(nbt)
    data.loadData(nbt.getCompound(DataTag))
    state = nbt.getBoolean(StateTag)
    updateShape()
    if (getLevel != null) {
      getLevel.sendBlockUpdated(getBlockPos, getLevel.getBlockState(getBlockPos), getLevel.getBlockState(getBlockPos), 3)
      if (data.emitLight) getLevel.getLightEngine.checkBlock(getBlockPos)
    }
  }

  override def saveForClient(nbt: CompoundNBT): Unit = {
    super.saveForClient(nbt)
    nbt.setNewCompoundTag(DataTag, data.saveData)
    nbt.putBoolean(StateTag, state)
  }

  // ----------------------------------------------------------------------- //

  @Deprecated
  override def getModelData() = this

  @Deprecated
  override def hasProperty(prop: ModelProperty[_]) = false

  @Deprecated
  override def getData[T](prop: ModelProperty[T]): T = null.asInstanceOf[T]

  @Deprecated
  override def setData[T](prop: ModelProperty[T], value: T): T = null.asInstanceOf[T]
}
