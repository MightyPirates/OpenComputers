package li.cil.oc.common.block

import java.util

import cpw.mods.fml.relauncher.Side
import cpw.mods.fml.relauncher.SideOnly
import li.cil.oc.OpenComputers
import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.client.KeyBindings
import li.cil.oc.common.GuiType
import li.cil.oc.common.tileentity
import li.cil.oc.integration.util.NEI
import li.cil.oc.server.PacketSender
import li.cil.oc.server.component.robot
import li.cil.oc.util.ItemUtils
import li.cil.oc.util.Tooltip
import net.minecraft.client.renderer.texture.IIconRegister
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.EnumRarity
import net.minecraft.item.ItemStack
import net.minecraft.util.AxisAlignedBB
import net.minecraft.util.IIcon
import net.minecraft.util.MovingObjectPosition
import net.minecraft.util.Vec3
import net.minecraft.world.IBlockAccess
import net.minecraft.world.World
import net.minecraftforge.common.util.ForgeDirection

class RobotProxy extends RedstoneAware with traits.SpecialBlock {
  setLightOpacity(0)
  setCreativeTab(null)
  NEI.hide(this)

  override val getUnlocalizedName = "Robot"

  private var icon: IIcon = _

  var moving = new ThreadLocal[Option[tileentity.Robot]] {
    override protected def initialValue = None
  }

  // ----------------------------------------------------------------------- //

  @SideOnly(Side.CLIENT)
  override def registerBlockIcons(iconRegister: IIconRegister) {
    super.registerBlockIcons(iconRegister)
    icon = iconRegister.registerIcon(Settings.resourceDomain + ":GenericTop")
  }

  @SideOnly(Side.CLIENT)
  override def getIcon(side: ForgeDirection, metadata: Int) = icon

  override def shouldSideBeRendered(world: IBlockAccess, x: Int, y: Int, z: Int, side: ForgeDirection) = false

  override def isBlockSolid(world: IBlockAccess, x: Int, y: Int, z: Int, side: ForgeDirection) = false

  override def getPickBlock(target: MovingObjectPosition, world: World, x: Int, y: Int, z: Int) =
    world.getTileEntity(x, y, z) match {
      case proxy: tileentity.RobotProxy => proxy.robot.info.copyItemStack()
      case _ => null
    }

  // ----------------------------------------------------------------------- //

  override def rarity = EnumRarity.epic

  override protected def tooltipHead(metadata: Int, stack: ItemStack, player: EntityPlayer, tooltip: util.List[String], advanced: Boolean) {
    super.tooltipHead(metadata, stack, player, tooltip, advanced)
    addLines(stack, tooltip)
  }

  override protected def tooltipBody(metadata: Int, stack: ItemStack, player: EntityPlayer, tooltip: util.List[String], advanced: Boolean) {
    tooltip.addAll(Tooltip.get("Robot"))
  }

  override protected def tooltipTail(metadata: Int, stack: ItemStack, player: EntityPlayer, tooltip: util.List[String], advanced: Boolean) {
    super.tooltipTail(metadata, stack, player, tooltip, advanced)
    if (KeyBindings.showExtendedTooltips) {
      val info = new ItemUtils.RobotData(stack)
      for (component <- info.containers ++ info.components) {
        tooltip.add("- " + component.getDisplayName)
      }
    }
  }

  private def addLines(stack: ItemStack, tooltip: util.List[String]) {
    if (stack.hasTagCompound) {
      if (stack.getTagCompound.hasKey(Settings.namespace + "xp")) {
        val xp = stack.getTagCompound.getDouble(Settings.namespace + "xp")
        val level = math.min((Math.pow(xp - Settings.get.baseXpToLevel, 1 / Settings.get.exponentialXpGrowth) / Settings.get.constantXpGrowth).toInt, 30)
        if (level > 0) {
          tooltip.addAll(Tooltip.get(getUnlocalizedName + "_Level", level))
        }
      }
      if (stack.getTagCompound.hasKey(Settings.namespace + "storedEnergy")) {
        val energy = stack.getTagCompound.getInteger(Settings.namespace + "storedEnergy")
        if (energy > 0) {
          tooltip.addAll(Tooltip.get(getUnlocalizedName + "_StoredEnergy", energy))
        }
      }
    }
  }

  // ----------------------------------------------------------------------- //

  override def createTileEntity(world: World, metadata: Int) = {
    moving.get match {
      case Some(robot) => new tileentity.RobotProxy(robot)
      case _ => new tileentity.RobotProxy()
    }
  }

  // ----------------------------------------------------------------------- //

  override def getExplosionResistance(entity: Entity) = 10f

  override def getDrops(world: World, x: Int, y: Int, z: Int, metadata: Int, fortune: Int) = new java.util.ArrayList[ItemStack]()

  override def intersect(world: World, x: Int, y: Int, z: Int, origin: Vec3, direction: Vec3) = {
    val bounds = getCollisionBoundingBoxFromPool(world, x, y, z)
    world.getTileEntity(x, y, z) match {
      case proxy: tileentity.RobotProxy if proxy.robot.animationTicksLeft <= 0 && bounds.isVecInside(origin) => null
      case _ => super.intersect(world, x, y, z, origin, direction)
    }
  }

  override def doSetBlockBoundsBasedOnState(world: IBlockAccess, x: Int, y: Int, z: Int) {
    world.getTileEntity(x, y, z) match {
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
        setBlockBounds(bounds)
      case _ => super.doSetBlockBoundsBasedOnState(world, x, y, z)
    }
  }

  // ----------------------------------------------------------------------- //

  override def onBlockActivated(world: World, x: Int, y: Int, z: Int, player: EntityPlayer,
                                side: ForgeDirection, hitX: Float, hitY: Float, hitZ: Float) = {
    if (!player.isSneaking) {
      if (!world.isRemote) {
        // We only send slot changes to nearby players, so if there was no slot
        // change since this player got into range he might have the wrong one,
        // so we send him the current one just in case.
        world.getTileEntity(x, y, z) match {
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
        world.getTileEntity(x, y, z) match {
          case proxy: tileentity.RobotProxy if !proxy.machine.isRunning => proxy.machine.start()
          case _ =>
        }
      }
      true
    }
    else false
  }

  override def onBlockPlacedBy(world: World, x: Int, y: Int, z: Int, entity: EntityLivingBase, stack: ItemStack) {
    super.onBlockPlacedBy(world, x, y, z, entity, stack)
    if (!world.isRemote) ((entity, world.getTileEntity(x, y, z)) match {
      case (player: robot.Player, proxy: tileentity.RobotProxy) =>
        Some((proxy.robot, player.robot.owner, player.robot.ownerUuid))
      case (player: EntityPlayer, proxy: tileentity.RobotProxy) =>
        Some((proxy.robot, player.getCommandSenderName, Option(player.getGameProfile.getId)))
      case _ => None
    }) match {
      case Some((robot, owner, uuid)) =>
        robot.owner = owner
        robot.ownerUuid = uuid
        robot.info.load(stack)
        robot.bot.node.changeBuffer(robot.info.robotEnergy - robot.bot.node.localBuffer)
        robot.updateInventorySize()
      case _ =>
    }
  }

  override def removedByPlayer(world: World, player: EntityPlayer, x: Int, y: Int, z: Int, willHarvest: Boolean): Boolean = {
    world.getTileEntity(x, y, z) match {
      case proxy: tileentity.RobotProxy =>
        val robot = proxy.robot
        if (!world.isRemote) {
          if (robot.player == player) return false
          robot.node.remove()
          robot.saveComponents()
          dropBlockAsItem(world, x, y, z, robot.info.createItemStack())
        }
        if (world.getBlock(robot.moveFromX, robot.moveFromY, robot.moveFromZ) == api.Items.get("robotAfterimage").block) {
          world.setBlock(robot.moveFromX, robot.moveFromY, robot.moveFromZ, net.minecraft.init.Blocks.air, 0, 1)
        }
      case _ =>
    }
    super.removedByPlayer(world, player, x, y, z, willHarvest)
  }

  override def onBlockPreDestroy(world: World, x: Int, y: Int, z: Int, metadata: Int) {
    if (moving.get.isEmpty) {
      super.onBlockPreDestroy(world, x, y, z, metadata)
    }
  }
}
