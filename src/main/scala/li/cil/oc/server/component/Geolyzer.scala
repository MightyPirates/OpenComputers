package li.cil.oc.server.component

import java.util

import li.cil.oc.Constants
import li.cil.oc.api.driver.DeviceInfo.DeviceAttribute
import li.cil.oc.api.driver.DeviceInfo.DeviceClass
import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.api.driver.DeviceInfo
import li.cil.oc.api.network.EnvironmentHost
import li.cil.oc.api.event.GeolyzerEvent
import li.cil.oc.api.event.GeolyzerEvent.Analyze
import li.cil.oc.api.internal
import li.cil.oc.api.machine.Arguments
import li.cil.oc.api.machine.Callback
import li.cil.oc.api.machine.Context
import li.cil.oc.api.network.Message
import li.cil.oc.api.network.Visibility
import li.cil.oc.api.prefab
import li.cil.oc.util.BlockPosition
import li.cil.oc.util.DatabaseAccess
import li.cil.oc.util.ExtendedArguments._
import li.cil.oc.util.ExtendedWorld._
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.EnumFacing
import net.minecraftforge.common.MinecraftForge

import scala.collection.convert.WrapAsJava._
import scala.collection.convert.WrapAsScala._
import scala.language.existentials

class Geolyzer(val host: EnvironmentHost) extends prefab.ManagedEnvironment with DeviceInfo {
  override val node = api.Network.newNode(this, Visibility.Network).
    withComponent("geolyzer").
    withConnector().
    create()

  private final lazy val deviceInfo = Map(
    DeviceAttribute.Class -> DeviceClass.Generic,
    DeviceAttribute.Description -> "Geolyzer",
    DeviceAttribute.Vendor -> Constants.DeviceInfo.DefaultVendor,
    DeviceAttribute.Product -> "Terrain Analyzer MkII",
    DeviceAttribute.Capacity -> Settings.get.geolyzerRange.toString
  )

  override def getDeviceInfo: util.Map[String, String] = deviceInfo

  // ----------------------------------------------------------------------- //

  @Callback(doc = """function(x:number, z:number[, y:number, w:number, d:number, h:number][, ignoreReplaceable:boolean|options:table]):table -- Analyzes the density of the column at the specified relative coordinates.""")
  def scan(computer: Context, args: Arguments): Array[AnyRef] = {
    val (minX, minY, minZ, maxX, maxY, maxZ, optIndex) = getScanArgs(args)
    val volume = (maxX - minX + 1) * (maxZ - minZ + 1) * (maxY - minY + 1)
    if (volume > 64) throw new IllegalArgumentException("volume too large (maximum is 64)")
    val options = if (args.isBoolean(optIndex)) mapAsJavaMap(Map("includeReplaceable" -> !args.checkBoolean(optIndex))) else args.optTable(optIndex, Map.empty[AnyRef, AnyRef])
    if (math.abs(minX) > Settings.get.geolyzerRange || math.abs(maxX) > Settings.get.geolyzerRange ||
      math.abs(minY) > Settings.get.geolyzerRange || math.abs(maxY) > Settings.get.geolyzerRange ||
      math.abs(minZ) > Settings.get.geolyzerRange || math.abs(maxZ) > Settings.get.geolyzerRange) {
      throw new IllegalArgumentException("location out of bounds")
    }

    if (!node.tryChangeBuffer(-Settings.get.geolyzerScanCost))
      return result(Unit, "not enough energy")

    val event = new GeolyzerEvent.Scan(host, options, minX, minY, minZ, maxX, maxY, maxZ)
    MinecraftForge.EVENT_BUS.post(event)
    if (event.isCanceled) result(Unit, "scan was canceled")
    else result(event.data)
  }

  private def getScanArgs(args: Arguments) = {
    val minX = args.checkInteger(0)
    val minZ = args.checkInteger(1)
    if (args.isInteger(2) && args.isInteger(3) && args.isInteger(4) && args.isInteger(5)) {
      val minY = args.checkInteger(2)
      val w = args.checkInteger(3)
      val d = args.checkInteger(4)
      val h = args.checkInteger(5)
      val maxX = minX + w - 1
      val maxY = minY + h - 1
      val maxZ = minZ + d - 1

      (math.min(minX, maxX), math.min(minY, maxY), math.min(minZ, maxZ),
        math.max(minX, maxX), math.max(minY, maxY), math.max(minZ, maxZ),
        6)
    }
    else {
      (minX, -32, minZ, minX, 31, minZ, 2)
    }
  }

  @Callback(doc = """function(side:number[,options:table]):table -- Get some information on a directly adjacent block.""")
  def analyze(computer: Context, args: Arguments): Array[AnyRef] = if (Settings.get.allowItemStackInspection) {
    val side = args.checkSideAny(0)
    val globalSide = host match {
      case rotatable: internal.Rotatable => rotatable.toGlobal(side)
      case _ => side
    }
    val options = args.optTable(1, Map.empty[AnyRef, AnyRef])

    if (!node.tryChangeBuffer(-Settings.get.geolyzerScanCost))
      return result(Unit, "not enough energy")

    val globalPos = BlockPosition(host).offset(globalSide)
    val event = new Analyze(host, options, globalPos.toBlockPos)
    MinecraftForge.EVENT_BUS.post(event)
    if (event.isCanceled) result(Unit, "scan was canceled")
    else result(event.data)
  }
  else result(Unit, "not enabled in config")

  @Callback(doc = """function(side:number, dbAddress:string, dbSlot:number):boolean -- Store an item stack representation of the block on the specified side in a database component.""")
  def store(computer: Context, args: Arguments): Array[AnyRef] = {
    val side = args.checkSideAny(0)
    val globalSide = host match {
      case rotatable: internal.Rotatable => rotatable.toGlobal(side)
      case _ => side
    }

    if (!node.tryChangeBuffer(-Settings.get.geolyzerScanCost))
      return result(Unit, "not enough energy")

    val blockPos = BlockPosition(host).offset(globalSide)
    val block = host.world.getBlock(blockPos)
    val item = Item.getItemFromBlock(block)
    if (item == null) result(Unit, "block has no registered item representation")
    else {
      val metadata = host.world.getBlockMetadata(blockPos)
      val damage = block.damageDropped(metadata)
      val stack = new ItemStack(item, 1, damage)
      DatabaseAccess.withDatabase(node, args.checkString(1), database => {
        val toSlot = args.checkSlot(database.data, 2)
        val nonEmpty = database.getStackInSlot(toSlot) != null
        database.setStackInSlot(toSlot, stack)
        result(nonEmpty)
      })
    }
  }

  override def onMessage(message: Message): Unit = {
    super.onMessage(message)
    if (message.name == "tablet.use") message.source.host match {
      case machine: api.machine.Machine => (machine.host, message.data) match {
        case (tablet: internal.Tablet, Array(nbt: NBTTagCompound, stack: ItemStack, player: EntityPlayer, blockPos: BlockPosition, side: EnumFacing, hitX: java.lang.Float, hitY: java.lang.Float, hitZ: java.lang.Float)) =>
          if (node.tryChangeBuffer(-Settings.get.geolyzerScanCost)) {
            val event = new Analyze(host, Map.empty[AnyRef, AnyRef], blockPos.toBlockPos)
            MinecraftForge.EVENT_BUS.post(event)
            if (!event.isCanceled) {
              for ((key, value) <- event.data) value match {
                case number: java.lang.Number => nbt.setDouble(key, number.doubleValue())
                case string: String if !string.isEmpty => nbt.setString(key, string)
                case _ => // Unsupported, ignore.
              }
            }
          }
        case _ => // Ignore.
      }
      case _ => // Ignore.
    }
  }
}
