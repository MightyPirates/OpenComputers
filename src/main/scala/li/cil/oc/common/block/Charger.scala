package li.cil.oc.common.block

import li.cil.oc.Settings
import li.cil.oc.client.Textures
import li.cil.oc.common.GuiType
import li.cil.oc.common.tileentity
import li.cil.oc.integration.util.Wrench
import li.cil.oc.server.PacketSender
import li.cil.oc.util.BlockPosition
import net.minecraft.block.Block
import net.minecraft.client.renderer.texture.IIconRegister
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.world.IBlockAccess
import net.minecraft.world.World
import net.minecraftforge.common.util.ForgeDirection

class Charger extends RedstoneAware with traits.PowerAcceptor with traits.StateAware with traits.GUI {
  override protected def customTextures = Array(
    None,
    None,
    Some("ChargerSide"),
    Some("ChargerFront"),
    Some("ChargerSide"),
    Some("ChargerSide")
  )

  override def registerBlockIcons(iconRegister: IIconRegister) = {
    super.registerBlockIcons(iconRegister)
    Textures.Charger.iconFrontCharging = iconRegister.registerIcon(Settings.resourceDomain + ":ChargerFrontOn")
    Textures.Charger.iconSideCharging = iconRegister.registerIcon(Settings.resourceDomain + ":ChargerSideOn")
  }

  // ----------------------------------------------------------------------- //

  override def energyThroughput = Settings.get.chargerRate

  override def guiType = GuiType.Charger

  override def createTileEntity(world: World, metadata: Int) = new tileentity.Charger()

  // ----------------------------------------------------------------------- //

  override def canConnectRedstone(world: IBlockAccess, x: Int, y: Int, z: Int, side: ForgeDirection) = true

  // ----------------------------------------------------------------------- //

  override def onBlockActivated(world: World, x: Int, y: Int, z: Int, player: EntityPlayer, side: ForgeDirection, hitX: Float, hitY: Float, hitZ: Float) =
    if (Wrench.holdsApplicableWrench(player, BlockPosition(x, y, z))) world.getTileEntity(x, y, z) match {
      case charger: tileentity.Charger =>
        if (!world.isRemote) {
          charger.invertSignal = !charger.invertSignal
          charger.chargeSpeed = 1.0 - charger.chargeSpeed
          PacketSender.sendChargerState(charger)
          Wrench.wrenchUsed(player, BlockPosition(x, y, z))
        }
        true
      case _ => false
    }
    else super.onBlockActivated(world, x, y, z, player, side, hitX, hitY, hitZ)

  override def onNeighborBlockChange(world: World, x: Int, y: Int, z: Int, block: Block) {
    world.getTileEntity(x, y, z) match {
      case charger: tileentity.Charger => charger.onNeighborChanged()
      case _ =>
    }
    super.onNeighborBlockChange(world, x, y, z, block)
  }
}
