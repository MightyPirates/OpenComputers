package li.cil.oc.server.fs

import java.io
import java.io.FileOutputStream
import li.cil.oc.api.fs.Mode

trait FileOutputStreamFileSystem extends FileInputStreamFileSystem with OutputStreamFileSystem {
  override protected def openOutputStream(path: String, mode: Mode.Value): Option[io.OutputStream] =
    Some(new FileOutputStream(new io.File(root, path), mode == Mode.Append))

  override def rename(from: String, to: String) = new io.File(root, from).renameTo(new io.File(root, to))

  override protected def delete(path: String) = new io.File(root, path).delete()
}
