package li.cil.oc.common.block

import java.util

import cpw.mods.fml.common.Optional
import cpw.mods.fml.relauncher.{Side, SideOnly}
import li.cil.oc.client.KeyBindings
import li.cil.oc.common.{GuiType, tileentity}
import li.cil.oc.server.PacketSender
import li.cil.oc.server.component.robot
import li.cil.oc.util.mods.Mods
import li.cil.oc.util.{ItemUtils, Tooltip}
import li.cil.oc.{Blocks, OpenComputers, Settings}
import mcp.mobius.waila.api.{IWailaConfigHandler, IWailaDataAccessor}
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.{Entity, EntityLivingBase}
import net.minecraft.item.{EnumRarity, ItemStack}
import net.minecraft.util.{AxisAlignedBB, MovingObjectPosition, Vec3}
import net.minecraft.world.{IBlockAccess, World}
import net.minecraftforge.common.ForgeDirection

class RobotProxy(val parent: SpecialDelegator) extends RedstoneAware with SpecialDelegate {
  override val unlocalizedName = "Robot"

  private var icon: Icon = _

  var moving = new ThreadLocal[Option[tileentity.Robot]] {
    override protected def initialValue = None
  }

  showInItemList = false

  // ----------------------------------------------------------------------- //

  override def rarity = EnumRarity.epic

  override def tooltipLines(stack: ItemStack, player: EntityPlayer, tooltip: util.List[String], advanced: Boolean) {
    addLines(stack, tooltip)
    super.tooltipLines(stack, player, tooltip, advanced)
    if (KeyBindings.showExtendedTooltips) {
      val info = new ItemUtils.RobotData(stack)
      for (component <- info.containers ++ info.components) {
        tooltip.add("- " + component.getDisplayName)
      }
    }
  }

  @Optional.Method(modid = Mods.IDs.Waila)
  override def wailaBody(stack: ItemStack, tooltip: util.List[String], accessor: IWailaDataAccessor, config: IWailaConfigHandler) {
    addLines(stack, tooltip)
  }

  private def addLines(stack: ItemStack, tooltip: util.List[String]) {
    if (stack.hasTagCompound) {
      if (stack.getTagCompound.hasKey(Settings.namespace + "xp")) {
        val xp = stack.getTagCompound.getDouble(Settings.namespace + "xp")
        val level = math.min((Math.pow(xp - Settings.get.baseXpToLevel, 1 / Settings.get.exponentialXpGrowth) / Settings.get.constantXpGrowth).toInt, 30)
        if (level > 0) {
          tooltip.addAll(Tooltip.get(unlocalizedName + "_Level", level))
        }
      }
      if (stack.getTagCompound.hasKey(Settings.namespace + "storedEnergy")) {
        val energy = stack.getTagCompound.getInteger(Settings.namespace + "storedEnergy")
        if (energy > 0) {
          tooltip.addAll(Tooltip.get(unlocalizedName + "_StoredEnergy", energy))
        }
      }
    }
  }

  @SideOnly(Side.CLIENT)
  override def icon(side: ForgeDirection) = Some(icon)

  @SideOnly(Side.CLIENT)
  override def registerIcons(iconRegister: IconRegister) {
    super.registerIcons(iconRegister)
    icon = iconRegister.registerIcon(Settings.resourceDomain + ":GenericTop")
  }

  override def pick(target: MovingObjectPosition, world: World, x: Int, y: Int, z: Int) =
    world.getBlockTileEntity(x, y, z) match {
      case proxy: tileentity.RobotProxy => proxy.robot.info.copyItemStack()
      case _ => null
    }

  // ----------------------------------------------------------------------- //

  override def createTileEntity(world: World) = {
    moving.get match {
      case Some(robot) => Some(new tileentity.RobotProxy(robot))
      case _ => Some(new tileentity.RobotProxy())
    }
  }

  // ----------------------------------------------------------------------- //

  override def explosionResistance(entity: Entity) = 10f

  override def isNormalCube(world: World, x: Int, y: Int, z: Int) = false

  override def isSolid(world: IBlockAccess, x: Int, y: Int, z: Int, side: ForgeDirection) = false

  override def opacity(world: World, x: Int, y: Int, z: Int) = 0

  override def shouldSideBeRendered(world: IBlockAccess, x: Int, y: Int, z: Int, side: ForgeDirection) = false

  // ----------------------------------------------------------------------- //

  override def drops(world: World, x: Int, y: Int, z: Int, fortune: Int) = Some(new java.util.ArrayList[ItemStack]())

  override def intersect(world: World, x: Int, y: Int, z: Int, origin: Vec3, direction: Vec3) = {
    val bounds = parent.getCollisionBoundingBoxFromPool(world, x, y, z)
    world.getBlockTileEntity(x, y, z) match {
      case proxy: tileentity.RobotProxy if proxy.robot.animationTicksLeft <= 0 && bounds.isVecInside(origin) => null
      case _ => super.intersect(world, x, y, z, origin, direction)
    }
  }

  override def updateBounds(world: IBlockAccess, x: Int, y: Int, z: Int) {
    world.getBlockTileEntity(x, y, z) match {
      case proxy: tileentity.RobotProxy =>
        val robot = proxy.robot
        val bounds = AxisAlignedBB.getBoundingBox(0.1, 0.1, 0.1, 0.9, 0.9, 0.9)
        if (robot.isAnimatingMove) {
          val remaining = robot.animationTicksLeft.toDouble / robot.animationTicksTotal.toDouble
          val dx = robot.moveFromX - robot.x
          val dy = robot.moveFromY - robot.y
          val dz = robot.moveFromZ - robot.z
          bounds.offset(dx * remaining, dy * remaining, dz * remaining)
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
    else if (player.getCurrentEquippedItem == null) {
      if (!world.isRemote) {
        world.getBlockTileEntity(x, y, z) match {
          case proxy: tileentity.RobotProxy if !proxy.isRunning => proxy.start()
          case _ =>
        }
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
        robot.info.load(stack)
        robot.bot.node.changeBuffer(robot.info.robotEnergy - robot.bot.node.localBuffer)
        robot.updateInventorySize()
        robot.updateMaxComponentCount()
      case _ =>
    }
  }

  override def removedByEntity(world: World, x: Int, y: Int, z: Int, player: EntityPlayer): Boolean = {
    world.getBlockTileEntity(x, y, z) match {
      case proxy: tileentity.RobotProxy =>
        val robot = proxy.robot
        if (!world.isRemote) {
          if (robot.player == player) return false
          robot.node.remove()
          robot.saveComponents()
          parent.dropBlockAsItem(world, x, y, z, robot.info.createItemStack())
        }
        if (Blocks.blockSpecial.subBlock(world, robot.moveFromX, robot.moveFromY, robot.moveFromZ).exists(_ == Blocks.robotAfterimage)) {
          world.setBlock(robot.moveFromX, robot.moveFromY, robot.moveFromZ, 0, 0, 1)
        }
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
