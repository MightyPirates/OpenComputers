package li.cil.oc.server.fs

import dan200.computer.api.IWritableMount
import li.cil.oc.api.fs.Mode

class ComputerCraftWritableFileSystem(override val mount: IWritableMount)
  extends ComputerCraftFileSystem(mount)
  with OutputStreamFileSystem {

  override def delete(path: String) = try {
    mount.delete(path)
    true
  } catch {
    case _: Throwable => false
  }

  override def makeDirectory(path: String) = try {
    mount.makeDirectory(path)
    true
  } catch {
    case _: Throwable => false
  }

  override protected def openOutputStream(path: String, mode: Mode.Value) = try {
    Some(mode match {
      case Mode.Append => mount.openForAppend(path)
      case Mode.Write => mount.openForWrite(path)
    })
  } catch {
    case _: Throwable => None
  }
}
