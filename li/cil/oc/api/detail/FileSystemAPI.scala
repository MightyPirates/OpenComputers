package li.cil.oc.api.detail

import li.cil.oc.api.FileSystem
import li.cil.oc.api.network.Node
import dan200.computer.api.{IMount, IWritableMount}

/** Avoids reflection structural types would induce. */
trait FileSystemAPI {
  def fromClass(clazz: Class[_], domain: String, root: String): Option[FileSystem]

  def fromSaveDirectory(root: String, capacity: Long, buffered: Boolean): Option[FileSystem]

  def fromMemory(capacity: Long): Option[FileSystem]

  def fromComputerCraft(mount: IMount): Option[FileSystem]

  def fromComputerCraft(mount: IWritableMount): Option[FileSystem]

  def asNode(fs: FileSystem): Option[Node]
}