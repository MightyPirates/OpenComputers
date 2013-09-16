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
}