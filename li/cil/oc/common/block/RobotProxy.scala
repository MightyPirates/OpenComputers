package li.cil.oc.common.block

import cpw.mods.fml.relauncher.{Side, SideOnly}
import java.util
import li.cil.oc.common.{GuiType, tileentity}
import li.cil.oc.server.PacketSender
import li.cil.oc.server.component.robot
import li.cil.oc.util.Tooltip
import li.cil.oc.{Settings, OpenComputers}
import net.minecraft.client.renderer.texture.IconRegister
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.util.{Icon, MovingObjectPosition, AxisAlignedBB, Vec3}
import net.minecraft.world.{IBlockAccess, World}
import net.minecraftforge.common.ForgeDirection

class RobotProxy(val parent: SpecialDelegator) extends Computer with SpecialDelegate {
  val unlocalizedName = "Robot"

  private var icon: Icon = _

  var moving = new ThreadLocal[Option[tileentity.Robot]] {
    override protected def initialValue = None
  }

  // ----------------------------------------------------------------------- //

  override def tooltipLines(stack: ItemStack, player: EntityPlayer, tooltip: util.List[String], advanced: Boolean) {
    if (stack.hasTagCompound && stack.getTagCompound.hasKey(Settings.namespace + "storedEnergy")) {
      tooltip.addAll(Tooltip.get(unlocalizedName + "_StoredEnergy", stack.getTagCompound.getInteger(Settings.namespace + "storedEnergy")))
    }
    tooltip.addAll(Tooltip.get(unlocalizedName))
  }

  @SideOnly(Side.CLIENT)
  override def icon(side: ForgeDirection) = Some(icon)

  @SideOnly(Side.CLIENT)
  override def registerIcons(iconRegister: IconRegister) {
    super.registerIcons(iconRegister)
    icon = iconRegister.registerIcon(Settings.resourceDomain + ":generic_top")
  }

  override def pick(target: MovingObjectPosition, world: World, x: Int, y: Int, z: Int) =
    world.getBlockTileEntity(x, y, z) match {
      case proxy: tileentity.RobotProxy => proxy.robot.createItemStack()
      case _ => null
    }

  // ----------------------------------------------------------------------- //

  override def createTileEntity(world: World) = {
    moving.get match {
      case Some(robot) => Some(new tileentity.RobotProxy(robot))
      case _ => Some(new tileentity.RobotProxy(new tileentity.Robot(world.isRemote)))
    }
  }

  // ----------------------------------------------------------------------- //

  override def isNormalCube(world: World, x: Int, y: Int, z: Int) = false

  override def isSolid(world: IBlockAccess, x: Int, y: Int, z: Int, side: ForgeDirection) = false

  override def opacity(world: World, x: Int, y: Int, z: Int) = 0

  override def shouldSideBeRendered(world: IBlockAccess, x: Int, y: Int, z: Int, side: ForgeDirection) = false

  // ----------------------------------------------------------------------- //

  override def drop(world: World, x: Int, y: Int, z: Int, chance: Float, fortune: Int) = true

  override def intersect(world: World, x: Int, y: Int, z: Int, origin: Vec3, direction: Vec3) = {
    val bounds = parent.getCollisionBoundingBoxFromPool(world, x, y, z)
    if (bounds.isVecInside(origin)) null
    else super.intersect(world, x, y, z, origin, direction)
  }

  override def updateBounds(world: IBlockAccess, x: Int, y: Int, z: Int) {
    world.getBlockTileEntity(x, y, z) match {
      case proxy: tileentity.RobotProxy =>
        val robot = proxy.robot
        val bounds = AxisAlignedBB.getBoundingBox(0.1, 0.1, 0.1, 0.9, 0.9, 0.9)
        if (robot.isAnimatingMove) {
          val remaining = robot.animationTicksLeft.toDouble / robot.animationTicksTotal.toDouble
          bounds.offset(
            -robot.moveDirection.offsetX * remaining,
            -robot.moveDirection.offsetY * remaining,
            -robot.moveDirection.offsetZ * remaining)
        }
        parent.setBlockBounds(bounds)
      case _ => super.updateBounds(world, x, y, z)
    }
  }

  override def rightClick(world: World, x: Int, y: Int, z: Int, player: EntityPlayer,
                          side: ForgeDirection, hitX: Float, hitY: Float, hitZ: Float) = {
    if (!player.isSneaking) {
      if (!world.isRemote) {
        // We only send slot changes to nearby players, so if there was no slot
        // change since this player got into range he might have the wrong one,
        // so we send him the current one just in case.
        world.getBlockTileEntity(x, y, z) match {
          case proxy: tileentity.RobotProxy =>
            PacketSender.sendRobotSelectedSlotChange(proxy.robot)
          case _ =>
        }
        player.openGui(OpenComputers, GuiType.Robot.id, world, x, y, z)
      }
      true
    }
    else false
  }

  override def addedByEntity(world: World, x: Int, y: Int, z: Int, entity: EntityLivingBase, stack: ItemStack) {
    super.addedByEntity(world, x, y, z, entity, stack)
    if (!world.isRemote) ((entity, world.getBlockTileEntity(x, y, z)) match {
      case (player: robot.Player, proxy: tileentity.RobotProxy) =>
        Some((proxy.robot, player.robot.owner))
      case (player: EntityPlayer, proxy: tileentity.RobotProxy) =>
        Some((proxy.robot, player.getCommandSenderName))
      case _ => None
    }) match {
      case Some((robot, owner)) =>
        robot.owner = owner
        robot.parseItemStack(stack)
      case _ =>
    }
  }

  override def removedByEntity(world: World, x: Int, y: Int, z: Int, player: EntityPlayer) = {
    if (!world.isRemote) world.getBlockTileEntity(x, y, z) match {
      case proxy: tileentity.RobotProxy if !player.capabilities.isCreativeMode || proxy.globalBuffer > 1 =>
        parent.dropBlockAsItem(world, x, y, z, proxy.robot.createItemStack())
      case _ =>
    }
    super.removedByEntity(world, x, y, z, player)
  }

  override def aboutToBeRemoved(world: World, x: Int, y: Int, z: Int) {
    if (moving.get.isEmpty) {
      super.aboutToBeRemoved(world, x, y, z)
    }
  }
}
