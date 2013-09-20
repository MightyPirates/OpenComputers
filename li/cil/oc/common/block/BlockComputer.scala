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

  object Icons {
    var back = Array.fill[Icon](2)(null)
    var front = Array.fill[Icon](2)(null)
    var side = Array.fill[Icon](2)(null)
    var top: Icon = null
  }

  override def getBlockTextureFromSide(world: IBlockAccess, x: Int, y: Int, z: Int, side: ForgeDirection) = {
    val isOn = if (world.getBlockTileEntity(x, y, z) match {
      case computer: TileEntityComputer => computer.isOn
      case _ => false
    }) 1 else 0
    side match {
      case ForgeDirection.NORTH => Icons.back(isOn)
      case ForgeDirection.SOUTH => Icons.front(isOn)
      case ForgeDirection.WEST | ForgeDirection.EAST => Icons.side(isOn)
      case _ => Icons.top
    }
  }

  override def registerIcons(iconRegister: IconRegister) = {
    Icons.back(0) = iconRegister.registerIcon("opencomputers:computer_back")
    Icons.back(1) = iconRegister.registerIcon("opencomputers:computer_back_on")
    Icons.front(0) = iconRegister.registerIcon("opencomputers:computer_front")
    Icons.front(1) = iconRegister.registerIcon("opencomputers:computer_front_on")
    Icons.side(0) = iconRegister.registerIcon("opencomputers:computer_side")
    Icons.side(1) = iconRegister.registerIcon("opencomputers:computer_side_on")
    Icons.top = iconRegister.registerIcon("opencomputers:computer_top")
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

  override def getValidRotations(world: World, x: Int, y: Int, z: Int) = validRotations

  /** Avoid creating new arrays at the cost of this possibly getting modified. */
  private val validRotations = Array(
    ForgeDirection.SOUTH,
    ForgeDirection.WEST,
    ForgeDirection.NORTH,
    ForgeDirection.EAST)
}