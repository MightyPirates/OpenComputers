package li.cil.oc.common.block

import li.cil.oc.Settings
import li.cil.oc.client.Textures
import li.cil.oc.common.tileentity
import net.minecraft.world.{IBlockAccess, World}

class PowerDistributor(val parent: SimpleDelegator) extends SimpleDelegate {
  override protected def customTextures = Array(
    None,
    Some("PowerDistributorTop"),
    Some("PowerDistributorSide"),
    Some("PowerDistributorSide"),
    Some("PowerDistributorSide"),
    Some("PowerDistributorSide")
  )

  override def registerIcons(iconRegister: IconRegister) = {
    super.registerIcons(iconRegister)
    Textures.PowerDistributor.iconSideOn = iconRegister.registerIcon(Settings.resourceDomain + ":PowerDistributorSideOn")
    Textures.PowerDistributor.iconTopOn = iconRegister.registerIcon(Settings.resourceDomain + ":PowerDistributorTopOn")
  }

  override def luminance(world: IBlockAccess, x: Int, y: Int, z: Int) = 5

  // ----------------------------------------------------------------------- //

  override def hasTileEntity = true

  override def createTileEntity(world: World) = Some(new tileentity.PowerDistributor())
}

