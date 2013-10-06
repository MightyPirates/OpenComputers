package li.cil.oc.server.fs

import java.io
import java.io.{FileNotFoundException, FileInputStream}
import li.cil.oc.api

trait FileInputStreamFileSystem extends api.FileSystem with InputStreamFileSystem {
  protected val root: io.File

  // ----------------------------------------------------------------------- //

  override def exists(path: String) = new io.File(root, path).exists()

  override def size(path: String) = new io.File(root, path) match {
    case file if file.isFile => file.length()
    case _ => 0L
  }

  override def isDirectory(path: String) = new io.File(root, path).isDirectory

  override def list(path: String) = new io.File(root, path) match {
    case file if file.exists() && file.isFile => Some(Array(file.getName))
    case directory if directory.exists() && directory.isDirectory => Some(directory.listFiles().
      map(file => if (file.isDirectory) file.getName + "/" else file.getName))
    case _ => throw new FileNotFoundException("no such file or directory")
  }

  // ----------------------------------------------------------------------- //

  override protected def openInputStream(path: String) =
    Some(new FileInputStream(new io.File(root, path)))
}
