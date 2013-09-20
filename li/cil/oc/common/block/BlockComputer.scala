package li.cil.oc.common.block

import li.cil.oc.OpenComputers
import li.cil.oc.common.GuiType
import li.cil.oc.common.tileentity.TileEntityComputer
import net.minecraft.client.renderer.texture.IconRegister
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.Icon
import net.minecraft.world.IBlockAccess
import net.minecraft.world.World
import net.minecraftforge.common.ForgeDirection

class BlockComputer extends SubBlock {
  val unlocalizedName = "Computer"

  // ----------------------------------------------------------------------- //
  // Rendering stuff
  // ----------------------------------------------------------------------- //

  var computerBack: Icon = null
  var computerBackOn: Icon = null
  var computerFront: Icon = null
  var computerFrontOn: Icon = null
  var computerSide: Icon = null
  var computerSideOn: Icon = null
  var computerTop: Icon = null

  override def getBlockTextureFromSide(world: IBlockAccess, x: Int, y: Int, z: Int, side: ForgeDirection) = {
    val isOn = world.getBlockTileEntity(x, y, z) match {
      case computer: TileEntityComputer => computer.isOn
      case _ => false
    }
    side match {
      case ForgeDirection.NORTH => if (isOn) computerBackOn else computerBack
      case ForgeDirection.SOUTH => if (isOn) computerFrontOn else computerFront
      case ForgeDirection.WEST | ForgeDirection.EAST => if (isOn) computerSideOn else computerSide
      case _ => computerTop
    }
  }

  override def registerIcons(iconRegister: IconRegister) = {
    computerBack = iconRegister.registerIcon("opencomputers:computer_back")
    computerBackOn = iconRegister.registerIcon("opencomputers:computer_back_on")
    computerFront = iconRegister.registerIcon("opencomputers:computer_front")
    computerFrontOn = iconRegister.registerIcon("opencomputers:computer_front_on")
    computerSide = iconRegister.registerIcon("opencomputers:computer_side")
    computerSideOn = iconRegister.registerIcon("opencomputers:computer_side_on")
    computerTop = iconRegister.registerIcon("opencomputers:computer_top")
  }

  // ----------------------------------------------------------------------- //
  // Tile entity
  // ----------------------------------------------------------------------- //

  override def hasTileEntity(metadata: Int) = true

  override def createTileEntity(world: World, metadata: Int) = new TileEntityComputer(world.isRemote)

  // ----------------------------------------------------------------------- //
  // Destruction / Interaction
  // ----------------------------------------------------------------------- //

  override def breakBlock(world: World, x: Int, y: Int, z: Int, `side?`: Int, metadata: Int) = {
    world.getBlockTileEntity(x, y, z).asInstanceOf[TileEntityComputer].turnOff()
    super.breakBlock(world, x, y, z, `side?`, metadata)
  }

  override def onBlockActivated(world: World, x: Int, y: Int, z: Int, player: EntityPlayer,
    side: ForgeDirection, hitX: Float, hitY: Float, hitZ: Float) = {
    if (!player.isSneaking()) {
      // Start the computer if it isn't already running and open the GUI.
      world.getBlockTileEntity(x, y, z).asInstanceOf[TileEntityComputer].turnOn()
      player.openGui(OpenComputers, GuiType.Computer.id, world, x, y, z)
      true
    }
    else false
  }

  override def onNeighborBlockChange(world: World, x: Int, y: Int, z: Int, blockId: Int) = {
    if (!world.isRemote) {
      world.getBlockTileEntity(x, y, z).asInstanceOf[TileEntityComputer].
        onNeighborBlockChange(blockId)
    }
  }

  // ----------------------------------------------------------------------- //
  // Block rotation
  // ----------------------------------------------------------------------- //

  override def getValidRotations(world: World, x: Int, y: Int, z: Int) =
    Array(ForgeDirection.SOUTH, ForgeDirection.WEST, ForgeDirection.NORTH, ForgeDirection.EAST)
}