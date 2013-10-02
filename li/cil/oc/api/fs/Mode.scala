package li.cil.oc.api.fs

/**
 * Possible file modes.
 * <p/>
 * This is used when opening files from a `FileSystem`.
 */
object Mode extends Enumeration {
  /** Open a file in reading mode. */
  val Read = Value("Read")

  /** Open a file in writing mode, overwriting existing contents. */
  val Write = Value("Write")

  /** Open a file in append mode, writing new data after existing contents. */
  val Append = Value("Append")

  /**
   * Parses a mode from a string.
   *
   * @param value the string to parse.
   * @return the mode the string represents.
   * @throws IllegalArgumentException if the string cannot be parsed to a mode.
   */
  def parse(value: String) = value match {
    case "r" | "rb" => Read
    case "w" | "wb" => Write
    case "a" | "ab" => Append
    case _ => throw new IllegalArgumentException("unsupported mode")
  }
}
