package li.cil.oc.common

object PacketType extends Enumeration {
  val ScreenResolutionChange = Value("ScreenResolutionChange")
  val ScreenSet = Value("ScreenSet")
  val ScreenFill = Value("ScreenFill")
  val ScreenCopy = Value("ScreenCopy")
}