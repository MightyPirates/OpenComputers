package li.cil.oc.common.block

import java.util

import li.cil.oc.client.KeyBindings
import li.cil.oc.common.GuiType
import li.cil.oc.common.item.data.RaidData
import li.cil.oc.common.tileentity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.world.World

import scala.reflect.ClassTag

class Raid(protected implicit val tileTag: ClassTag[tileentity.Raid]) extends SimpleBlock with traits.GUI with traits.CustomDrops[tileentity.Raid] {
  override protected def customTextures = Array(
    None,
    None,
    Some("RaidSide"),
    Some("RaidFront"),
    Some("RaidSide"),
    Some("RaidSide")
  )

  // ----------------------------------------------------------------------- //

  override protected def tooltipTail(metadata: Int, stack: ItemStack, player: EntityPlayer, tooltip: util.List[String], advanced: Boolean) {
    super.tooltipTail(metadata, stack, player, tooltip, advanced)
    if (KeyBindings.showExtendedTooltips) {
      val data = new RaidData(stack)
      for (disk <- data.disks if disk != null) {
        tooltip.add("- " + disk.getDisplayName)
      }
    }
  }

  // ----------------------------------------------------------------------- //

  override def guiType = GuiType.Raid

  override def hasTileEntity(metadata: Int) = true

  override def createTileEntity(world: World, metadata: Int) = new tileentity.Raid()

  // ----------------------------------------------------------------------- //

  override def hasComparatorInputOverride = true

  override def getComparatorInputOverride(world: World, x: Int, y: Int, z: Int, side: Int) =
    world.getTileEntity(x, y, z) match {
      case raid: tileentity.Raid if raid.presence.forall(ok => ok) => 15
      case _ => 0
    }

  override protected def doCustomInit(tileEntity: tileentity.Raid, player: EntityLivingBase, stack: ItemStack): Unit = {
    super.doCustomInit(tileEntity, player, stack)
    if (!tileEntity.world.isRemote) {
      val data = new RaidData(stack)
      for (i <- 0 until math.min(data.disks.length, tileEntity.getSizeInventory)) {
        tileEntity.setInventorySlotContents(i, data.disks(i))
      }
      data.label.foreach(tileEntity.label.setLabel)
      if (!data.filesystem.hasNoTags) {
        tileEntity.tryCreateRaid(data.filesystem.getCompoundTag("node").getString("address"))
        tileEntity.filesystem.foreach(_.load(data.filesystem))
      }
    }
  }

  override protected def doCustomDrops(tileEntity: tileentity.Raid, player: EntityPlayer, willHarvest: Boolean): Unit = {
    super.doCustomDrops(tileEntity, player, willHarvest)
    val stack = createItemStack()
    if (tileEntity.items.exists(_.isDefined)) {
      val data = new RaidData()
      data.disks = tileEntity.items.map(_.orNull)
      tileEntity.filesystem.foreach(_.save(data.filesystem))
      data.label = Option(tileEntity.label.getLabel)
      data.save(stack)
    }
    dropBlockAsItem(tileEntity.world, tileEntity.x, tileEntity.y, tileEntity.z, stack)
  }
}
