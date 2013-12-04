package li.cil.oc.common

object PacketType extends Enumeration {
  val
  // Server -> Client
  Analyze,
  ChargerState,
  ComputerState,
  ComputerUserList,
  PowerState,
  RedstoneState,
  RobotAnimateSwing,
  RobotAnimateTurn,
  RobotEquippedItemChange,
  RobotMove,
  RobotSelectedSlotChange,
  RobotXp,
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