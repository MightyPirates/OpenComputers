package li.cil.oc.common

object PacketType extends Enumeration {
  val
  // Server -> Client
  Analyze,
  ChargerState,
  ComputerState,
  PowerState,
  RedstoneState,
  RobotAnimateSwing,
  RobotAnimateTurn,
  RobotEquippedItemChange,
  RobotMove,
  RobotSelectedSlotChange,
  RobotState,
  RotatableState,
  ScreenColorChange,
  ScreenCopy,
  ScreenDepthChange,
  ScreenFill,
  ScreenPowerChange,
  ScreenResolutionChange,
  ScreenSet,

  // Client -> Server
  ComputerPower,
  KeyDown,
  KeyUp,
  Clipboard,
  MouseClick = Value
}