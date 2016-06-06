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
import net.minecraft.util.IIcon
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

  override def onBlockActivated(world: World, x: Int, y: Int, z: Int, player: EntityPlayer, side: ForgeDirection, hitX: Float, hitY: Float, hitZ: Float): Boolean = {
    world.getTileEntity(x, y, z) match {
      case rack: tileentity.Rack => rack.slotAt(side, hitX, hitY, hitZ) match {
        case Some(slot) =>
          val hitVec = Vec3.createVectorHelper(hitX, hitY, hitZ)
          val rotation = side match {
            case ForgeDirection.WEST => Math.toRadians(90).toFloat
            case ForgeDirection.NORTH => Math.toRadians(180).toFloat
            case ForgeDirection.EAST => Math.toRadians(270).toFloat
            case _ => 0
          }
          val localHitVec = rotate(hitVec.addVector(-0.5, -0.5, -0.5), rotation).addVector(0.5, 0.5, 0.5)
          localHitVec.xCoord = ((if (side.offsetX != 0) 1 - localHitVec.xCoord else localHitVec.xCoord) * 16 - 1) / 14f
          localHitVec.yCoord = ((1 - localHitVec.yCoord) * 16 - 2 - 3 * slot) / 3f
          rack.getMountable(slot) match {
            case mountable: RackMountable if mountable.onActivate(player, localHitVec.xCoord.toFloat, localHitVec.yCoord.toFloat) => return true // Activation handled by mountable.
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
