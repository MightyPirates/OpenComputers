package li.cil.oc.common.block

import cpw.mods.fml.relauncher.Side
import cpw.mods.fml.relauncher.SideOnly
import li.cil.oc.Settings
import li.cil.oc.client.Textures
import li.cil.oc.common.tileentity
import li.cil.oc.integration.coloredlights.ModColoredLights
import net.minecraft.client.renderer.texture.IIconRegister
import net.minecraft.world.World

class Geolyzer extends SimpleBlock {
  ModColoredLights.setLightLevel(this, 3, 1, 1)

  override protected def customTextures = Array(
    None,
    Some("GeolyzerTop"),
    Some("GeolyzerSide"),
    Some("GeolyzerSide"),
    Some("GeolyzerSide"),
    Some("GeolyzerSide")
  )

  @SideOnly(Side.CLIENT)
  override def registerBlockIcons(iconRegister: IIconRegister) = {
    super.registerBlockIcons(iconRegister)
    Textures.Geolyzer.iconTopOn = iconRegister.registerIcon(Settings.resourceDomain + ":GeolyzerTopOn")
  }

  // ----------------------------------------------------------------------- //

  override def hasTileEntity(metadata: Int) = true

  override def createTileEntity(world: World, metadata: Int) = new tileentity.Geolyzer()
}
