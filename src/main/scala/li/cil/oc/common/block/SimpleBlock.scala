package li.cil.oc.common.block

import java.util

import cpw.mods.fml.relauncher.{Side, SideOnly}
import li.cil.oc.Settings
import li.cil.oc.common.tileentity
import li.cil.oc.util.{ItemCosts, Tooltip}
import net.minecraft.block.Block
import net.minecraft.block.material.Material
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.{Entity, EnumCreatureType}
import net.minecraft.item.{EnumRarity, ItemStack}
import net.minecraft.util.Vec3
import net.minecraft.world.{IBlockAccess, World}
import net.minecraftforge.common.util.ForgeDirection
import org.lwjgl.input

class SimpleBlock(material: Material) extends Block(material) {
  override def getUnlocalizedName = Settings.namespace + super.getUnlocalizedName

  def rarity = EnumRarity.common

  def setFacing(world: World, x: Int, y: Int, z: Int, value: ForgeDirection) =
    world.getTileEntity(x, y, z) match {
      case rotatable: tileentity.traits.Rotatable => rotatable.setFromFacing(value); true
      case _ => false
    }

  def setRotationFromEntityPitchAndYaw(world: World, x: Int, y: Int, z: Int, value: Entity) =
    world.getTileEntity(x, y, z) match {
      case rotatable: tileentity.traits.Rotatable => rotatable.setFromEntityPitchAndYaw(value); true
      case _ => false
    }

  override def rotateBlock(world: World, x: Int, y: Int, z: Int, axis: ForgeDirection) =
    world.getTileEntity(x, y, z) match {
      case rotatable: tileentity.traits.Rotatable if rotatable.rotate(axis) => world.markBlockForUpdate(x, y, z); true
      case _ => false
    }

  @SideOnly(Side.CLIENT)
  def tooltipLines(metadata: Int, stack: ItemStack, player: EntityPlayer, tooltip: java.util.List[String], advanced: Boolean) {
    tooltip.addAll(Tooltip.get(getUnlocalizedName))
    if (input.Keyboard.isKeyDown(input.Keyboard.KEY_LMENU)) {
      ItemCosts.addTooltip(stack, tooltip.asInstanceOf[util.List[String]])
    }
  }

  @SideOnly(Side.CLIENT)
  def preItemRender(metadata: Int) {}

  final override def setBlockBoundsForItemRender() = setBlockBoundsForItemRender(0)

  def setBlockBoundsForItemRender(metadata: Int) = super.setBlockBoundsForItemRender()

  override def getRenderType = Settings.blockRenderId

  override def canBeReplacedByLeaves(world: IBlockAccess, x: Int, y: Int, z: Int) = false

  override def canCreatureSpawn(creature: EnumCreatureType, world: IBlockAccess, x: Int, y: Int, z: Int) = false

  final override def canPlaceBlockOnSide(world: World, x: Int, y: Int, z: Int, side: Int) =
    canPlaceBlockOnSide(world, x, y, z, ForgeDirection.getOrientation(side).getOpposite)

  def canPlaceBlockOnSide(world: World, x: Int, y: Int, z: Int, side: ForgeDirection) =
    super.canPlaceBlockOnSide(world, x, y, z, side.getOpposite.ordinal)

  final override def canConnectRedstone(world: IBlockAccess, x: Int, y: Int, z: Int, side: Int) =
    canConnectRedstone(world, x, y, z, side match {
      case -1 => ForgeDirection.UP
      case 0 => ForgeDirection.NORTH
      case 1 => ForgeDirection.EAST
      case 2 => ForgeDirection.SOUTH
      case 3 => ForgeDirection.WEST
    })

  def canConnectRedstone(world: IBlockAccess, x: Int, y: Int, z: Int, side: ForgeDirection) = false

  final override def isProvidingStrongPower(world: IBlockAccess, x: Int, y: Int, z: Int, side: Int) =
    isProvidingStrongPower(world, x, y, z, ForgeDirection.getOrientation(side).getOpposite)

  def isProvidingStrongPower(world: IBlockAccess, x: Int, y: Int, z: Int, side: ForgeDirection) =
    isProvidingWeakPower(world, x, y, z, side)

  final override def isProvidingWeakPower(world: IBlockAccess, x: Int, y: Int, z: Int, side: Int) =
    isProvidingWeakPower(world, x, y, z, ForgeDirection.getOrientation(side).getOpposite)

  def isProvidingWeakPower(world: IBlockAccess, x: Int, y: Int, z: Int, side: ForgeDirection) = 0

  // This function can mess things up badly in single player if not
  // synchronized because it sets fields in an instance stored in the
  // static block list... which is used by both server and client thread.
  // The other place where this is locked is in collisionRayTrace below,
  // which seems to be the only built-in function that *logically* depends
  // on the state bounds (rest is rendering which is unimportant).
  final override def setBlockBoundsBasedOnState(world: IBlockAccess, x: Int, y: Int, z: Int) =
    this.synchronized(doSetBlockBoundsBasedOnState(world, x, y, z))

  protected def doSetBlockBoundsBasedOnState(world: IBlockAccess, x: Int, y: Int, z: Int) =
    super.setBlockBoundsBasedOnState(world, x, y, z)

  final override def collisionRayTrace(world: World, x: Int, y: Int, z: Int, origin: Vec3, direction: Vec3) =
    this.synchronized(intersect(world, x, y, z, origin, direction))

  protected def intersect(world: World, x: Int, y: Int, z: Int, origin: Vec3, direction: Vec3) =
    super.collisionRayTrace(world, x, y, z, origin, direction)

  final override def onBlockActivated(world: World, x: Int, y: Int, z: Int, player: EntityPlayer, side: Int, hitX: Float, hitY: Float, hitZ: Float): Boolean =
    onBlockActivated(world, x, y, z, player, ForgeDirection.getOrientation(side), hitX, hitY, hitZ)

  def onBlockActivated(world: World, x: Int, y: Int, z: Int, player: EntityPlayer, side: ForgeDirection, hitX: Float, hitY: Float, hitZ: Float) = false
}
