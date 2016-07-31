package li.cil.oc.common.block

import cpw.mods.fml.relauncher.Side
import cpw.mods.fml.relauncher.SideOnly
import li.cil.oc.Settings
import li.cil.oc.api.component.RackMountable
import li.cil.oc.client.Textures
import li.cil.oc.common.GuiType
import li.cil.oc.common.tileentity
import net.minecraft.client.renderer.texture.IIconRegister
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.AxisAlignedBB
import net.minecraft.util.IIcon
import net.minecraft.util.MovingObjectPosition
import net.minecraft.util.Vec3
import net.minecraft.world.IBlockAccess
import net.minecraft.world.World
import net.minecraftforge.common.util.ForgeDirection

class Rack extends RedstoneAware with traits.SpecialBlock with traits.PowerAcceptor with traits.StateAware with traits.GUI {
  override protected def customTextures = Array(
    None,
    None,
    Some("RackSide"),
    Some("RackFront"),
    Some("RackSide"),
    Some("RackSide")
  )

  var frontOverride: IIcon = _

  override def registerBlockIcons(iconRegister: IIconRegister) = {
    super.registerBlockIcons(iconRegister)
    System.arraycopy(icons, 0, Textures.Rack.icons, 0, icons.length)
    Textures.Rack.diskDrive = iconRegister.registerIcon(Settings.resourceDomain + ":" + "DiskDriveMountable")
    Textures.Rack.server = iconRegister.registerIcon(Settings.resourceDomain + ":" + "ServerFront")
    Textures.Rack.terminal = iconRegister.registerIcon(Settings.resourceDomain + ":" + "TerminalServerFront")
  }

  @SideOnly(Side.CLIENT)
  override def getIcon(world: IBlockAccess, x: Int, y: Int, z: Int, globalSide: ForgeDirection, localSide: ForgeDirection): IIcon = {
    if (localSide == ForgeDirection.SOUTH && frontOverride != null) frontOverride
    else super.getIcon(world, x, y, z, globalSide, localSide)
  }

  @SideOnly(Side.CLIENT)
  override def getMixedBrightnessForBlock(world: IBlockAccess, x: Int, y: Int, z: Int) = {
    if (y >= 0 && y < world.getHeight) world.getTileEntity(x, y, z) match {
      case rack: tileentity.Rack =>
        def brightness(x: Int, y: Int, z: Int) = world.getLightBrightnessForSkyBlocks(x, y, z, world.getBlock(x, y, z).getLightValue(world, x, y, z))
        val value = brightness(x + rack.facing.offsetX, y + rack.facing.offsetY, z + rack.facing.offsetZ)
        val skyBrightness = (value >> 20) & 15
        val blockBrightness = (value >> 4) & 15
        ((skyBrightness * 3 / 4) << 20) | ((blockBrightness * 3 / 4) << 4)
      case _ => super.getMixedBrightnessForBlock(world, x, y, z)
    }
    else super.getMixedBrightnessForBlock(world, x, y, z)
  }

  override def isBlockSolid(world: IBlockAccess, x: Int, y: Int, z: Int, side: ForgeDirection) = side != ForgeDirection.SOUTH

  override def isSideSolid(world: IBlockAccess, x: Int, y: Int, z: Int, side: ForgeDirection) = toLocal(world, x, y, z, side) != ForgeDirection.SOUTH

  // ----------------------------------------------------------------------- //

  override def energyThroughput = Settings.get.serverRackRate

  override def guiType = GuiType.Rack

  override def hasTileEntity(metadata: Int) = true

  override def createTileEntity(world: World, metadata: Int) = new tileentity.Rack()

  // ----------------------------------------------------------------------- //

  final val collisionBounds = Array(
    AxisAlignedBB.getBoundingBox(0, 0, 0, 1, 1/16f, 1),
    AxisAlignedBB.getBoundingBox(0, 15/16f, 0, 1, 1, 1),
    AxisAlignedBB.getBoundingBox(0, 0, 0, 1, 1, 1/16f),
    AxisAlignedBB.getBoundingBox(0, 0, 15/16f, 1, 1, 1),
    AxisAlignedBB.getBoundingBox(0, 0, 0, 1/16f, 1, 1),
    AxisAlignedBB.getBoundingBox(15/16f, 0, 0, 1, 1, 1),
    AxisAlignedBB.getBoundingBox(1/16f, 1/16f, 1/16f, 15/16f, 15/16f, 15/16f)
  )

  override protected def intersect(world: World, x: Int, y: Int, z: Int, start: Vec3, end: Vec3): MovingObjectPosition = {
    world.getTileEntity(x, y, z) match {
      case rack: tileentity.Rack =>
        var closestDistance = Double.PositiveInfinity
        var closest: Option[MovingObjectPosition] = None

        def intersect(bounds: AxisAlignedBB): Unit = {
          val hit = bounds.copy().offset(x, y, z).calculateIntercept(start, end)
          if (hit != null) {
            val distance = hit.hitVec.distanceTo(start)
            if (distance < closestDistance) {
              closestDistance = distance
              closest = Option(hit)
            }
          }
        }
        val facings = ForgeDirection.VALID_DIRECTIONS
        for (i <- 0 until facings.length) {
          if (rack.facing != facings(i)) {
            intersect(collisionBounds(i))
          }
        }
        intersect(collisionBounds.last)
        closest.map(hit => new MovingObjectPosition(x, y, z, hit.sideHit, hit.hitVec)).orNull
      case _ => super.intersect(world, x, y, z, start, end)
    }
  }

  override def onBlockActivated(world: World, x: Int, y: Int, z: Int, player: EntityPlayer, side: ForgeDirection, hitX: Float, hitY: Float, hitZ: Float): Boolean = {
    world.getTileEntity(x, y, z) match {
      case rack: tileentity.Rack => rack.slotAt(side, hitX, hitY, hitZ) match {
        case Some(slot) =>
          // Snap to grid to get same behavior on client and server...
          val hitVec = Vec3.createVectorHelper((hitX*16f).toInt/16f, (hitY*16f).toInt/16f, (hitZ*16f).toInt/16f)
          val rotation = side match {
            case ForgeDirection.WEST => Math.toRadians(90).toFloat
            case ForgeDirection.NORTH => Math.toRadians(180).toFloat
            case ForgeDirection.EAST => Math.toRadians(270).toFloat
            case _ => 0
          }
          // Rotate *centers* of pixels to keep association when reversing axis.
          val localHitVec = rotate(hitVec.addVector(-0.5+1/32f, -0.5+1/32f, -0.5+1/32f), rotation).addVector(0.5-1/32f, 0.5-1/32f, 0.5-1/32f)
          val globalX = (localHitVec.xCoord * 16.05f).toInt // [0, 15], work around floating point inaccuracies
          val globalY = (localHitVec.yCoord * 16.05f).toInt // [0, 15], work around floating point inaccuracies
          val localX = (if (side.offsetX != 0) 15 - globalX else globalX) - 1
          val localY = (15 - globalY) - 2 - 3 * slot
          if (localX >= 0 && localX < 14 && localY >= 0 && localY < 3) rack.getMountable(slot) match {
            case mountable: RackMountable if mountable.onActivate(player, localX / 14f, localY / 3f) => return true // Activation handled by mountable.
            case _ =>
          }
        case _ =>
      }
      case _ =>
    }
    super.onBlockActivated(world, x, y, z, player, side, hitX, hitY, hitZ)
  }

  def rotate(v: Vec3, t: Float): Vec3 = {
    val cos = Math.cos(t)
    val sin = Math.sin(t)
    Vec3.createVectorHelper(v.xCoord * cos - v.zCoord * sin, v.yCoord, v.xCoord * sin + v.zCoord * cos)
  }
}
