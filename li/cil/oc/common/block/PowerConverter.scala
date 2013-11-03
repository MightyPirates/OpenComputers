package li.cil.oc.common.block

import cpw.mods.fml.common.registry.GameRegistry
import li.cil.oc.Config
import li.cil.oc.common.tileentity
import net.minecraft.client.renderer.texture.IconRegister
import net.minecraft.util.Icon
import net.minecraft.world.World
import net.minecraftforge.common.ForgeDirection

class PowerConverter(val parent: SimpleDelegator) extends SimpleDelegate {
  GameRegistry.registerTileEntity(classOf[tileentity.PowerConverter], "oc.power_converter")
  val unlocalizedName = "PowerConverter"

  override def breakBlock(world: World, x: Int, y: Int, z: Int, blockId: Int) =
    world.getBlockTileEntity(x, y, z).asInstanceOf[tileentity.PowerConverter].unload()

  // ----------------------------------------------------------------------- //

  private val icons = Array.fill[Icon](6)(null)

  override def icon(side: ForgeDirection) = Some(icons(side.ordinal))

  override def registerIcons(iconRegister: IconRegister) = {
    icons(ForgeDirection.DOWN.ordinal) = iconRegister.registerIcon(Config.resourceDomain + ":computer_top")
    icons(ForgeDirection.UP.ordinal) = icons(ForgeDirection.DOWN.ordinal)

    icons(ForgeDirection.NORTH.ordinal) = iconRegister.registerIcon(Config.resourceDomain + ":power_converter")
    icons(ForgeDirection.SOUTH.ordinal) = icons(ForgeDirection.NORTH.ordinal)
    icons(ForgeDirection.WEST.ordinal) = icons(ForgeDirection.NORTH.ordinal)
    icons(ForgeDirection.EAST.ordinal) = icons(ForgeDirection.NORTH.ordinal)
  }

  // ----------------------------------------------------------------------- //
  // Tile entity
  // ----------------------------------------------------------------------- //

  override def hasTileEntity = true

  override def createTileEntity(world: World) = Some(new tileentity.PowerConverter)
}
