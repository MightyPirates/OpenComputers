package li.cil.oc.api.detail

import li.cil.oc.api.FileSystem
import li.cil.oc.api.network.Node

/** Avoids reflection structural types would induce. */
trait FileSystemAPI {
  def fromClass(clazz: Class[_], domain: String, root: String): Option[FileSystem]

  def fromSaveDir(root: String, capacity: Long): Option[FileSystem]

  def fromRam(capacity: Long): Option[FileSystem]

  def asNode(fs: FileSystem): Option[Node]
}