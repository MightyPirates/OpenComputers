package li.cil.oc.common.tileentity

import net.minecraft.block.Block
import net.minecraft.world.World

trait TileEntity {
  def world: World

  def x: Int

  def y: Int

  def z: Int

  def block: Block

  def isClient = world.isRemote

  def isServer = !isClient
}
