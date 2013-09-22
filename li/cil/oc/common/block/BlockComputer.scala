package li.cil.oc.common.block

import cpw.mods.fml.common.registry.GameRegistry
import li.cil.oc.OpenComputers
import li.cil.oc.common.GuiType
import li.cil.oc.common.tileentity.TileEntityComputer
import net.minecraft.client.renderer.texture.IconRegister
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.Icon
import net.minecraft.world.IBlockAccess
import net.minecraft.world.World
import net.minecraftforge.common.ForgeDirection

class BlockComputer(val parent: BlockMulti) extends SubBlock {
  GameRegistry.registerTileEntity(classOf[TileEntityComputer], "oc.computer")

  val unlocalizedName = "Computer"

  // ----------------------------------------------------------------------- //
  // INetworkBlock
  // ----------------------------------------------------------------------- //

  override def hasNode = true

  override def getNode(world: IBlockAccess, x: Int, y: Int, z: Int) =
    world.getBlockTileEntity(x, y, z) match {
      case computer: TileEntityComputer => computer
    }

  // ----------------------------------------------------------------------- //
  // Rendering stuff
  // ----------------------------------------------------------------------- //

  object Icons {
    val on = Array.fill[Icon](6)(null)
    val off = Array.fill[Icon](6)(null)
  }

  override def getBlockTextureFromSide(world: IBlockAccess, x: Int, y: Int, z: Int, worldSide: ForgeDirection, localSide: ForgeDirection) = {
    getIcon(localSide, world.getBlockTileEntity(x, y, z) match {
      case computer: TileEntityComputer => computer.isOn
      case _ => false
    })
  }

  override def getIcon(side: ForgeDirection) = getIcon(side, false)

  private def getIcon(side: ForgeDirection, isOn: Boolean) =
    if (isOn) Icons.on(side.ordinal) else Icons.off(side.ordinal)

  override def registerIcons(iconRegister: IconRegister) = {
    Icons.off(ForgeDirection.DOWN.ordinal) = iconRegister.registerIcon("opencomputers:computer_top")
    Icons.on(ForgeDirection.DOWN.ordinal) = Icons.off(ForgeDirection.DOWN.ordinal)
    Icons.off(ForgeDirection.UP.ordinal) = Icons.off(ForgeDirection.DOWN.ordinal)
    Icons.on(ForgeDirection.UP.ordinal) = Icons.on(ForgeDirection.UP.ordinal)

    Icons.off(ForgeDirection.NORTH.ordinal) = iconRegister.registerIcon("opencomputers:computer_back")
    Icons.on(ForgeDirection.NORTH.ordinal) = iconRegister.registerIcon("opencomputers:computer_back_on")

    Icons.off(ForgeDirection.SOUTH.ordinal) = iconRegister.registerIcon("opencomputers:computer_front")
    Icons.on(ForgeDirection.SOUTH.ordinal) = Icons.off(ForgeDirection.SOUTH.ordinal)

    Icons.off(ForgeDirection.WEST.ordinal) = iconRegister.registerIcon("opencomputers:computer_side")
    Icons.on(ForgeDirection.WEST.ordinal) = iconRegister.registerIcon("opencomputers:computer_side_on")
    Icons.off(ForgeDirection.EAST.ordinal) = Icons.off(ForgeDirection.WEST.ordinal)
    Icons.on(ForgeDirection.EAST.ordinal) = Icons.on(ForgeDirection.WEST.ordinal)
  }

  // ----------------------------------------------------------------------- //
  // Tile entity
  // ----------------------------------------------------------------------- //

  override def hasTileEntity = true

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
    if (!player.isSneaking) {
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