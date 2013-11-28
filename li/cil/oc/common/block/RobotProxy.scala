package li.cil.oc.common.block

import java.util
import li.cil.oc.common.{GuiType, tileentity}
import li.cil.oc.server.component.robot
import li.cil.oc.util.Tooltip
import li.cil.oc.{Settings, OpenComputers}
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.{AxisAlignedBB, Vec3}
import net.minecraft.world.{IBlockAccess, World}
import net.minecraftforge.common.ForgeDirection

class RobotProxy(val parent: SpecialDelegator) extends Computer with SpecialDelegate {
  val unlocalizedName = "Robot"

  var moving = new ThreadLocal[Option[tileentity.Robot]] {
    override protected def initialValue = None
  }

  // ----------------------------------------------------------------------- //

  override def addInformation(stack: ItemStack, player: EntityPlayer, tooltip: util.List[String], advanced: Boolean) {
    if (stack.hasTagCompound && stack.getTagCompound.hasKey(Settings.namespace + "storedEnergy")) {
      tooltip.addAll(Tooltip.get(unlocalizedName + "_StoredEnergy", stack.getTagCompound.getInteger(Settings.namespace + "storedEnergy")))
    }
    tooltip.addAll(Tooltip.get(unlocalizedName))
  }

  // ----------------------------------------------------------------------- //

  override def createTileEntity(world: World) = {
    moving.get match {
      case Some(robot) => Some(new tileentity.RobotProxy(robot))
      case _ => Some(new tileentity.RobotProxy(new tileentity.Robot(world.isRemote)))
    }
  }

  // ----------------------------------------------------------------------- //

  override def isBlockNormalCube(world: World, x: Int, y: Int, z: Int) = false

  override def isBlockSolid(world: IBlockAccess, x: Int, y: Int, z: Int, side: ForgeDirection) = false

  override def getLightOpacity(world: World, x: Int, y: Int, z: Int) = 0

  override def shouldSideBeRendered(world: IBlockAccess, x: Int, y: Int, z: Int, side: ForgeDirection) = false

  // ----------------------------------------------------------------------- //

  override def dropBlockAsItemWithChance(world: World, x: Int, y: Int, z: Int, chance: Float, fortune: Int) = true

  override def collisionRayTrace(world: World, x: Int, y: Int, z: Int, origin: Vec3, direction: Vec3) = {
    val bounds = parent.getCollisionBoundingBoxFromPool(world, x, y, z)
    if (bounds.isVecInside(origin)) null
    else super.collisionRayTrace(world, x, y, z, origin, direction)
  }

  override def setBlockBoundsBasedOnState(world: IBlockAccess, x: Int, y: Int, z: Int) {
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
      case _ => super.setBlockBoundsBasedOnState(world, x, y, z)
    }
  }

  override def onBlockActivated(world: World, x: Int, y: Int, z: Int, player: EntityPlayer,
                                side: ForgeDirection, hitX: Float, hitY: Float, hitZ: Float) = {
    if (!player.isSneaking) {
      if (!world.isRemote) {
        player.openGui(OpenComputers, GuiType.Robot.id, world, x, y, z)
      }
      true
    }
    else false
  }

  override def onBlockPlacedBy(world: World, x: Int, y: Int, z: Int, entity: EntityLivingBase, stack: ItemStack) {
    super.onBlockPlacedBy(world, x, y, z, entity, stack)
    if (!world.isRemote) ((entity, world.getBlockTileEntity(x, y, z)) match {
      case (player: robot.Player, proxy: tileentity.RobotProxy) =>
        Some((proxy, player.robot.owner))
      case (player: EntityPlayer, proxy: tileentity.RobotProxy) =>
        Some((proxy, player.getCommandSenderName))
      case _ => None
    }) match {
      case Some((proxy, owner)) =>
        proxy.robot.owner = owner
        if (stack.hasTagCompound) {
          proxy.robot.battery.changeBuffer(stack.getTagCompound.getInteger(Settings.namespace + "storedEnergy"))
        }
      case _ =>
    }
  }

  override def onBlockRemovedBy(world: World, x: Int, y: Int, z: Int, player: EntityPlayer) = {
    if (!world.isRemote) world.getBlockTileEntity(x, y, z) match {
      case proxy: tileentity.RobotProxy if !player.capabilities.isCreativeMode || proxy.globalBuffer > 0 =>
        val stack = createItemStack()
        if (proxy.globalBuffer > 1) {
          stack.setTagCompound(new NBTTagCompound("tag"))
          stack.getTagCompound.setInteger(Settings.namespace + "storedEnergy", proxy.globalBuffer.toInt)
        }
        parent.dropBlockAsItem(world, x, y, z, stack)
      case _ =>
    }
    super.onBlockRemovedBy(world, x, y, z, player)
  }

  override def onBlockPreDestroy(world: World, x: Int, y: Int, z: Int) {
    if (moving.get.isEmpty) {
      super.onBlockPreDestroy(world, x, y, z)
    }
  }
}
