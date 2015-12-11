package li.cil.oc.common.block

import java.util

import li.cil.oc.client.KeyBindings
import li.cil.oc.common.GuiType
import li.cil.oc.common.block.property.PropertyRotatable
import li.cil.oc.common.item.data.RaidData
import li.cil.oc.common.tileentity
import net.minecraft.block.Block
import net.minecraft.block.state.BlockState
import net.minecraft.block.state.IBlockState
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumFacing
import net.minecraft.world.World

import scala.reflect.ClassTag

class Raid(protected implicit val tileTag: ClassTag[tileentity.Raid]) extends SimpleBlock with traits.GUI with traits.CustomDrops[tileentity.Raid] {
  override def createBlockState(): BlockState = new BlockState(this, PropertyRotatable.Facing)

  override def getStateFromMeta(meta: Int): IBlockState = getDefaultState.withProperty(PropertyRotatable.Facing, EnumFacing.getHorizontal(meta))

  override def getMetaFromState(state: IBlockState): Int = state.getValue(PropertyRotatable.Facing).getHorizontalIndex

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

  override def hasTileEntity(state: IBlockState) = true

  override def createNewTileEntity(world: World, metadata: Int) = new tileentity.Raid()

  // ----------------------------------------------------------------------- //

  override def hasComparatorInputOverride = true

  override def getComparatorInputOverride(world: World, pos: BlockPos) =
    world.getTileEntity(pos) match {
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
    Block.spawnAsEntity(tileEntity.world, tileEntity.getPos, stack)
  }
}
