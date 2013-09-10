package li.cil.oc.server.drivers

import li.cil.oc.Config
import li.cil.oc.api.Callback
import li.cil.oc.api.ComponentType
import li.cil.oc.api.IComputerContext
import li.cil.oc.api.IItemDriver
import li.cil.oc.common.items.ItemGraphicsCard
import li.cil.oc.server.components.GraphicsCard
import li.cil.oc.server.components.Screen
import net.minecraft.item.ItemStack

object GraphicsCardDriver extends IItemDriver {
  // ----------------------------------------------------------------------- //
  // API
  // ----------------------------------------------------------------------- //

  @Callback
  def setResolution(computer: IComputerContext, idGpu: Int, w: Int, h: Int) =
    computer.component[GraphicsCard](idGpu).resolution = (w, h)

  @Callback
  def getResolution(computer: IComputerContext, idGpu: Int) = {
    val res = computer.component[GraphicsCard](idGpu).resolution
    Array(res._1, res._2)
  }

  @Callback
  def resolutions(computer: IComputerContext, idGpu: Int) =
    computer.component[GraphicsCard](idGpu).resolutions

  @Callback
  def set(computer: IComputerContext, idGpu: Int, x: Int, y: Int, value: String) =
    computer.component[GraphicsCard](idGpu).set(x, y, value)

  @Callback
  def fill(computer: IComputerContext, idGpu: Int, value: String, x: Int, y: Int, w: Int, h: Int) = {
    if (value == null || value.length < 1)
      throw new IllegalArgumentException("bad argument #2 (invalid string)")
    computer.component[GraphicsCard](idGpu).fill(x, y, w, h, value.charAt(0))
  }

  @Callback
  def copy(computer: IComputerContext, idGpu: Int, x: Int, y: Int, w: Int, h: Int, tx: Int, ty: Int) =
    computer.component[GraphicsCard](idGpu).copy(x, y, w, h, tx, ty)

  /**
   * Binds the GPU to the specified monitor, meaning it'll send its output to
   * that monitor from now on.
   *
   * TODO Add another parameter to define the buffer to bind to the monitor, in
   * case we add advanced GPUs that support multiple buffers + monitors.
   */
  @Callback
  def bind(computer: IComputerContext, idGpu: Int, idScreen: Int) = {
    val gpu = computer.component[GraphicsCard](idGpu)
    if (idScreen > 0) gpu.bind(computer.component[Screen](idScreen))
    else gpu.bind(null)
  }

  // ----------------------------------------------------------------------- //
  // IDriver
  // ----------------------------------------------------------------------- //

  def componentName = "gpu"

  override def apiName = "gpu"

  def id(component: Any) = component.asInstanceOf[GraphicsCard].id

  def id(component: Any, id: Int) = component.asInstanceOf[GraphicsCard].id = id

  // ----------------------------------------------------------------------- //
  // IItemDriver
  // ----------------------------------------------------------------------- //

  def worksWith(item: ItemStack) = item.itemID == Config.itemGPUId

  def componentType(item: ItemStack) = ComponentType.PCI

  def component(item: ItemStack) = ItemGraphicsCard.getComponent(item)
}