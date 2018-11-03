package li.cil.oc.server.component

import java.util

import li.cil.oc.Constants
import li.cil.oc.api.driver.DeviceInfo.DeviceAttribute
import li.cil.oc.api.driver.DeviceInfo.DeviceClass
import li.cil.oc.Settings
import li.cil.oc.api.network.EnvironmentHost
import li.cil.oc.api.machine.Arguments
import li.cil.oc.api.machine.Callback
import li.cil.oc.api.machine.Context
import li.cil.oc.common.tileentity.traits.BundledRedstoneAware
import net.minecraftforge.common.util.ForgeDirection

import scala.collection.convert.WrapAsJava._

trait RedstoneBundled extends RedstoneVanilla {
  private final lazy val deviceInfo = Map(
    DeviceAttribute.Class -> DeviceClass.Communication,
    DeviceAttribute.Description -> "Advanced redstone controller",
    DeviceAttribute.Vendor -> Constants.DeviceInfo.DefaultVendor,
    DeviceAttribute.Product -> "Rb800-M",
    DeviceAttribute.Capacity -> "65536",
    DeviceAttribute.Width -> "16"
  )

  override def getDeviceInfo: util.Map[String, String] = deviceInfo

  private val COLOR_RANGE = 0 until 16

  // ----------------------------------------------------------------------- //

  override def redstone: EnvironmentHost with BundledRedstoneAware

  private def getBundleKey(args: Arguments): (Option[ForgeDirection], Option[Int]) = {
    args.count match {
      case 2 => (Option(checkSide(args, 0)), Option(checkColor(args, 1)))
      case 1 => (Option(checkSide(args, 0)), None)
      case 0 => (None, None)
      case _ => throw new Exception("too many arguments, expected 0, 1, or 2")
    }
  }

  private def tableToColorValues(table: util.Map[_, _]): Array[Int] = {
    COLOR_RANGE.collect {
      case color: Int if table.containsKey(color) => {
        table.get(color) match {
          case value: Integer => value.toInt
        }
      }
    }.toArray
  }

  private def colorsToMap(ar: Array[Int]): Map[Int, Int] = {
    COLOR_RANGE.map{
      case color if color < ar.length => color -> ar(color)
    }.toMap
  }

  private def sidesToMap(ar: Array[Array[Int]]): Map[Int, Map[Int, Int]] = {
    SIDE_RANGE.map {
      case side if side.ordinal < ar.length && ar(side.ordinal).length > 0 => side.ordinal -> colorsToMap(ar(side.ordinal))
    }.toMap
  }

  private def getBundleAssignment(args: Arguments): (Any, Any, Any) = {
    args.count match {
      case 3 => (checkSide(args, 0), checkColor(args, 1), args.checkInteger(2))
      case 2 => (checkSide(args, 0), args.checkTable(1), null)
      case 1 => (args.checkTable(0), null, null)
      case _ => throw new Exception("invalid number of arguments, expected 1, 2, or 3")
    }
  }

  @Callback(direct = true, doc = "function([side:number[, color:number]]):number or table -- Fewer params returns set of inputs")
  def getBundledInput(context: Context, args: Arguments): Array[AnyRef] = {
    val (side, color) = getBundleKey(args)

    if (color.isDefined) {
      result(redstone.getBundledInput(side.get, color.get))
    } else if (side.isDefined) {
      result(colorsToMap(redstone.getBundledInput(side.get)))
    } else {
      result(sidesToMap(redstone.getBundledInput))
    }
  }

  @Callback(direct = true, doc = "function([side:number[, color:number]]):number or table -- Fewer params returns set of outputs")
  def getBundledOutput(context: Context, args: Arguments): Array[AnyRef] = {
    val (side, color) = getBundleKey(args)

    if (color.isDefined) {
      result(redstone.getBundledOutput(side.get, color.get))
    } else if (side.isDefined) {
      result(colorsToMap(redstone.getBundledOutput(side.get)))
    } else {
      result(sidesToMap(redstone.getBundledOutput))
    }
  }

  @Callback(doc = "function([side:number[, color:number,]] value:number or table):number or table --  Fewer params to assign set of outputs. Returns previous values")
  def setBundledOutput(context: Context, args: Arguments): Array[AnyRef] = {
    var ret: AnyRef = null
    if (getBundleAssignment(args) match {
      case (side: ForgeDirection, color: Int, value: Int) =>
        ret = redstone.getBundledOutput(side, color)
        redstone.setBundledOutput(side, color, value)
      case (side: ForgeDirection, value: util.Map[_, _], _) =>
        ret = redstone.getBundledOutput(side)
        redstone.setBundledOutput(side, value)
      case (value: util.Map[_, _], _, _) =>
        ret = redstone.getBundledOutput
        redstone.setBundledOutput(value)
    }) {
      if (Settings.get.redstoneDelay > 0)
        context.pause(Settings.get.redstoneDelay)
    }
    result(ret)
  }

  // ----------------------------------------------------------------------- //

  private def checkColor(args: Arguments, index: Int): Int = {
    val color = args.checkInteger(index)
    if (!COLOR_RANGE.contains(color))
      throw new IllegalArgumentException("invalid color")
    color
  }
}
