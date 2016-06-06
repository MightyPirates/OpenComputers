package li.cil.oc.common.tileentity

import java.util

import com.google.common.base.Strings
import li.cil.oc.Constants
import li.cil.oc.api
import li.cil.oc.common.item.data.PrintData
import li.cil.oc.util.ExtendedAABB
import li.cil.oc.util.ExtendedAABB._
import li.cil.oc.util.ExtendedNBT._
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util._
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly

class Print(val canToggle: Option[() => Boolean], val scheduleUpdate: Option[Int => Unit], val onStateChange: Option[() => Unit]) extends traits.TileEntity with traits.RedstoneAware with traits.RotatableTile {
  def this() = this(None, None, None)
  def this(canToggle: () => Boolean, scheduleUpdate: Int => Unit, onStateChange: () => Unit) = this(Option(canToggle), Option(scheduleUpdate), Option(onStateChange))

  _isOutputEnabled = true

  val data = new PrintData()

  var boundsOff = ExtendedAABB.unitBounds
  var boundsOn = ExtendedAABB.unitBounds
  var state = false

  def bounds = if (state) boundsOn else boundsOff
  def noclip = if (state) data.noclipOn else data.noclipOff
  def shapes = if (state) data.stateOn else data.stateOff

  def isSideSolid(side: EnumFacing): Boolean = {
    for (shape <- shapes if !Strings.isNullOrEmpty(shape.texture)) {
      val bounds = shape.bounds.rotateTowards(facing)
      val fullX = bounds.minX == 0 && bounds.maxX == 1
      val fullY = bounds.minY == 0 && bounds.maxY == 1
      val fullZ = bounds.minZ == 0 && bounds.maxZ == 1
      if (side match {
        case EnumFacing.DOWN => bounds.minY == 0 && fullX && fullZ
        case EnumFacing.UP => bounds.maxY == 1 && fullX && fullZ
        case EnumFacing.NORTH => bounds.minZ == 0 && fullX && fullY
        case EnumFacing.SOUTH => bounds.maxZ == 1 && fullX && fullY
        case EnumFacing.WEST => bounds.minX == 0 && fullY && fullZ
        case EnumFacing.EAST => bounds.maxX == 1 && fullY && fullZ
        case _ => false
      }) return true
    }
    false
  }

  def addCollisionBoxesToList(mask: AxisAlignedBB, list: util.List[AxisAlignedBB], pos: BlockPos = BlockPos.ORIGIN): Unit = {
    if (!noclip) {
      if (shapes.isEmpty) {
        val unitBounds = AxisAlignedBB.fromBounds(0, 0, 0, 1, 1, 1).offset(pos)
        if (mask == null || unitBounds.intersectsWith(mask)) {
          list.add(unitBounds)
        }
      } else {
        for (shape <- shapes) {
          val bounds = shape.bounds.rotateTowards(facing).offset(pos)
          if (mask == null || bounds.intersectsWith(mask)) {
            list.add(bounds)
          }
        }
      }
    }
  }

  def rayTrace(start: Vec3, end: Vec3, pos: BlockPos = BlockPos.ORIGIN): MovingObjectPosition = {
    var closestDistance = Double.PositiveInfinity
    var closest: Option[MovingObjectPosition] = None
    if (shapes.isEmpty) {
      val bounds = AxisAlignedBB.fromBounds(0, 0, 0, 1, 1, 1).offset(pos)
      val hit = bounds.calculateIntercept(start, end)
      if (hit != null) {
        val distance = hit.hitVec.distanceTo(start)
        if (distance < closestDistance) {
          closestDistance = distance
          closest = Option(hit)
        }
      }
    } else {
      for (shape <- shapes) {
        val bounds = shape.bounds.rotateTowards(facing).offset(pos)
        val hit = bounds.calculateIntercept(start, end)
        if (hit != null) {
          val distance = hit.hitVec.distanceTo(start)
          if (distance < closestDistance) {
            closestDistance = distance
            closest = Option(hit)
          }
        }
      }
    }
    closest.map(hit => new MovingObjectPosition(hit.hitVec, hit.sideHit, pos)).orNull
  }

  def activate(): Boolean = {
    if (data.hasActiveState) {
      if (!state || !data.isButtonMode) {
        toggleState()
        return true
      }
    }
    false
  }

  def toggleState(): Unit = {
    if (canToggle.fold(true)(_.apply())) {
      state = !state
      world.playSoundEffect(x + 0.5, y + 0.5, z + 0.5, "random.click", 0.3F, if (state) 0.6F else 0.5F)
      world.markBlockForUpdate(getPos)
      updateRedstone()
      if (state && data.isButtonMode) {
        val block = api.Items.get(Constants.BlockName.Print).block()
        val delay = block.tickRate(world)
        scheduleUpdate match {
          case Some(callback) => callback(delay)
          case _ => world.scheduleUpdate(getPos, block, delay)
        }
      }
      onStateChange.foreach(_.apply())
    }
  }

  def updateBounds(): Unit = {
    boundsOff = data.stateOff.drop(1).foldLeft(data.stateOff.headOption.fold(ExtendedAABB.unitBounds)(_.bounds))((a, b) => a.union(b.bounds))
    if (boundsOff.volume == 0) boundsOff = ExtendedAABB.unitBounds
    else boundsOff = boundsOff.rotateTowards(facing)
    boundsOn = data.stateOn.drop(1).foldLeft(data.stateOn.headOption.fold(ExtendedAABB.unitBounds)(_.bounds))((a, b) => a.union(b.bounds))
    if (boundsOn.volume == 0) boundsOn = ExtendedAABB.unitBounds
    else boundsOn = boundsOn.rotateTowards(facing)
  }

  def updateRedstone(): Unit = {
    if (data.emitRedstone) {
      EnumFacing.values().foreach(output(_, if (data.emitRedstone(state)) data.redstoneLevel else 0))
    }
  }

  override protected def onRedstoneInputChanged(side: EnumFacing, oldMaxValue: Int, newMaxValue: Int): Unit = {
    super.onRedstoneInputChanged(side, oldMaxValue, newMaxValue)
    val newState = newMaxValue > 0
    if (!data.emitRedstone && data.hasActiveState && state != newState) {
      toggleState()
    }
  }

  override def readFromNBTForServer(nbt: NBTTagCompound): Unit = {
    super.readFromNBTForServer(nbt)
    data.load(nbt.getCompoundTag("data"))
    state = nbt.getBoolean("state")
    updateBounds()
  }

  override def writeToNBTForServer(nbt: NBTTagCompound): Unit = {
    super.writeToNBTForServer(nbt)
    nbt.setNewCompoundTag("data", data.save)
    nbt.setBoolean("state", state)
  }

  @SideOnly(Side.CLIENT)
  override def readFromNBTForClient(nbt: NBTTagCompound): Unit = {
    super.readFromNBTForClient(nbt)
    data.load(nbt.getCompoundTag("data"))
    state = nbt.getBoolean("state")
    updateBounds()
    if (world != null) {
      world.markBlockForUpdate(getPos)
      if (data.emitLight) world.checkLight(getPos)
    }
  }

  override def writeToNBTForClient(nbt: NBTTagCompound): Unit = {
    super.writeToNBTForClient(nbt)
    nbt.setNewCompoundTag("data", data.save)
    nbt.setBoolean("state", state)
  }

  override protected def onRotationChanged(): Unit = {
    super.onRotationChanged()
    updateBounds()
  }
}
