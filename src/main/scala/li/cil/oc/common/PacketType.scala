package li.cil.oc.common

object PacketType extends Enumeration {
  val
  // Server -> Client
  AbstractBusState,
  Analyze,
  ChargerState,
  ColorChange,
  ComputerState,
  ComputerUserList,
  DisassemblerActiveChange,
  HologramClear,
  HologramColor,
  HologramPowerChange,
  HologramScale,
  HologramSet,
  PetVisibility, // Goes both ways.
  PowerState,
  RedstoneState,
  RobotAnimateSwing,
  RobotAnimateTurn,
  RobotAssemblingState,
  RobotInventoryChange,
  RobotMove,
  RobotSelectedSlotChange,
  RotatableState,
  RouterActivity,
  TextBufferColorChange,
  TextBufferCopy,
  TextBufferDepthChange,
  TextBufferFill,
  TextBufferPaletteChange,
  TextBufferPowerChange,
  TextBufferResolutionChange,
  TextBufferSet,
  ServerPresence,
  Sound,

  // Client -> Server
  ComputerPower,
  KeyDown,
  KeyUp,
  Clipboard,
  MouseClickOrDrag,
  MouseScroll,
  MouseUp,
  MultiPartPlace,
  RobotAssemblerStart,
  RobotStateRequest,
  ServerSide,
  ServerRange = Value
}