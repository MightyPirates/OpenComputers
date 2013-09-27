package li.cil.oc.server.components

import net.minecraftforge.common.ForgeDirection

trait RedstoneEnabled {
  protected val _output = Array.fill(6)(0)

  def input(side: ForgeDirection): Int

  def output = new {
    def apply(side: ForgeDirection) = _output(side.ordinal)

    def update(side: ForgeDirection, value: Int) = _output(side.ordinal) = value
  }
}
