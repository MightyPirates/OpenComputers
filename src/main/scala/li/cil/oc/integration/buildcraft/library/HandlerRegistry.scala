package li.cil.oc.integration.buildcraft.library

import buildcraft.api.library.LibraryAPI

// Extra layer of indirection to avoid class not found errors, because Java
// tries to resolve interfaces in method calls even if the referencing code is
// never reached when loading the containing class.
object HandlerRegistry {
  def init(): Unit = {
    LibraryAPI.registerHandler(EEPROMHandler)
  }
}
