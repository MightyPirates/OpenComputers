package li.cil.oc.server.fs

trait Volatile extends VirtualFileSystem {
  override def close() {
    super.close()
    root.children.clear()
  }
}
