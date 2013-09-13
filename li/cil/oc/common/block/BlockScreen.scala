package li.cil.oc.common.block

import cpw.mods.fml.common.registry.GameRegistry
import li.cil.oc.Config
import li.cil.oc.CreativeTab
import li.cil.oc.common.tileentity.TileEntityScreen
import net.minecraft.block.Block
import net.minecraft.block.material.Material
import net.minecraft.world.World
import net.minecraft.entity.player.EntityPlayer
import li.cil.oc.OpenComputers

class BlockScreen extends Block(Config.blockScreenId, Material.iron) {
  // ----------------------------------------------------------------------- //
  // Construction
  // ----------------------------------------------------------------------- //

  setHardness(2f)
  GameRegistry.registerBlock(this, "oc.screen")
  GameRegistry.registerTileEntity(classOf[TileEntityScreen], "oc.screen")
  setUnlocalizedName("oc.screen")
  setCreativeTab(CreativeTab)

  // ----------------------------------------------------------------------- //
  // Tile entity
  // ----------------------------------------------------------------------- //

  override def hasTileEntity(metadata: Int) = true

  override def createTileEntity(world: World, metadata: Int) = new TileEntityScreen(world.isRemote)
  
  override def onBlockActivated(world: World, x: Int, y: Int, z: Int, player: EntityPlayer,
    side: Int, hitX: Float, hitY: Float, hitZ: Float) = {
//    if (player.isSneaking())
//      if (canWrench(player, x, y, z))
//        setRotation(world, x, y, z, rotation(world, x, y, z) + 1)
//      else
//        false
//    else {
      // Start the computer if it isn't already running and open the GUI.
//      world.getBlockTileEntity(x, y, z).asInstanceOf[TileEntityComputer].turnOn()
      player.openGui(OpenComputers, 0, world, x, y, z)
      true
//    }
  }
}