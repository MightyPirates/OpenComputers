package li.cil.oc.api.fs

object Mode extends Enumeration {
  val Read = Value("Read")
  val Write = Value("Write")
  val Append = Value("Append")

  def parse(value: String) = value match {
    case "r" | "rb" => Read
    case "w" | "wb" => Write
    case "a" | "ab" => Append
    case _ => throw new IllegalArgumentException("unsupported mode")
  }
}
