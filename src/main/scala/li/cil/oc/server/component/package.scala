package li.cil.oc.server

package object component {
  implicit def result(args: Any*): Array[AnyRef] = li.cil.oc.util.ResultWrapper.result(args: _*)
}
