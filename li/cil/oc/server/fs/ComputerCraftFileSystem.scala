package li.cil.oc.server.fs

import dan200.computer.api.IMount

class ComputerCraftFileSystem(val mount: IMount) extends InputStreamFileSystem {
  override def exists(path: String) = mount.exists(path)

  override def isDirectory(path: String) = mount.isDirectory(path)

  override def list(path: String) = {
    val result = new java.util.ArrayList[String]
    mount.list(path, result)
    Some(result.toArray.asInstanceOf[Array[String]])
  }

  override def size(path: String) = mount.getSize(path)

  protected def openInputStream(path: String) = try {
    Some(mount.openForRead(path))
  } catch {
    case _: Throwable => None
  }
}
