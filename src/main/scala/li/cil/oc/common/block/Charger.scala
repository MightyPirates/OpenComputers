package li.cil.oc.common.block

import java.util

import cpw.mods.fml.common.Optional
import cpw.mods.fml.relauncher.{Side, SideOnly}
import li.cil.oc.client.Textures
import li.cil.oc.common.tileentity
import li.cil.oc.server.PacketSender
import li.cil.oc.util.Tooltip
import li.cil.oc.util.mods.BuildCraft
import li.cil.oc.{Localization, Settings}
import mcp.mobius.waila.api.{IWailaConfigHandler, IWailaDataAccessor}
import net.minecraft.client.renderer.texture.IconRegister
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.util.Icon
import net.minecraft.world.{IBlockAccess, World}
import net.minecraftforge.common.ForgeDirection

class Charger(val parent: SimpleDelegator) extends RedstoneAware with SimpleDelegate {
  val unlocalizedName = "Charger"

  private val icons = Array.fill[Icon](6)(null)

  override def tooltipLines(stack: ItemStack, player: EntityPlayer, tooltip: util.List[String], advanced: Boolean) {
    tooltip.addAll(Tooltip.get(unlocalizedName))
  }

  @Optional.Method(modid = "Waila")
  override def wailaBody(stack: ItemStack, tooltip: util.List[String], accessor: IWailaDataAccessor, config: IWailaConfigHandler) {
    accessor.getTileEntity match {
      case charger: tileentity.Charger =>
        tooltip.add(Localization.Analyzer.ChargerSpeed(charger.chargeSpeed).toString)
      case _ =>
    }
  }

  override def icon(side: ForgeDirection) = Some(icons(side.ordinal()))

  @SideOnly(Side.CLIENT)
  override def icon(world: IBlockAccess, x: Int, y: Int, z: Int, worldSide: ForgeDirection, localSide: ForgeDirection) = Some(icons(localSide.ordinal()))

  override def registerIcons(iconRegister: IconRegister) = {
    icons(ForgeDirection.DOWN.ordinal) = iconRegister.registerIcon(Settings.resourceDomain + ":generic_top")
    icons(ForgeDirection.UP.ordinal) = icons(ForgeDirection.DOWN.ordinal)

    icons(ForgeDirection.NORTH.ordinal) = iconRegister.registerIcon(Settings.resourceDomain + ":charger_side")
    icons(ForgeDirection.SOUTH.ordinal) = iconRegister.registerIcon(Settings.resourceDomain + ":charger_front")
    icons(ForgeDirection.WEST.ordinal) = icons(ForgeDirection.NORTH.ordinal)
    icons(ForgeDirection.EAST.ordinal) = icons(ForgeDirection.NORTH.ordinal)

    Textures.Charger.iconFrontCharging = iconRegister.registerIcon(Settings.resourceDomain + ":charger_front_on")
    Textures.Charger.iconSideCharging = iconRegister.registerIcon(Settings.resourceDomain + ":charger_side_on")
  }

  override def createTileEntity(world: World) = Some(new tileentity.Charger(world.isRemote))

  override def canConnectToRedstone(world: IBlockAccess, x: Int, y: Int, z: Int, side: ForgeDirection) = true

  override def rightClick(world: World, x: Int, y: Int, z: Int, player: EntityPlayer, side: ForgeDirection, hitX: Float, hitY: Float, hitZ: Float) =
    world.getBlockTileEntity(x, y, z) match {
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

  override def neighborBlockChanged(world: World, x: Int, y: Int, z: Int, blockId: Int) {
    world.getBlockTileEntity(x, y, z) match {
      case charger: tileentity.Charger => charger.onNeighborChanged()
      case _ =>
    }
    super.neighborBlockChanged(world, x, y, z, blockId)
  }
}
