package li.cil.oc.common.block.traits

import java.util

import cpw.mods.fml.common.Optional
import li.cil.oc.common.block.SimpleBlock
import li.cil.oc.common.tileentity.traits.power.ResonantEngine
import li.cil.oc.integration.Mods
import li.cil.oc.util.Tooltip
import net.minecraft.block.Block
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.tileentity.TileEntity
import net.minecraft.world.World
import net.minecraftforge.common.util.ForgeDirection
import resonant.api.electric.IEnergyNode

trait PowerAcceptor extends SimpleBlock {
  def energyThroughput: Double

  override def onNeighborBlockChange(world: World, x: Int, y: Int, z: Int, block: Block) {
    super.onNeighborBlockChange(world, x, y, z, block)
    if (Mods.ResonantEngine.isAvailable) {
      updateRENode(world.getTileEntity(x, y, z))
    }
  }

  @Optional.Method(modid = Mods.IDs.ResonantEngine)
  private def updateRENode(tileEntity: TileEntity) {
    tileEntity match {
      case re: ResonantEngine => re.getNode(classOf[IEnergyNode], ForgeDirection.UNKNOWN).reconstruct()
      case _ =>
    }
  }

  // ----------------------------------------------------------------------- //

  override protected def tooltipTail(metadata: Int, stack: ItemStack, player: EntityPlayer, tooltip: util.List[String], advanced: Boolean) {
    super.tooltipTail(metadata, stack, player, tooltip, advanced)
    tooltip.addAll(Tooltip.extended("PowerAcceptor", energyThroughput.toInt))
  }
}
