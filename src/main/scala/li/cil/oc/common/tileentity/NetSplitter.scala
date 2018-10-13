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
import net.minecraft.init.SoundEvents
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.EnumFacing
import net.minecraft.util.SoundCategory
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly

import scala.collection.convert.WrapAsJava._
import scala.collection.mutable

class NetSplitter extends traits.Environment with traits.OpenSides with traits.RedstoneAware with api.network.SidedEnvironment with DeviceInfo {
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

  override def isSideOpen(side: EnumFacing): Boolean = if (isInverted) !super.isSideOpen(side) else super.isSideOpen(side)

  override def setSideOpen(side: EnumFacing, value: Boolean) {
    val previous = isSideOpen(side)
    super.setSideOpen(side, value)
    if (previous != isSideOpen(side)) {
      if (isServer) {
        node.remove()
        api.Network.joinOrCreateNetwork(this)
        ServerPacketSender.sendNetSplitterState(this)
        world.playSound(null, x + 0.5, y + 0.5, z + 0.5, SoundEvents.BLOCK_PISTON_EXTEND, SoundCategory.BLOCKS, 0.5f, world.rand.nextFloat() * 0.25f + 0.7f)
        world.notifyNeighborsOfStateChange(getPos, getBlockType)
      }
      else {
        world.notifyBlockUpdate(getPos, world.getBlockState(getPos), world.getBlockState(getPos), 3)
      }
    }
  }

  // ----------------------------------------------------------------------- //

  override def sidedNode(side: EnumFacing): Node = if (isSideOpen(side)) node else null

  @SideOnly(Side.CLIENT)
  override def canConnect(side: EnumFacing): Boolean = isSideOpen(side)

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
        world.playSound(null, x + 0.5, y + 0.5, z + 0.5, SoundEvents.BLOCK_PISTON_CONTRACT, SoundCategory.BLOCKS, 0.5f, world.rand.nextFloat() * 0.25f + 0.7f)
      }
      else {
        world.notifyBlockUpdate(getPos, getWorld.getBlockState(getPos), getWorld.getBlockState(getPos), 3)
      }
    }
  }

  // ----------------------------------------------------------------------- //

  private final val IsInvertedTag = Settings.namespace + "isInverted"
  private final val OpenSidesTag = Settings.namespace + "openSides"

  override def readFromNBTForServer(nbt: NBTTagCompound): Unit = {
    super.readFromNBTForServer(nbt)
    isInverted = nbt.getBoolean(IsInvertedTag)
  }

  override def writeToNBTForServer(nbt: NBTTagCompound): Unit = {
    super.writeToNBTForServer(nbt)
    nbt.setBoolean(IsInvertedTag, isInverted)
  }

  @SideOnly(Side.CLIENT) override
  def readFromNBTForClient(nbt: NBTTagCompound): Unit = {
    super.readFromNBTForClient(nbt)
    isInverted = nbt.getBoolean(IsInvertedTag)
  }

  override def writeToNBTForClient(nbt: NBTTagCompound): Unit = {
    super.writeToNBTForClient(nbt)
    nbt.setBoolean(IsInvertedTag, isInverted)
  }

  // component api
  def currentStatus(): mutable.Map[Int, Boolean] = {
    val openStatus = mutable.Map[Int, Boolean]()
    for (side <- EnumFacing.VALUES) {
      openStatus += side.ordinal() -> isSideOpen(side)
    }
    openStatus
  }

  def setSide(side: EnumFacing, state: Boolean): Boolean = {
    val previous = isSideOpen(side) // isSideOpen uses inverter
    setSideOpen(side, if (isInverted) !state else state) // but setSideOpen does not
    previous != state
  }

  @Callback(doc = "function(settings:table):table -- set open state (true/false) of all sides in an array; index by direction. Returns previous states")
  def setSides(context: Context, args: Arguments): Array[AnyRef] = {
    val settings = args.checkTable(0)
    val previous = currentStatus()
    for (side <- EnumFacing.VALUES) {
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
    val side = EnumFacing.getFront(sideIndex)
    result(setSide(side, value))
  }

  @Callback(doc = "function(side: number):boolean -- Open the side, returns true if it changed to open.")
  def open(context: Context, args: Arguments): Array[AnyRef] = setSideHelper(args, value = true)

  @Callback(doc = "function(side: number):boolean -- Close the side, returns true if it changed to close.")
  def close(context: Context, args: Arguments): Array[AnyRef] = setSideHelper(args, value = false)
}
