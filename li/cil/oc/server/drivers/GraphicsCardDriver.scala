package li.cil.oc.server.drivers

import li.cil.oc.Items
import li.cil.oc.api.Callback
import li.cil.oc.api.ComponentType
import li.cil.oc.api.scala.IComputerContext
import li.cil.oc.api.scala.IItemDriver
import li.cil.oc.common.components.Screen
import li.cil.oc.common.util.ItemComponentCache
import li.cil.oc.server.components.GraphicsCard
import net.minecraft.item.ItemStack

object GraphicsCardDriver extends IItemDriver {
  // ----------------------------------------------------------------------- //
  // API
  // ----------------------------------------------------------------------- //

  @Callback
  def setResolution(computer: IComputerContext, idGpu: Int, idScreen: Int, w: Int, h: Int) =
    computer.component[GraphicsCard](idGpu).resolution(computer.component[Screen](idScreen), (w, h))

  @Callback
  def getResolution(computer: IComputerContext, idGpu: Int, idScreen: Int) = {
    val res = computer.component[GraphicsCard](idGpu).resolution(computer.getComponent[Screen](idScreen))
    Array(res._1, res._2)
  }

  @Callback
  def resolutions(computer: IComputerContext, idGpu: Int, idScreen: Int) =
    computer.component[GraphicsCard](idGpu).supportedResolutions.
      intersect(computer.component[Screen](idScreen).supportedResolutions)

  @Callback
  def set(computer: IComputerContext, idGpu: Int, idScreen: Int, x: Int, y: Int, value: String) =
    computer.component[GraphicsCard](idGpu).set(computer.component[Screen](idScreen), x - 1, y - 1, value)

  @Callback
  def fill(computer: IComputerContext, idGpu: Int, idScreen: Int, x: Int, y: Int, w: Int, h: Int, value: String) = {
    if (value == null || value.length < 1)
      throw new IllegalArgumentException("bad argument #2 (invalid string)")
    computer.component[GraphicsCard](idGpu).fill(computer.component[Screen](idScreen), x - 1, y - 1, w, h, value.charAt(0))
  }

  @Callback
  def copy(computer: IComputerContext, idGpu: Int, idScreen: Int, x: Int, y: Int, w: Int, h: Int, tx: Int, ty: Int) =
    computer.component[GraphicsCard](idGpu).copy(computer.component[Screen](idScreen), x - 1, y - 1, w, h, tx, ty)

  // ----------------------------------------------------------------------- //
  // IDriver / IItemDriver
  // ----------------------------------------------------------------------- //

  def componentName = "gpu"

  override def apiName = Some("gpu")

  def worksWith(item: ItemStack) = item.itemID == Items.gpu.itemID

  def componentType(item: ItemStack) = ComponentType.PCI

  def component(item: ItemStack) = ItemComponentCache.get[GraphicsCard](item)
}