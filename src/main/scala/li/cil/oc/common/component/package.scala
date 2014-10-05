package li.cil.oc.common

package object component {
  implicit def result(args: Any*): Array[AnyRef] = li.cil.oc.util.ResultWrapper.result(args: _*)
}
