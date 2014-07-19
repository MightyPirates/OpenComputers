package li.cil.oc.common.block

import java.util

import cpw.mods.fml.common.Optional
import li.cil.oc.common.{GuiType, tileentity}
import li.cil.oc.util.Tooltip
import li.cil.oc.util.mods.Mods
import li.cil.oc.{Localization, OpenComputers, Settings}
import mcp.mobius.waila.api.{IWailaConfigHandler, IWailaDataAccessor}
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.world.World
import net.minecraftforge.common.util.Constants.NBT
import net.minecraftforge.common.util.ForgeDirection

class DiskDrive(val parent: SimpleDelegator) extends SimpleDelegate {
  override protected def customTextures = Array(
    None,
    None,
    Some("DiskDriveSide"),
    Some("DiskDriveFront"),
    Some("DiskDriveSide"),
    Some("DiskDriveSide")
  )

  override def tooltipLines(stack: ItemStack, player: EntityPlayer, tooltip: util.List[String], advanced: Boolean) {
    super.tooltipLines(stack, player, tooltip, advanced)
    if (Mods.ComputerCraft.isAvailable) {
      tooltip.addAll(Tooltip.get(unlocalizedName + ".CC"))
    }
  }

  @Optional.Method(modid = Mods.IDs.Waila)
  override def wailaBody(stack: ItemStack, tooltip: util.List[String], accessor: IWailaDataAccessor, config: IWailaConfigHandler) {
    val items = accessor.getNBTData.getTagList(Settings.namespace + "items", NBT.TAG_COMPOUND)
    if (items.tagCount > 0) {
      val node = items.getCompoundTagAt(0).
        getCompoundTag("item").
        getCompoundTag("tag").
        getCompoundTag(Settings.namespace + "data").
        getCompoundTag("node")
      if (node.hasKey("address")) {
        tooltip.add(Localization.Analyzer.Address(node.getString("address")).getUnformattedTextForChat)
      }
    }
  }

  // ----------------------------------------------------------------------- //

  override def hasTileEntity = true

  override def createTileEntity(world: World) = Some(new tileentity.DiskDrive())

  // ----------------------------------------------------------------------- //

  override def rightClick(world: World, x: Int, y: Int, z: Int, player: EntityPlayer,
                          side: ForgeDirection, hitX: Float, hitY: Float, hitZ: Float) = {
    world.getTileEntity(x, y, z) match {
      case drive: tileentity.DiskDrive =>
        // Behavior: sneaking -> Insert[+Eject], not sneaking -> GUI.
        if (!player.isSneaking) {
          if (!world.isRemote) {
            player.openGui(OpenComputers, GuiType.DiskDrive.id, world, x, y, z)
          }
          true
        }
        else {
          val isDiskInDrive = drive.getStackInSlot(0) != null
          val isHoldingDisk = drive.isItemValidForSlot(0, player.getCurrentEquippedItem)
          if (isDiskInDrive) {
            if (!world.isRemote) {
              drive.dropSlot(0, 1, drive.facing)
            }
          }
          if (isHoldingDisk) {
            // Insert the disk.
            drive.setInventorySlotContents(0, player.inventory.decrStackSize(player.inventory.currentItem, 1))
          }
          isDiskInDrive || isHoldingDisk
        }
      case _ => false
    }
  }
}
