package li.cil.oc.common.block

import cpw.mods.fml.common.registry.GameRegistry
import li.cil.oc.Config
import li.cil.oc.CreativeTab
import li.cil.oc.common.tileentity.TileEntityScreen
import net.minecraft.block.Block
import net.minecraft.block.material.Material
import net.minecraft.world.World

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
}