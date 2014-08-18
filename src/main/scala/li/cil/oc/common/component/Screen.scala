package li.cil.oc.common.component

import li.cil.oc.api.network.{Arguments, Callback, Context}
import li.cil.oc.common.tileentity
import li.cil.oc.server.{PacketSender => ServerPacketSender}

class Screen(val screen: tileentity.Screen) extends TextBuffer(screen) {
  @Callback(direct = true, doc = """function():boolean -- Whether touch mode is inverted (sneak-activate opens GUI, instead of normal activate).""")
  def isTouchModeInverted(computer: Context, args: Arguments): Array[AnyRef] = result(screen.invertTouchMode)

  @Callback(doc = """function(value:boolean):boolean -- Sets whether to invert touch mode (sneak-activate opens GUI, instead of normal activate).""")
  def setTouchModeInverted(computer: Context, args: Arguments): Array[AnyRef] = {
    val newValue = args.checkBoolean(0)
    val oldValue = screen.invertTouchMode
    if (newValue != oldValue) {
      screen.invertTouchMode = newValue
      ServerPacketSender.sendScreenTouchMode(screen, newValue)
    }
    result(oldValue)
  }
}
