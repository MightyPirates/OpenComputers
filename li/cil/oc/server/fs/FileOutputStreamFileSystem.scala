package li.cil.oc.server.fs

import java.io
import java.io.FileOutputStream
import li.cil.oc.api.fs.Mode

trait FileOutputStreamFileSystem extends FileInputStreamFileSystem with OutputStreamFileSystem {
  override def spaceTotal = -1

  override def spaceUsed = root.getFreeSpace

  // ----------------------------------------------------------------------- //

  override def delete(path: String) = new io.File(root, path).delete()

  override def makeDirectory(path: String) = new io.File(root, path).mkdir()

  override def rename(from: String, to: String) = new io.File(root, from).renameTo(new io.File(root, to))

  override def setLastModified(path: String, time: Long) = new io.File(root, path).setLastModified(time)

  // ----------------------------------------------------------------------- //

  override protected def openOutputStream(path: String, mode: Mode.Value): Option[io.OutputStream] =
    Some(new FileOutputStream(new io.File(root, path), mode == Mode.Append))
}
