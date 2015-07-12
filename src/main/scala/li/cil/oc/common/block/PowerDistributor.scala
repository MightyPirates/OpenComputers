package li.cil.oc.common.block

import li.cil.oc.Settings
import li.cil.oc.client.Textures
import li.cil.oc.common.tileentity
import li.cil.oc.integration.coloredlights.ModColoredLights
import net.minecraft.client.renderer.texture.IIconRegister
import net.minecraft.world.World

class PowerDistributor extends SimpleBlock {
  ModColoredLights.setLightLevel(this, 5, 5, 3)

  override protected def customTextures = Array(
    None,
    Some("PowerDistributorTop"),
    Some("PowerDistributorSide"),
    Some("PowerDistributorSide"),
    Some("PowerDistributorSide"),
    Some("PowerDistributorSide")
  )

  override def registerBlockIcons(iconRegister: IIconRegister) = {
    super.registerBlockIcons(iconRegister)
    Textures.PowerDistributor.iconSideOn = iconRegister.registerIcon(Settings.resourceDomain + ":PowerDistributorSideOn")
    Textures.PowerDistributor.iconTopOn = iconRegister.registerIcon(Settings.resourceDomain + ":PowerDistributorTopOn")
  }

  // ----------------------------------------------------------------------- //

  override def hasTileEntity(metadata: Int) = true

  override def createTileEntity(world: World, metadata: Int) = new tileentity.PowerDistributor()
}

