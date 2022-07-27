package li.cil.oc.common.tileentity

import java.util

import li.cil.oc.{Constants, Settings, api}
import li.cil.oc.api.driver.DeviceInfo
import li.cil.oc.api.driver.DeviceInfo.{DeviceAttribute, DeviceClass}
import li.cil.oc.api.machine.{Arguments, Callback, Context}
import li.cil.oc.api.network.{Node, Visibility}
import li.cil.oc.common.EventHandler
import li.cil.oc.common.tileentity.traits.RedstoneChangedEventArgs
import li.cil.oc.server.{PacketSender => ServerPacketSender}
import li.cil.oc.util.RotationHelper
import net.minecraft.util.SoundEvents
import net.minecraft.nbt.CompoundNBT
import net.minecraft.tileentity.TileEntity
import net.minecraft.tileentity.TileEntityType
import net.minecraft.util.Direction
import net.minecraft.util.SoundCategory
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn

import scala.collection.convert.ImplicitConversionsToJava._
import scala.collection.mutable

class NetSplitter(selfType: TileEntityType[_ <: NetSplitter]) extends TileEntity(selfType) with traits.Environment with traits.OpenSides with traits.RedstoneAware with api.network.SidedEnvironment with DeviceInfo {
  private lazy val deviceInfo: util.Map[String, String] = Map(
    DeviceAttribute.Class -> DeviceClass.Network,
    DeviceAttribute.Description -> "Ethernet controller",
    DeviceAttribute.Vendor -> Constants.DeviceInfo.DefaultVendor,
    DeviceAttribute.Product -> "NetSplits",
    DeviceAttribute.Version -> "1.0",
    DeviceAttribute.Width -> "6"
  )

  override def getDeviceInfo: util.Map[String, String] = deviceInfo

  _isOutputEnabled = true

  val node: Node = api.Network.newNode(this, Visibility.Network).
    withComponent("net_splitter", Visibility.Network).
    create()

  var isInverted = false

  override def isSideOpen(side: Direction): Boolean = if (isInverted) !super.isSideOpen(side) else super.isSideOpen(side)

  override def setSideOpen(side: Direction, value: Boolean) {
    val previous = isSideOpen(side)
    super.setSideOpen(side, value)
    if (previous != isSideOpen(side)) {
      if (isServer) {
        node.remove()
        api.Network.joinOrCreateNetwork(this)
        ServerPacketSender.sendNetSplitterState(this)
        getLevel.playSound(null, getBlockPos, SoundEvents.PISTON_EXTEND, SoundCategory.BLOCKS, 0.5f, getLevel.random.nextFloat() * 0.25f + 0.7f)
        getLevel.updateNeighborsAt(getBlockPos, getBlockState.getBlock)
      }
      else {
        getLevel.sendBlockUpdated(getBlockPos, getLevel.getBlockState(getBlockPos), getLevel.getBlockState(getBlockPos), 3)
      }
    }
  }

  // ----------------------------------------------------------------------- //

  override def sidedNode(side: Direction): Node = if (isSideOpen(side)) node else null

  @OnlyIn(Dist.CLIENT)
  override def canConnect(side: Direction): Boolean = isSideOpen(side)

  // ----------------------------------------------------------------------- //

  override protected def initialize(): Unit = {
    super.initialize()
    EventHandler.scheduleServer(this)
  }

  // ----------------------------------------------------------------------- //

  override protected def onRedstoneInputChanged(args: RedstoneChangedEventArgs): Unit = {
    super.onRedstoneInputChanged(args)
    val oldIsInverted = isInverted
    isInverted = args.newValue > 0
    if (isInverted != oldIsInverted) {
      if (isServer) {
        node.remove()
        api.Network.joinOrCreateNetwork(this)
        ServerPacketSender.sendNetSplitterState(this)
        getLevel.playSound(null, getBlockPos, SoundEvents.PISTON_CONTRACT, SoundCategory.BLOCKS, 0.5f, getLevel.random.nextFloat() * 0.25f + 0.7f)
      }
      else {
        getLevel.sendBlockUpdated(getBlockPos, getLevel.getBlockState(getBlockPos), getLevel.getBlockState(getBlockPos), 3)
      }
    }
  }

  // ----------------------------------------------------------------------- //

  private final val IsInvertedTag = Settings.namespace + "isInverted"
  private final val OpenSidesTag = Settings.namespace + "openSides"

  override def loadForServer(nbt: CompoundNBT): Unit = {
    super.loadForServer(nbt)
    isInverted = nbt.getBoolean(IsInvertedTag)
  }

  override def saveForServer(nbt: CompoundNBT): Unit = {
    super.saveForServer(nbt)
    nbt.putBoolean(IsInvertedTag, isInverted)
  }

  @OnlyIn(Dist.CLIENT) override
  def loadForClient(nbt: CompoundNBT): Unit = {
    super.loadForClient(nbt)
    isInverted = nbt.getBoolean(IsInvertedTag)
  }

  override def saveForClient(nbt: CompoundNBT): Unit = {
    super.saveForClient(nbt)
    nbt.putBoolean(IsInvertedTag, isInverted)
  }

  // component api
  def currentStatus(): mutable.Map[Int, Boolean] = {
    val openStatus = mutable.Map[Int, Boolean]()
    for (side <- Direction.values) {
      openStatus += side.ordinal() -> isSideOpen(side)
    }
    openStatus
  }

  def setSide(side: Direction, state: Boolean): Boolean = {
    val previous = isSideOpen(side) // isSideOpen uses inverter
    setSideOpen(side, if (isInverted) !state else state) // but setSideOpen does not
    previous != state
  }

  @Callback(doc = "function(settings:table):table -- set open state (true/false) of all sides in an array; index by direction. Returns previous states")
  def setSides(context: Context, args: Arguments): Array[AnyRef] = {
    val settings = args.checkTable(0)
    val previous = currentStatus()
    for (side <- Direction.values) {
      val ordinal = side.ordinal()
      val value = if (settings.containsKey(ordinal)) {
        settings.get(ordinal) match {
          case v: Boolean => v
          case _ => false
        }
      } else false
      setSide(side, value)
    }
    result(previous)
  }

  @Callback(direct = true, doc = "function():table -- Returns current open/close state of all sides in an array, indexed by direction.")
  def getSides(context: Context, args: Arguments): Array[AnyRef] = result(currentStatus())

  def setSideHelper(args: Arguments, value: Boolean): Array[AnyRef] = {
    val sideIndex = args.checkInteger(0)
    if (sideIndex < 0 || sideIndex > 5)
      return result(Unit, "invalid direction")
    val side = Direction.from3DDataValue(sideIndex)
    result(setSide(side, value))
  }

  @Callback(doc = "function(side: number):boolean -- Open the side, returns true if it changed to open.")
  def open(context: Context, args: Arguments): Array[AnyRef] = setSideHelper(args, value = true)

  @Callback(doc = "function(side: number):boolean -- Close the side, returns true if it changed to close.")
  def close(context: Context, args: Arguments): Array[AnyRef] = setSideHelper(args, value = false)
}
