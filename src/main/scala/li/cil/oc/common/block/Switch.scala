package li.cil.oc.common.block

import li.cil.oc.Settings
import li.cil.oc.client.Textures
import li.cil.oc.common.GuiType
import li.cil.oc.common.tileentity
import li.cil.oc.integration.util.NEI
import net.minecraft.client.renderer.texture.IIconRegister
import net.minecraft.world.World

// TODO Remove in 1.7
class Switch extends SimpleBlock with traits.GUI {
  NEI.hide(this)

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

  override def guiType = GuiType.Switch

  override def hasTileEntity(metadata: Int) = true

  override def createTileEntity(world: World, metadata: Int) = new tileentity.Switch()
}
