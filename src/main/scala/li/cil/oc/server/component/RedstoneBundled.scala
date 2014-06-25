package li.cil.oc.server.component

import li.cil.oc.api.network.{Arguments, Callback, Context}
import li.cil.oc.common.tileentity.traits.BundledRedstoneAware

trait RedstoneBundled extends Redstone[BundledRedstoneAware] {
  @Callback(direct = true, doc = """function(side:number, color:number):number -- Get the bundled redstone input on the specified side and with the specified color.""")
  def getBundledInput(context: Context, args: Arguments): Array[AnyRef] = {
    val side = checkSide(args, 0)
    val color = checkColor(args, 1)
    result(owner.bundledInput(side, color))
  }

  @Callback(direct = true, doc = """function(side:number, color:number):number -- Get the bundled redstone output on the specified side and with the specified color.""")
  def getBundledOutput(context: Context, args: Arguments): Array[AnyRef] = {
    val side = checkSide(args, 0)
    val color = checkColor(args, 1)
    result(owner.bundledOutput(side, color))
  }

  @Callback(doc = """function(side:number, color:number, value:number):number -- Set the bundled redstone output on the specified side and with the specified color.""")
  def setBundledOutput(context: Context, args: Arguments): Array[AnyRef] = {
    val side = checkSide(args, 0)
    val color = checkColor(args, 1)
    val value = args.checkInteger(2)
    owner.bundledOutput(side, color, value)
    context.pause(0.1)
    result(owner.bundledOutput(side, color))
  }

  // ----------------------------------------------------------------------- //

  private def checkColor(args: Arguments, index: Int): Int = {
    val color = args.checkInteger(index)
    if (color < 0 || color > 15)
      throw new IllegalArgumentException("invalid color")
    color
  }
}
