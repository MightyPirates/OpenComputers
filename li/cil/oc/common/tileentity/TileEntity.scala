package li.cil.oc.common.tileentity

import cpw.mods.fml.relauncher.{Side, SideOnly}
import net.minecraft.block.Block
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.world.World

trait TileEntity {
  def world: World

  def x: Int

  def y: Int

  def z: Int

  def block: Block

  lazy val isClient = world.isRemote

  lazy val isServer = !isClient

  @SideOnly(Side.CLIENT)
  def readFromNBTForClient(nbt: NBTTagCompound) {}

  def writeToNBTForClient(nbt: NBTTagCompound) {}
}
