package li.cil.oc.server.fs

import java.io
import java.io.FileOutputStream
import li.cil.oc.api.fs.Mode

trait FileOutputStreamFileSystem extends FileInputStreamFileSystem with OutputStreamFileSystem {
  override def spaceTotal = -1

  override def spaceUsed = root.getFreeSpace

  // ----------------------------------------------------------------------- //

  override def rename(from: String, to: String) = new io.File(root, from).renameTo(new io.File(root, to))

  // ----------------------------------------------------------------------- //

  override protected def makeDirectory(path: String) = new io.File(root, path).mkdir()

  override protected def delete(path: String) = new io.File(root, path).delete()

  // ----------------------------------------------------------------------- //

  override protected def openOutputStream(path: String, mode: Mode.Value): Option[io.OutputStream] =
    Some(new FileOutputStream(new io.File(root, path), mode == Mode.Append))
}
