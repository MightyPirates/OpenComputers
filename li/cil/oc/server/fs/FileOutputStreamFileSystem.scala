package li.cil.oc.server.fs

import java.io
import java.io.FileOutputStream
import li.cil.oc.api.fs.Mode

trait FileOutputStreamFileSystem extends FileInputStreamFileSystem {
  protected def openOutputStream(path: String, mode: Mode.Value) =
    Some(new FileOutputStream(new io.File(root, path), mode == Mode.Append))
}
