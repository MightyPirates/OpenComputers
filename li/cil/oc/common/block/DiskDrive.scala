package li.cil.oc.common.block

import cpw.mods.fml.common.Loader
import java.util
import li.cil.oc.common.{GuiType, tileentity}
import li.cil.oc.util.Tooltip
import li.cil.oc.{api, OpenComputers, Settings}
import net.minecraft.client.renderer.texture.IconRegister
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.Icon
import net.minecraft.world.World
import net.minecraftforge.common.ForgeDirection

class DiskDrive(val parent: SimpleDelegator) extends SimpleDelegate {
  val unlocalizedName = "DiskDrive"

  private val icons = Array.fill[Icon](6)(null)

  // ----------------------------------------------------------------------- //

  override def addInformation(player: EntityPlayer, tooltip: util.List[String], advanced: Boolean) {
    tooltip.addAll(Tooltip.get(unlocalizedName))
    if (Loader.isModLoaded("ComputerCraft")) {
      tooltip.addAll(Tooltip.get(unlocalizedName + ".CC"))
    }
  }

  override def icon(side: ForgeDirection) = Some(icons(side.ordinal))

  override def registerIcons(iconRegister: IconRegister) = {
    icons(ForgeDirection.DOWN.ordinal) = iconRegister.registerIcon(Settings.resourceDomain + ":case_top")
    icons(ForgeDirection.UP.ordinal) = icons(ForgeDirection.DOWN.ordinal)

    icons(ForgeDirection.NORTH.ordinal) = iconRegister.registerIcon(Settings.resourceDomain + ":disk_drive_side")
    icons(ForgeDirection.SOUTH.ordinal) = iconRegister.registerIcon(Settings.resourceDomain + ":disk_drive_front")
    icons(ForgeDirection.WEST.ordinal) = icons(ForgeDirection.NORTH.ordinal)
    icons(ForgeDirection.EAST.ordinal) = icons(ForgeDirection.NORTH.ordinal)
  }

  // ----------------------------------------------------------------------- //

  override def hasTileEntity = true

  override def createTileEntity(world: World) = Some(new tileentity.DiskDrive)

  override def update(world: World, x: Int, y: Int, z: Int) =
    world.getBlockTileEntity(x, y, z) match {
      case drive: tileentity.DiskDrive => api.Network.joinOrCreateNetwork(drive)
      case _ =>
    }

  // ----------------------------------------------------------------------- //

  override def onBlockActivated(world: World, x: Int, y: Int, z: Int, player: EntityPlayer,
                                side: ForgeDirection, hitX: Float, hitY: Float, hitZ: Float) = {
    if (!player.isSneaking) {
      if (!world.isRemote) {
        player.openGui(OpenComputers, GuiType.DiskDrive.id, world, x, y, z)
      }
      true
    }
    else false
  }
}
