package li.cil.oc.server.component

import net.minecraftforge.common.ForgeDirection

trait RedstoneEnabled {
  protected val _output = Array.fill(6)(0)

  def input(side: ForgeDirection): Int

  def output = _outputAccess

  /** Avoid reflective access. */
  class OutputAccess(val parent: RedstoneEnabled) {
    def apply(side: ForgeDirection) = parent._output(side.ordinal)

    def update(side: ForgeDirection, value: Int) = parent._output(side.ordinal) = value
  }
  private val _outputAccess = new OutputAccess(this)
}
