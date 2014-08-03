package li.cil.oc.common.block

import java.util

import cpw.mods.fml.common.Optional
import cpw.mods.fml.relauncher.{Side, SideOnly}
import li.cil.oc.Settings
import li.cil.oc.common.tileentity
import li.cil.oc.common.tileentity.traits.Inventory
import li.cil.oc.util.Tooltip
import li.cil.oc.util.mods.Mods
import mcp.mobius.waila.api.{IWailaConfigHandler, IWailaDataAccessor}
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.{Entity, EntityLivingBase}
import net.minecraft.item.{EnumRarity, ItemStack}
import net.minecraft.tileentity.TileEntity
import net.minecraft.util._
import net.minecraft.world.{IBlockAccess, World}
import net.minecraftforge.common.ForgeDirection

trait Delegate {
  type Icon = net.minecraft.util.Icon
  type IconRegister = net.minecraft.client.renderer.texture.IconRegister

  def unlocalizedName = getClass.getSimpleName

  var showInItemList = true

  def blockId: Int

  def parent: Delegator[_]

  val icons = new Array[Icon](6)

  protected def customTextures = Array.fill[Option[String]](6)(None)

  def setBlock(world: World, x: Int, y: Int, z: Int, flags: Int) = {
    world.setBlock(x, y, z, parent.blockID, blockId, flags)
  }

  def createItemStack(amount: Int = 1) = new ItemStack(parent, amount, itemDamage)

  // ----------------------------------------------------------------------- //

  def itemDamage = blockId

  def pick(target: MovingObjectPosition, world: World, x: Int, y: Int, z: Int): ItemStack = createItemStack()

  def drops(world: World, x: Int, y: Int, z: Int, fortune: Int): Option[java.util.ArrayList[ItemStack]] = None

  def explosionResistance(entity: Entity): Float = parent.getExplosionResistance(entity)

  def isAir(world: World, x: Int, y: Int, z: Int) = false

  def isNormalCube(world: World, x: Int, y: Int, z: Int) = true

  def validRotations(world: World, x: Int, y: Int, z: Int) = validRotations_

  def canPlaceBlockOnSide(world: World, x: Int, y: Int, z: Int, side: ForgeDirection) = true

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
    if (!world.isRemote) world.getBlockTileEntity(x, y, z) match {
      case inventory: Inventory => inventory.dropAllSlots()
      case _ => // Ignore.
    }

  def removedFromWorld(world: World, x: Int, y: Int, z: Int, blockId: Int) {}

  def removedByEntity(world: World, x: Int, y: Int, z: Int, player: EntityPlayer) = true

  def neighborBlockChanged(world: World, x: Int, y: Int, z: Int, blockId: Int) {}

  def leftClick(world: World, x: Int, y: Int, z: Int, player: EntityPlayer) {}

  def rightClick(world: World, x: Int, y: Int, z: Int, player: EntityPlayer, side: ForgeDirection, hitX: Float, hitY: Float, hitZ: Float) = false

  def walk(world: World, x: Int, y: Int, z: Int, entity: Entity) {}

  def collide(world: World, x: Int, y: Int, z: Int, entity: Entity) {}

  // ----------------------------------------------------------------------- //

  def rarity = EnumRarity.common

  @SideOnly(Side.CLIENT)
  def tooltipLines(stack: ItemStack, player: EntityPlayer, tooltip: java.util.List[String], advanced: Boolean) {
    tooltip.addAll(Tooltip.get(unlocalizedName))
  }

  @Optional.Method(modid = Mods.IDs.Waila)
  def wailaBody(stack: ItemStack, tooltip: util.List[String], accessor: IWailaDataAccessor, config: IWailaConfigHandler) {
  }

  def opacity(world: World, x: Int, y: Int, z: Int) = 255

  def luminance(world: IBlockAccess, x: Int, y: Int, z: Int) = 0

  @SideOnly(Side.CLIENT)
  def mixedBrightness(world: IBlockAccess, x: Int, y: Int, z: Int) = -1

  @SideOnly(Side.CLIENT)
  def color = 0xFFFFFF

  @SideOnly(Side.CLIENT)
  def color(world: IBlockAccess, x: Int, y: Int, z: Int): Int =
    world.getBlockTileEntity(x, y, z) match {
      case colored: tileentity.traits.Colored => colored.color
      case _ => color
    }

  @SideOnly(Side.CLIENT)
  def icon(side: ForgeDirection): Option[Icon] = Some(icons(side.ordinal))

  @SideOnly(Side.CLIENT)
  def icon(world: IBlockAccess, x: Int, y: Int, z: Int, worldSide: ForgeDirection, localSide: ForgeDirection): Option[Icon] = icon(localSide)

  def itemBounds(): Unit = parent.setBlockBoundsForItemRender()

  @SideOnly(Side.CLIENT)
  def preItemRender() {}

  @SideOnly(Side.CLIENT)
  def registerIcons(iconRegister: IconRegister) {
    icons(ForgeDirection.DOWN.ordinal) = iconRegister.registerIcon(Settings.resourceDomain + ":GenericTop")
    icons(ForgeDirection.UP.ordinal) = icons(ForgeDirection.DOWN.ordinal)
    icons(ForgeDirection.NORTH.ordinal) = iconRegister.registerIcon(Settings.resourceDomain + ":GenericSide")
    icons(ForgeDirection.SOUTH.ordinal) = icons(ForgeDirection.NORTH.ordinal)
    icons(ForgeDirection.WEST.ordinal) = icons(ForgeDirection.NORTH.ordinal)
    icons(ForgeDirection.EAST.ordinal) = icons(ForgeDirection.NORTH.ordinal)

    val custom = customTextures
    for (side <- ForgeDirection.VALID_DIRECTIONS) {
      custom(side.ordinal) match {
        case Some(name) => icons(side.ordinal) = iconRegister.registerIcon(Settings.resourceDomain + ":" + name)
        case _ =>
      }
    }
  }

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
    !world.isBlockOpaqueCube(x, y, z)
}
