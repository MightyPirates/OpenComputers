package li.cil.oc.common.tileentity

import net.minecraft.world.World

trait TileEntity {
  def world: World

  def x: Int

  def y: Int

  def z: Int
}
