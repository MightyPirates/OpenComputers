package li.cil.oc.util.mods

object NEI {
  private lazy val layoutManagerClass = try {
    Class.forName("codechicken.nei.LayoutManager")
  }
  catch {
    case _: Throwable => null
  }

  def isInputFocused =
    Mods.NotEnoughItems.isAvailable && layoutManagerClass != null && (try {
      layoutManagerClass.getDeclaredMethods.find(m => m.getName == "getInputFocused").fold(false)(m => m.invoke(null) != null)
    }
    catch {
      case _: Throwable => false
    })
}
