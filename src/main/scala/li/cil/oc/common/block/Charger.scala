package li.cil.oc.common.block

import cpw.mods.fml.relauncher.{Side, SideOnly}
import java.util
import li.cil.oc.Settings
import li.cil.oc.common.tileentity
import li.cil.oc.server.PacketSender
import li.cil.oc.util.Tooltip
import li.cil.oc.util.mods.BuildCraft
import net.minecraft.block.Block
import net.minecraft.client.renderer.texture.IIconRegister
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.util.IIcon
import net.minecraft.world.{IBlockAccess, World}
import net.minecraftforge.common.util.ForgeDirection

class Charger(val parent: SimpleDelegator) extends RedstoneAware with SimpleDelegate {
  val unlocalizedName = "Charger"

  private val icons = Array.fill[IIcon](6)(null)
  private val iconsCharging = Array.fill[IIcon](6)(null)

  override def tooltipLines(stack: ItemStack, player: EntityPlayer, tooltip: util.List[String], advanced: Boolean) {
    tooltip.addAll(Tooltip.get(unlocalizedName))
  }

  override def icon(side: ForgeDirection) = Some(icons(side.ordinal()))

  @SideOnly(Side.CLIENT)
  override def icon(world: IBlockAccess, x: Int, y: Int, z: Int, worldSide: ForgeDirection, localSide: ForgeDirection) =
    world.getTileEntity(x, y, z) match {
      case charger: tileentity.Charger if charger.chargeSpeed > 0 => Some(iconsCharging(localSide.ordinal()))
      case _ => Some(icons(localSide.ordinal()))
    }

  override def registerIcons(iconRegister: IIconRegister) = {
    icons(ForgeDirection.DOWN.ordinal) = iconRegister.registerIcon(Settings.resourceDomain + ":generic_top")
    icons(ForgeDirection.UP.ordinal) = icons(ForgeDirection.DOWN.ordinal)

    icons(ForgeDirection.NORTH.ordinal) = iconRegister.registerIcon(Settings.resourceDomain + ":charger")
    icons(ForgeDirection.SOUTH.ordinal) = icons(ForgeDirection.NORTH.ordinal)
    icons(ForgeDirection.WEST.ordinal) = icons(ForgeDirection.NORTH.ordinal)
    icons(ForgeDirection.EAST.ordinal) = icons(ForgeDirection.NORTH.ordinal)

    iconsCharging(ForgeDirection.DOWN.ordinal) = icons(ForgeDirection.DOWN.ordinal)
    iconsCharging(ForgeDirection.UP.ordinal) = icons(ForgeDirection.UP.ordinal)

    iconsCharging(ForgeDirection.NORTH.ordinal) = iconRegister.registerIcon(Settings.resourceDomain + ":charger_on")
    iconsCharging(ForgeDirection.SOUTH.ordinal) = iconsCharging(ForgeDirection.NORTH.ordinal)
    iconsCharging(ForgeDirection.WEST.ordinal) = iconsCharging(ForgeDirection.NORTH.ordinal)
    iconsCharging(ForgeDirection.EAST.ordinal) = iconsCharging(ForgeDirection.NORTH.ordinal)
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
