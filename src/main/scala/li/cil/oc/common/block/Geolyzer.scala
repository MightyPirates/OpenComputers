package li.cil.oc.common.block

import li.cil.oc.Settings
import li.cil.oc.client.Textures
import li.cil.oc.common.tileentity
import net.minecraft.world.{IBlockAccess, World}

class Geolyzer(val parent: SimpleDelegator) extends SimpleDelegate {
  override protected def customTextures = Array(
    None,
    Some("GeolyzerTop"),
    Some("GeolyzerSide"),
    Some("GeolyzerSide"),
    Some("GeolyzerSide"),
    Some("GeolyzerSide")
  )

  override def registerIcons(iconRegister: IconRegister) = {
    super.registerIcons(iconRegister)
    Textures.Geolyzer.iconTopOn = iconRegister.registerIcon(Settings.resourceDomain + ":GeolyzerTopOn")
  }

  override def luminance(world: IBlockAccess, x: Int, y: Int, z: Int) = 2

  // ----------------------------------------------------------------------- //

  override def hasTileEntity = true

  override def createTileEntity(world: World) = Some(new tileentity.Geolyzer())
}

