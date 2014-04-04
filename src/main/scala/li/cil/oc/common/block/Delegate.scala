package li.cil.oc.common.block

import cpw.mods.fml.common.Optional
import cpw.mods.fml.relauncher.{SideOnly, Side}
import java.util
import li.cil.oc.common.tileentity.traits.{Colored, Inventory}
import mcp.mobius.waila.api.{IWailaConfigHandler, IWailaDataAccessor}
import net.minecraft.block.Block
import net.minecraft.client.renderer.texture.IIconRegister
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.{Entity, EntityLivingBase}
import net.minecraft.item.{EnumRarity, ItemStack}
import net.minecraft.tileentity.TileEntity
import net.minecraft.util._
import net.minecraft.world.IBlockAccess
import net.minecraft.world.World
import net.minecraftforge.common.util.ForgeDirection

trait Delegate {
  val unlocalizedName: String

  var showInItemList = true

  def blockId: Int

  def parent: Delegator[_]

  def setBlock(world: World, x: Int, y: Int, z: Int, flags: Int) = {
    world.setBlock(x, y, z, parent, blockId, flags)
  }

  def createItemStack(amount: Int = 1) = new ItemStack(parent, amount, itemDamage)

  // ----------------------------------------------------------------------- //

  def itemDamage = blockId

  def pick(target: MovingObjectPosition, world: World, x: Int, y: Int, z: Int): ItemStack = createItemStack()

  def drops(world: World, x: Int, y: Int, z: Int, fortune: Int): Option[java.util.ArrayList[ItemStack]] = None

  def explosionResistance(entity: Entity): Float = parent.getExplosionResistance(entity)

  def isNormalCube(world: IBlockAccess, x: Int, y: Int, z: Int) = true

  def validRotations(world: World, x: Int, y: Int, z: Int) = validRotations_

  // DEPRECATED Seems this isn't available anymore with stack info, use real
  // items in 1.7 when needed since IDs are no problem anymore.
  //  def canPlaceBlockOnSide(world: World, x: Int, y: Int, z: Int, side: ForgeDirection) = true

  def bounds(world: IBlockAccess, x: Int, y: Int, z: Int) =
    AxisAlignedBB.getAABBPool.getAABB(0, 0, 0, 1, 1, 1)

  def updateBounds(world: IBlockAccess, x: Int, y: Int, z: Int) =
    parent.setBlockBounds(bounds(world, x, y, z))

  def intersect(world: World, x: Int, y: Int, z: Int, origin: Vec3, direction: Vec3) =
    parent.superCollisionRayTrace(world, x, y, z, origin, direction)

  // ----------------------------------------------------------------------- //

  def canConnectToRedstone(world: IBlockAccess, x: Int, y: Int, z: Int, side: ForgeDirection) = false

  def isProvidingStrongPower(world: IBlockAccess, x: Int, y: Int, z: Int, side: ForgeDirection) = 0

  def isProvidingWeakPower(world: IBlockAccess, x: Int, y: Int, z: Int, side: ForgeDirection) = 0

  // ----------------------------------------------------------------------- //

  def hasTileEntity = false

  def createTileEntity(world: World): Option[TileEntity] = None

  // ----------------------------------------------------------------------- //

  def update(world: World, x: Int, y: Int, z: Int) = {}

  def addedToWorld(world: World, x: Int, y: Int, z: Int) {}

  def addedByEntity(world: World, x: Int, y: Int, z: Int, player: EntityLivingBase, stack: ItemStack) {}

  def aboutToBeRemoved(world: World, x: Int, y: Int, z: Int) =
    if (!world.isRemote) world.getTileEntity(x, y, z) match {
      case inventory: Inventory => inventory.dropAllSlots()
      case _ => // Ignore.
    }

  def removedFromWorld(world: World, x: Int, y: Int, z: Int, block: Block) {}

  def removedByEntity(world: World, x: Int, y: Int, z: Int, player: EntityPlayer) = true

  def neighborBlockChanged(world: World, x: Int, y: Int, z: Int, block: Block) {}

  def leftClick(world: World, x: Int, y: Int, z: Int, player: EntityPlayer) {}

  def rightClick(world: World, x: Int, y: Int, z: Int, player: EntityPlayer, side: ForgeDirection, hitX: Float, hitY: Float, hitZ: Float) = false

  def walk(world: World, x: Int, y: Int, z: Int, entity: Entity) {}

  def collide(world: World, x: Int, y: Int, z: Int, entity: Entity) {}

  // ----------------------------------------------------------------------- //

  def rarity = EnumRarity.common

  @SideOnly(Side.CLIENT)
  def tooltipLines(stack: ItemStack, player: EntityPlayer, tooltip: java.util.List[String], advanced: Boolean) {}

  @Optional.Method(modid = "Waila")
  def wailaBody(stack: ItemStack, tooltip: util.List[String], accessor: IWailaDataAccessor, config: IWailaConfigHandler) {
  }

  def opacity(world: IBlockAccess, x: Int, y: Int, z: Int) = 255

  def luminance(world: IBlockAccess, x: Int, y: Int, z: Int) = 0

  @SideOnly(Side.CLIENT)
  def mixedBrightness(world: IBlockAccess, x: Int, y: Int, z: Int) = -1

  @SideOnly(Side.CLIENT)
  def color = 0xFFFFFF

  @SideOnly(Side.CLIENT)
  def color(world: IBlockAccess, x: Int, y: Int, z: Int): Int =
    world.getTileEntity(x, y, z) match {
      case colored: Colored => colored.color
      case _ => color
    }

  @SideOnly(Side.CLIENT)
  def icon(side: ForgeDirection): Option[IIcon] = None

  @SideOnly(Side.CLIENT)
  def icon(world: IBlockAccess, x: Int, y: Int, z: Int, worldSide: ForgeDirection, localSide: ForgeDirection): Option[IIcon] = icon(localSide)

  def itemBounds(): Unit = parent.setBlockBoundsForItemRender()

  @SideOnly(Side.CLIENT)
  def preItemRender() {}

  @SideOnly(Side.CLIENT)
  def registerIcons(iconRegister: IIconRegister) {}

  // ----------------------------------------------------------------------- //

  protected val validRotations_ = Array(ForgeDirection.UP, ForgeDirection.DOWN)
}

trait SimpleDelegate extends Delegate {
  val parent: SimpleDelegator

  val blockId = parent.add(this)
}

trait SpecialDelegate extends Delegate {
  val parent: SpecialDelegator

  val blockId = parent.add(this)

  // ----------------------------------------------------------------------- //

  def isSolid(world: IBlockAccess, x: Int, y: Int, z: Int, side: ForgeDirection) = true

  @SideOnly(Side.CLIENT)
  def shouldSideBeRendered(world: IBlockAccess, x: Int, y: Int, z: Int, side: ForgeDirection) =
    !world.isSideSolid(x, y, z, side, true)
}
