package li.cil.oc.api.detail

import li.cil.oc.api.network.Node
import li.cil.oc.api.FileSystem

/** Avoids reflection structural types would induce. */
trait FileSystemAPI {
  def fromClass(clazz: Class[_], domain: String, root: String): Option[FileSystem]

  def asNode(fs: FileSystem): Option[Node]
}