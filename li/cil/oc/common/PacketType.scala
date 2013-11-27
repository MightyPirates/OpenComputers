package li.cil.oc.common

object PacketType extends Enumeration {
  val
  ChargerStateRequest,
  ChargerStateResponse,

  ComputerStateRequest,
  ComputerStateResponse,

  ItemComponentAddress,

  PowerStateRequest,
  PowerStateResponse,

  RedstoneStateRequest,
  RedstoneStateResponse,

  RobotAnimateSwing,
  RobotAnimateTurn,
  RobotEquippedItemChange,
  RobotMove,
  RobotSelectedSlotChange,
  RobotStateRequest,
  RobotStateResponse,

  RotatableStateRequest,
  RotatableStateResponse,

  ScreenBufferRequest,
  ScreenBufferResponse,

  ScreenColorChange,
  ScreenCopy,
  ScreenDepthChange,
  ScreenFill,
  ScreenPowerChange,
  ScreenResolutionChange,
  ScreenSet,

  KeyDown,
  KeyUp,
  Clipboard,

  Analyze = Value
}