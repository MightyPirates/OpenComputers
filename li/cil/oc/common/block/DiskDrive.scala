package li.cil.oc.common.block

import cpw.mods.fml.common.registry.GameRegistry
import li.cil.oc.common.{GuiType, tileentity}
import li.cil.oc.{OpenComputers, Config}
import net.minecraft.client.renderer.texture.IconRegister
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.Icon
import net.minecraft.world.World
import net.minecraftforge.common.ForgeDirection

class DiskDrive(val parent: SimpleDelegator) extends SimpleDelegate {
  GameRegistry.registerTileEntity(classOf[tileentity.DiskDrive], "oc.disk_drive")

  val unlocalizedName = "DiskDrive"

  // ----------------------------------------------------------------------- //

  private val icons = Array.fill[Icon](6)(null)

  override def icon(side: ForgeDirection) = Some(icons(side.ordinal))

  override def registerIcons(iconRegister: IconRegister) = {
    icons(ForgeDirection.DOWN.ordinal) = iconRegister.registerIcon(Config.resourceDomain + ":computer_top")
    icons(ForgeDirection.UP.ordinal) = icons(ForgeDirection.DOWN.ordinal)

    icons(ForgeDirection.NORTH.ordinal) = iconRegister.registerIcon(Config.resourceDomain + ":disk_drive_side")
    icons(ForgeDirection.SOUTH.ordinal) = iconRegister.registerIcon(Config.resourceDomain + ":disk_drive_front")
    icons(ForgeDirection.WEST.ordinal) = icons(ForgeDirection.NORTH.ordinal)
    icons(ForgeDirection.EAST.ordinal) = icons(ForgeDirection.NORTH.ordinal)
  }

  // ----------------------------------------------------------------------- //

  override def hasTileEntity = true

  override def createTileEntity(world: World, metadata: Int) = Some(new tileentity.DiskDrive)

  // ----------------------------------------------------------------------- //

  override def breakBlock(world: World, x: Int, y: Int, z: Int, blockId: Int, metadata: Int) = {
    if (!world.isRemote) world.getBlockTileEntity(x, y, z) match {
      case drive: tileentity.DiskDrive => drive.dropContent(world, x, y, z)
      case _ => // Ignore.
    }
    super.breakBlock(world, x, y, z, blockId, metadata)
  }

  override def onBlockActivated(world: World, x: Int, y: Int, z: Int, player: EntityPlayer,
                                side: ForgeDirection, hitX: Float, hitY: Float, hitZ: Float) = {
    if (!player.isSneaking) {
      player.openGui(OpenComputers, GuiType.DiskDrive.id, world, x, y, z)
      true
    }
    else false
  }
}
