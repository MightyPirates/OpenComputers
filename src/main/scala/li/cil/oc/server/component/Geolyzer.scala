package li.cil.oc.server.component

import li.cil.oc.Settings
import li.cil.oc.api
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
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.common.util.ForgeDirection

import scala.collection.convert.WrapAsJava._
import scala.collection.convert.WrapAsScala._
import scala.language.existentials

class Geolyzer(val host: EnvironmentHost) extends prefab.ManagedEnvironment {
  override val node = api.Network.newNode(this, Visibility.Network).
    withComponent("geolyzer").
    withConnector().
    create()

  @Callback(doc = """function(x:number, z:number[, ignoreReplaceable:boolean|options:table]):table -- Analyzes the density of the column at the specified relative coordinates.""")
  def scan(computer: Context, args: Arguments): Array[AnyRef] = {
    val rx = args.checkInteger(0)
    val rz = args.checkInteger(1)
    val options = if (args.isBoolean(2)) mapAsJavaMap(Map("includeReplaceable" -> !args.checkBoolean(2))) else args.optTable(2, Map.empty[AnyRef, AnyRef])

    if (math.abs(rx) > Settings.get.geolyzerRange || math.abs(rz) > Settings.get.geolyzerRange) {
      throw new IllegalArgumentException("location out of bounds")
    }

    if (!node.tryChangeBuffer(-Settings.get.geolyzerScanCost))
      return result(Unit, "not enough energy")

    val event = new GeolyzerEvent.Scan(host, options, rx, rz)
    MinecraftForge.EVENT_BUS.post(event)
    if (event.isCanceled) result(Unit, "scan was canceled")
    else result(event.data)
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
    val event = new Analyze(host, options, globalPos.x, globalPos.y, globalPos.z)
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
        case (tablet: internal.Tablet, Array(nbt: NBTTagCompound, stack: ItemStack, player: EntityPlayer, blockPos: BlockPosition, side: ForgeDirection, hitX: java.lang.Float, hitY: java.lang.Float, hitZ: java.lang.Float)) =>
          if (node.tryChangeBuffer(-Settings.get.geolyzerScanCost)) {
            val event = new Analyze(host, Map.empty[AnyRef, AnyRef], blockPos.x, blockPos.y, blockPos.z)
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
