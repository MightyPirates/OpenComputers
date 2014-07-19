package li.cil.oc.common.block

import java.util

import cpw.mods.fml.common.Optional
import li.cil.oc.client.Textures
import li.cil.oc.common.tileentity
import li.cil.oc.server.PacketSender
import li.cil.oc.util.mods.{BuildCraft, Mods}
import li.cil.oc.{Localization, Settings}
import mcp.mobius.waila.api.{IWailaConfigHandler, IWailaDataAccessor}
import net.minecraft.block.Block
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.world.{IBlockAccess, World}
import net.minecraftforge.common.util.ForgeDirection

class Charger(val parent: SimpleDelegator) extends RedstoneAware with SimpleDelegate {
  override protected def customTextures = Array(
    None,
    None,
    Some("ChargerSide"),
    Some("ChargerFront"),
    Some("ChargerSide"),
    Some("ChargerSide")
  )

  @Optional.Method(modid = Mods.IDs.Waila)
  override def wailaBody(stack: ItemStack, tooltip: util.List[String], accessor: IWailaDataAccessor, config: IWailaConfigHandler) {
    accessor.getTileEntity match {
      case charger: tileentity.Charger =>
        tooltip.add(Localization.Analyzer.ChargerSpeed(charger.chargeSpeed).getUnformattedTextForChat)
      case _ =>
    }
  }

  override def registerIcons(iconRegister: IconRegister) = {
    super.registerIcons(iconRegister)
    Textures.Charger.iconFrontCharging = iconRegister.registerIcon(Settings.resourceDomain + ":ChargerFrontOn")
    Textures.Charger.iconSideCharging = iconRegister.registerIcon(Settings.resourceDomain + ":ChargerSideOn")
  }

  override def createTileEntity(world: World) = Some(new tileentity.Charger())

  override def canConnectToRedstone(world: IBlockAccess, x: Int, y: Int, z: Int, side: ForgeDirection) = true

  override def rightClick(world: World, x: Int, y: Int, z: Int, player: EntityPlayer, side: ForgeDirection, hitX: Float, hitY: Float, hitZ: Float) =
    world.getTileEntity(x, y, z) match {
      case charger: tileentity.Charger if BuildCraft.holdsApplicableWrench(player, x, y, z) =>
        if (!world.isRemote) {
          charger.invertSignal = !charger.invertSignal
          charger.chargeSpeed = 1.0 - charger.chargeSpeed
          PacketSender.sendChargerState(charger)
          BuildCraft.wrenchUsed(player, x, y, z)
        }
        true
      case _ => super.rightClick(world, x, y, z, player, side, hitX, hitY, hitZ)
    }

  override def neighborBlockChanged(world: World, x: Int, y: Int, z: Int, block: Block) {
    world.getTileEntity(x, y, z) match {
      case charger: tileentity.Charger => charger.onNeighborChanged()
      case _ =>
    }
    super.neighborBlockChanged(world, x, y, z, block)
  }
}
