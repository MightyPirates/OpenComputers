package li.cil.oc.common

object PacketType extends Enumeration {
  /** These are sent from the server to the client for partial updates. */
  val ScreenResolutionChange = Value("ScreenResolutionChange")
  val ScreenSet = Value("ScreenSet")
  val ScreenFill = Value("ScreenFill")
  val ScreenCopy = Value("ScreenCopy")

  /**
   * Full buffer request / response.
   *
   * This is necessary when a tile entity containing a screen is created for
   * the first time on the client side, for example, since tile entities are
   * not automatically synchronized via read/write NBT.
   */
  val ScreenBufferRequest = Value("ScreenBufferRequest")
  val ScreenBufferResponse = Value("ScreenBufferResponse")

  /**
   * Computer running / stopped.
   *
   * Same as for screen, but for computer running state. The running state is
   * used on the client side to display different textures based on whether the
   * computer is running or not.
   */
  val ComputerStateRequest = Value("ComputerStateRequest")
  val ComputerStateResponse = Value("ComputerStateResponse")

  /** Sent by rotatable tile entities to notify clients of changes. */
  val RotatableStateRequest = Value("RotatableStateRequest")
  val RotatableStateResponse = Value("RotatableStateResponse")

  /** Sent by clients on keyboard input for computers. */
  val KeyDown = Value("KeyDown")
  val KeyUp = Value("KeyUp")
  val Clipboard = Value("Clipboard")

  /** Sent by redstone capable blocks (e.g. computers). */
  val RedstoneStateRequest = Value("RedstoneStateRequest")
  val RedstoneStateResponse = Value("RedstoneStateResponse")
}