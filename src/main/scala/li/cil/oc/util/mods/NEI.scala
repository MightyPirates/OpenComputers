package li.cil.oc.util.mods

import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.item.ItemStack

object NEI {
  private lazy val (layoutManagerClass, getInstance, getStackUnderMouse) = try {
    val layoutManager = Class.forName("codechicken.nei.LayoutManager")
    val getInstance = layoutManager.getMethod("instance")
    val getStackUnderMouse = layoutManager.getMethod("getStackUnderMouse", classOf[GuiContainer], classOf[Int], classOf[Int])
    (layoutManager, getInstance, getStackUnderMouse)
  }
  catch {
    case _: Throwable => (null, null, null)
  }

  def isInputFocused =
    Mods.NotEnoughItems.isAvailable && layoutManagerClass != null && (try {
      layoutManagerClass.getDeclaredMethods.find(m => m.getName == "getInputFocused").fold(false)(m => m.invoke(null) != null)
    }
    catch {
      case _: Throwable => false
    })

  def hoveredStack(container: GuiContainer, mouseX: Int, mouseY: Int): Option[ItemStack] = {
    if (Mods.NotEnoughItems.isAvailable && layoutManagerClass != null && getInstance != null && getStackUnderMouse != null)
      try return Option(getStackUnderMouse.invoke(getInstance.invoke(null), container, mouseX.underlying(), mouseY.underlying()).asInstanceOf[ItemStack]) catch {
        case t: Throwable => println(t)
      }
    None
  }
}
