package li.cil.oc.common

object PacketType extends Enumeration {
  val ComputerStateRequest = Value("ComputerStateRequest")
  val ComputerStateResponse = Value("ComputerStateResponse")

  val PowerStateRequest = Value("PowerStateRequest")
  val PowerStateResponse = Value("PowerStateResponse")

  val RedstoneStateRequest = Value("RedstoneStateRequest")
  val RedstoneStateResponse = Value("RedstoneStateResponse")

  val RobotMove = Value("RobotMove")
  val RobotSelectedSlotChange = Value("RobotSelectedSlotChange")
  val RobotStateRequest = Value("RobotSelectedSlotRequest")
  val RobotStateResponse = Value("RobotStateResponse")

  val RotatableStateRequest = Value("RotatableStateRequest")
  val RotatableStateResponse = Value("RotatableStateResponse")

  val ScreenBufferRequest = Value("ScreenBufferRequest")
  val ScreenBufferResponse = Value("ScreenBufferResponse")

  val ScreenColorChange = Value("ScreenColorChange")
  val ScreenCopy = Value("ScreenCopy")
  val ScreenDepthChange = Value("ScreenDepthChange")
  val ScreenFill = Value("ScreenFill")
  val ScreenPowerChange = Value("ScreenPowerChange")
  val ScreenResolutionChange = Value("ScreenResolutionChange")
  val ScreenSet = Value("ScreenSet")

  val KeyDown = Value("KeyDown")
  val KeyUp = Value("KeyUp")
  val Clipboard = Value("Clipboard")

  val Analyze = Value("Analyze")
}