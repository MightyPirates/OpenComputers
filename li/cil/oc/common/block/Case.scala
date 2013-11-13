package li.cil.oc.common.block

import li.cil.oc.common.GuiType
import li.cil.oc.common.tileentity
import li.cil.oc.{Config, OpenComputers}
import net.minecraft.client.renderer.texture.IconRegister
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.Icon
import net.minecraft.world.IBlockAccess
import net.minecraft.world.World
import net.minecraftforge.common.ForgeDirection

class Case(val parent: SimpleDelegator) extends SimpleDelegate {
  val unlocalizedName = "Case"

  // ----------------------------------------------------------------------- //

  private object Icons {
    val on = Array.fill[Icon](6)(null)
    val off = Array.fill[Icon](6)(null)
  }

  override def getBlockTextureFromSide(world: IBlockAccess, x: Int, y: Int, z: Int, worldSide: ForgeDirection, localSide: ForgeDirection) = {
    getIcon(localSide, world.getBlockTileEntity(x, y, z) match {
      case computer: tileentity.Case => computer.isOn
      case _ => false
    })
  }

  override def icon(side: ForgeDirection) = getIcon(side, isOn = false)

  private def getIcon(side: ForgeDirection, isOn: Boolean) =
    Some(if (isOn) Icons.on(side.ordinal) else Icons.off(side.ordinal))

  override def registerIcons(iconRegister: IconRegister) = {
    Icons.off(ForgeDirection.DOWN.ordinal) = iconRegister.registerIcon(Config.resourceDomain + ":case_top")
    Icons.on(ForgeDirection.DOWN.ordinal) = Icons.off(ForgeDirection.DOWN.ordinal)
    Icons.off(ForgeDirection.UP.ordinal) = Icons.off(ForgeDirection.DOWN.ordinal)
    Icons.on(ForgeDirection.UP.ordinal) = Icons.off(ForgeDirection.UP.ordinal)

    Icons.off(ForgeDirection.NORTH.ordinal) = iconRegister.registerIcon(Config.resourceDomain + ":case_back")
    Icons.on(ForgeDirection.NORTH.ordinal) = iconRegister.registerIcon(Config.resourceDomain + ":case_back_on")

    Icons.off(ForgeDirection.SOUTH.ordinal) = iconRegister.registerIcon(Config.resourceDomain + ":case_front")
    Icons.on(ForgeDirection.SOUTH.ordinal) = Icons.off(ForgeDirection.SOUTH.ordinal)

    Icons.off(ForgeDirection.WEST.ordinal) = iconRegister.registerIcon(Config.resourceDomain + ":case_side")
    Icons.on(ForgeDirection.WEST.ordinal) = iconRegister.registerIcon(Config.resourceDomain + ":case_side_on")
    Icons.off(ForgeDirection.EAST.ordinal) = Icons.off(ForgeDirection.WEST.ordinal)
    Icons.on(ForgeDirection.EAST.ordinal) = Icons.on(ForgeDirection.WEST.ordinal)
  }

  // ----------------------------------------------------------------------- //

  override def hasTileEntity = true

  override def createTileEntity(world: World) = Some(new tileentity.Case(world.isRemote))

  // ----------------------------------------------------------------------- //

  override def canConnectRedstone(world: IBlockAccess, x: Int, y: Int, z: Int, side: ForgeDirection) =
    world.getBlockTileEntity(x, y, z).asInstanceOf[tileentity.Case].canConnectRedstone(side)

  override def isProvidingStrongPower(world: IBlockAccess, x: Int, y: Int, z: Int, side: ForgeDirection) =
    isProvidingWeakPower(world, x, y, z, side)

  override def isProvidingWeakPower(world: IBlockAccess, x: Int, y: Int, z: Int, side: ForgeDirection) =
    world.getBlockTileEntity(x, y, z).asInstanceOf[tileentity.Case].output(side)

  override def onBlockPreDestroy(world: World, x: Int, y: Int, z: Int) =
    if (!world.isRemote) world.getBlockTileEntity(x, y, z) match {
      case computer: tileentity.Case =>
        computer.turnOff()
        computer.dropContent(world, x, y, z)
      case _ => // Ignore.
    }

  override def onBlockActivated(world: World, x: Int, y: Int, z: Int, player: EntityPlayer,
                                side: ForgeDirection, hitX: Float, hitY: Float, hitZ: Float) = {
    if (!player.isSneaking) {
      // Start the computer if it isn't already running and open the GUI.
      if (!world.isRemote) {
        world.getBlockTileEntity(x, y, z).asInstanceOf[tileentity.Case].turnOn()
      }
      player.openGui(OpenComputers, GuiType.Case.id, world, x, y, z)
      true
    }
    else false
  }

  override def onNeighborBlockChange(world: World, x: Int, y: Int, z: Int, blockId: Int) =
    world.getBlockTileEntity(x, y, z) match {
      case computer: tileentity.Case => computer.checkRedstoneInputChanged()
      case _ => // Ignore.
    }

  // TODO do we have to manually sync the client since we can only check this on the server side?
  override def onBlockRemovedBy(world: World, x: Int, y: Int, z: Int, player: EntityPlayer) =
    world.getBlockTileEntity(x, y, z) match {
      case computer: tileentity.Case if !world.isRemote =>
        computer.isUser(player.getCommandSenderName)
      case _ => super.onBlockRemovedBy(world, x, y, z, player)
    }
}