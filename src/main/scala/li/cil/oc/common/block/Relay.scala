package li.cil.oc.common.block

import li.cil.oc.Settings
import li.cil.oc.client.Textures
import li.cil.oc.common.GuiType
import li.cil.oc.common.tileentity
import net.minecraft.client.renderer.texture.IIconRegister
import net.minecraft.world.World

class Relay extends SimpleBlock with traits.GUI with traits.PowerAcceptor {
  override protected def customTextures = Array(
    None,
    Some("SwitchTop"),
    Some("SwitchSide"),
    Some("SwitchSide"),
    Some("SwitchSide"),
    Some("SwitchSide")
  )

  override def registerBlockIcons(iconRegister: IIconRegister) = {
    super.registerBlockIcons(iconRegister)
    Textures.Switch.iconSideActivity = iconRegister.registerIcon(Settings.resourceDomain + ":SwitchSideOn")
  }

  // ----------------------------------------------------------------------- //

  override def guiType = GuiType.Relay

  override def energyThroughput = Settings.get.accessPointRate

  override def hasTileEntity(metadata: Int) = true

  override def createTileEntity(world: World, metadata: Int) = new tileentity.Relay()
}
