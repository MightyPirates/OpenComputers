package li.cil.oc.common

object PacketType extends Enumeration {
  /**
   * Computer running / stopped.
   *
   * Same as for screen, but for computer running state. The running state is
   * used on the client side to display different textures based on whether the
   * computer is running or not.
   */
  val ComputerStateRequest = Value("ComputerStateRequest")
  val ComputerStateResponse = Value("ComputerStateResponse")

  /** Sent by power distributors for client display of average buffer fill. */
  val PowerStateRequest = Value("PowerStateRequest")
  val PowerStateResponse = Value("PowerStateResponse")

  /** Sent by redstone capable blocks (e.g. computers). */
  val RedstoneStateRequest = Value("RedstoneStateRequest")
  val RedstoneStateResponse = Value("RedstoneStateResponse")

  /** Sent by rotatable tile entities to notify clients of changes. */
  val RotatableStateRequest = Value("RotatableStateRequest")
  val RotatableStateResponse = Value("RotatableStateResponse")

  /**
   * Full buffer request / response.
   *
   * This is necessary when a tile entity containing a screen is created for
   * the first time on the client side, for example, since tile entities are
   * not automatically synchronized via read/write NBT.
   */
  val ScreenBufferRequest = Value("ScreenBufferRequest")
  val ScreenBufferResponse = Value("ScreenBufferResponse")

  /** These are sent from the server to the client for partial updates. */
  val ScreenColorChange = Value("ScreenColorChange")
  val ScreenCopy = Value("ScreenCopy")
  val ScreenDepthChange = Value("ScreenDepthChange")
  val ScreenFill = Value("ScreenFill")
  val ScreenResolutionChange = Value("ScreenResolutionChange")
  val ScreenSet = Value("ScreenSet")

  /** Sent by analyzer tool when used. */
  val Analyze = Value("Analyze")

  /** Sent by clients on keyboard input for computers. */
  val KeyDown = Value("KeyDown")
  val KeyUp = Value("KeyUp")
  val Clipboard = Value("Clipboard")
}