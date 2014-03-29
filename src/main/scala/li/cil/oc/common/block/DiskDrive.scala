package li.cil.oc.common.block

import cpw.mods.fml.common.Optional
import java.util
import li.cil.oc.common.{GuiType, tileentity}
import li.cil.oc.util.mods.Mods
import li.cil.oc.util.Tooltip
import li.cil.oc.{OpenComputers, Settings}
import net.minecraft.client.renderer.texture.IIconRegister
import mcp.mobius.waila.api.{IWailaConfigHandler, IWailaDataAccessor}
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.util.IIcon
import net.minecraft.util.StatCollector
import net.minecraft.world.World
import net.minecraftforge.common.util.ForgeDirection
import net.minecraftforge.common.util.Constants.NBT

class DiskDrive(val parent: SimpleDelegator) extends SimpleDelegate {
  val unlocalizedName = "DiskDrive"

  private val icons = Array.fill[IIcon](6)(null)

  // ----------------------------------------------------------------------- //

  override def tooltipLines(stack: ItemStack, player: EntityPlayer, tooltip: util.List[String], advanced: Boolean) {
    tooltip.addAll(Tooltip.get(unlocalizedName))
    if (Mods.ComputerCraft.isAvailable) {
      tooltip.addAll(Tooltip.get(unlocalizedName + ".CC"))
    }
  }

  @Optional.Method(modid = "Waila")
  override def wailaBody(stack: ItemStack, tooltip: util.List[String], accessor: IWailaDataAccessor, config: IWailaConfigHandler) {
    val items = accessor.getNBTData.getTagList(Settings.namespace + "items", NBT.TAG_COMPOUND)
    if (items.tagCount > 0) {
      val node = items.getCompoundTagAt(0).
        getCompoundTag("item").
        getCompoundTag("tag").
        getCompoundTag(Settings.namespace + "data").
        getCompoundTag("node")
      if (node.hasKey("address")) {
        tooltip.add(StatCollector.translateToLocalFormatted(
          Settings.namespace + "gui.Analyzer.Address", node.getString("address")))
      }
    }
  }

  override def icon(side: ForgeDirection) = Some(icons(side.ordinal))

  override def registerIcons(iconRegister: IIconRegister) = {
    icons(ForgeDirection.DOWN.ordinal) = iconRegister.registerIcon(Settings.resourceDomain + ":generic_top")
    icons(ForgeDirection.UP.ordinal) = icons(ForgeDirection.DOWN.ordinal)

    icons(ForgeDirection.NORTH.ordinal) = iconRegister.registerIcon(Settings.resourceDomain + ":disk_drive_side")
    icons(ForgeDirection.SOUTH.ordinal) = iconRegister.registerIcon(Settings.resourceDomain + ":disk_drive_front")
    icons(ForgeDirection.WEST.ordinal) = icons(ForgeDirection.NORTH.ordinal)
    icons(ForgeDirection.EAST.ordinal) = icons(ForgeDirection.NORTH.ordinal)
  }

  // ----------------------------------------------------------------------- //

  override def hasTileEntity = true

  override def createTileEntity(world: World) = Some(new tileentity.DiskDrive)

  // ----------------------------------------------------------------------- //

  override def rightClick(world: World, x: Int, y: Int, z: Int, player: EntityPlayer,
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
