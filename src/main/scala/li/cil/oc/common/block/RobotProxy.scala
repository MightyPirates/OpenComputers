package li.cil.oc.common.block

import java.util

import li.cil.oc.OpenComputers
import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.client.KeyBindings
import li.cil.oc.common.GuiType
import li.cil.oc.common.tileentity
import li.cil.oc.integration.util.NEI
import li.cil.oc.server.PacketSender
import li.cil.oc.server.component.robot
import li.cil.oc.util.BlockPosition
import li.cil.oc.util.InventoryUtils
import li.cil.oc.util.ItemUtils
import li.cil.oc.util.Rarity
import li.cil.oc.util.Tooltip
import net.minecraft.block.state.IBlockState
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.tileentity.TileEntity
import net.minecraft.util._
import net.minecraft.world.IBlockAccess
import net.minecraft.world.World

class RobotProxy extends RedstoneAware with traits.SpecialBlock with traits.StateAware {
  setLightOpacity(0)
  setCreativeTab(null)
  NEI.hide(this)

  override val getUnlocalizedName = "Robot"

  var moving = new ThreadLocal[Option[tileentity.Robot]] {
    override protected def initialValue = None
  }

  // ----------------------------------------------------------------------- //

  override def shouldSideBeRendered(world: IBlockAccess, pos: BlockPos, side: EnumFacing) = false

  override def isBlockSolid(world: IBlockAccess, pos: BlockPos, side: EnumFacing) = false

  override def getPickBlock(target: MovingObjectPosition, world: World, pos: BlockPos) =
    world.getTileEntity(pos) match {
      case proxy: tileentity.RobotProxy => proxy.robot.info.copyItemStack()
      case _ => null
    }

  // ----------------------------------------------------------------------- //

  override def rarity(stack: ItemStack) = {
    val data = new ItemUtils.RobotData(stack)
    Rarity.byTier(data.tier)
  }

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
      val components = info.containers ++ info.components
      if (components.length > 0) {
        tooltip.addAll(Tooltip.get("Server.Components"))
        for (component <- components) {
          tooltip.add("- " + component.getDisplayName)
        }
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

  override def createNewTileEntity(world: World, metadata: Int) = {
    moving.get match {
      case Some(robot) => new tileentity.RobotProxy(robot)
      case _ => new tileentity.RobotProxy()
    }
  }

  // ----------------------------------------------------------------------- //

  override def getExplosionResistance(entity: Entity) = 10f

  override def getDrops(world: IBlockAccess, pos: BlockPos, state: IBlockState, fortune: Int) = {
    val list = new java.util.ArrayList[ItemStack]()

    // Superspecial hack... usually this will not work, because Minecraft calls
    // this method *after* the block has already been destroyed. Meaning we
    // won't have access to the tile entity.
    // However! Some mods with block breakers, specifically AE2's annihilation
    // plane, will call *only* this method (don't use a fake player to call
    // removedByPlayer), but call it *before* the block was destroyed. So in
    // general it *should* be safe to generate the item here if the tile entity
    // still exists, and always spawn the stack in removedByPlayer... if some
    // mod calls this before the block is broken *and* calls removedByPlayer
    // this will lead to dupes, but in some initial testing this wasn't the
    // case anywhere (TE autonomous activator, CC turtles).
    world.getTileEntity(pos) match {
      case proxy: tileentity.RobotProxy =>
        val robot = proxy.robot
        if (robot.node != null) {
          // Update: even more special hack! As discussed here http://git.io/IcNAyg
          // some mods call this even when they're not about to actually break the
          // block... soooo we need a whitelist to know when to generate a *proper*
          // drop (i.e. with file systems closed / open handles not saved, e.g.).
          if (gettingDropsForActualDrop) {
            robot.node.remove()
            robot.saveComponents()
          }
          list.add(robot.info.createItemStack())
        }
      case _ =>
    }

    list
  }

  private val getDropForRealDropCallers = Set(
    "appeng.parts.automation.PartAnnihilationPlane.EatBlock"
  )

  private def gettingDropsForActualDrop = new Exception().getStackTrace.exists(element => getDropForRealDropCallers.contains(element.getClassName + "." + element.getMethodName))

  protected override def intersect(world: World, pos: BlockPos, origin: Vec3, direction: Vec3) = {
    val bounds = getCollisionBoundingBox(world, pos, world.getBlockState(pos))
    world.getTileEntity(pos) match {
      case proxy: tileentity.RobotProxy if proxy.robot.animationTicksLeft <= 0 && bounds.isVecInside(origin) => null
      case _ => super.intersect(world, pos, origin, direction)
    }
  }

  protected override def doSetBlockBoundsBasedOnState(world: IBlockAccess, pos: BlockPos) {
    world.getTileEntity(pos) match {
      case proxy: tileentity.RobotProxy =>
        val robot = proxy.robot
        val bounds = AxisAlignedBB.fromBounds(0.1, 0.1, 0.1, 0.9, 0.9, 0.9)
        if (robot.isAnimatingMove) {
          val remaining = robot.animationTicksLeft.toDouble / robot.animationTicksTotal.toDouble
          val delta = robot.moveFrom.get.subtract(robot.getPos)
          bounds.offset(delta.getX * remaining, delta.getY * remaining, delta.getZ * remaining)
        }
        setBlockBounds(bounds)
      case _ => super.doSetBlockBoundsBasedOnState(world, pos)
    }
  }

  // ----------------------------------------------------------------------- //

  override def localOnBlockActivated(world: World, pos: BlockPos, player: EntityPlayer, side: EnumFacing, hitX: Float, hitY: Float, hitZ: Float) = {
    if (!player.isSneaking) {
      if (!world.isRemote) {
        // We only send slot changes to nearby players, so if there was no slot
        // change since this player got into range he might have the wrong one,
        // so we send him the current one just in case.
        world.getTileEntity(pos) match {
          case proxy: tileentity.RobotProxy if proxy.robot.node.network != null =>
            PacketSender.sendRobotSelectedSlotChange(proxy.robot)
            player.openGui(OpenComputers, GuiType.Robot.id, world, pos.getX, pos.getY, pos.getZ)
          case _ =>
        }
      }
      true
    }
    else if (player.getCurrentEquippedItem == null) {
      if (!world.isRemote) {
        world.getTileEntity(pos) match {
          case proxy: tileentity.RobotProxy if !proxy.machine.isRunning => proxy.machine.start()
          case _ =>
        }
      }
      true
    }
    else false
  }

  override def onBlockPlacedBy(world: World, pos: BlockPos, state: IBlockState, entity: EntityLivingBase, stack: ItemStack) {
    super.onBlockPlacedBy(world, pos, state, entity, stack)
    if (!world.isRemote) ((entity, world.getTileEntity(pos)) match {
      case (player: robot.Player, proxy: tileentity.RobotProxy) =>
        Some((proxy.robot, player.robot.owner, player.robot.ownerUuid))
      case (player: EntityPlayer, proxy: tileentity.RobotProxy) =>
        Some((proxy.robot, player.getName, Option(player.getGameProfile.getId)))
      case _ => None
    }) match {
      case Some((robot, owner, uuid)) =>
        robot.owner = owner
        robot.ownerUuid = Option(robot.determineUUID(uuid))
        robot.info.load(stack)
        robot.bot.node.changeBuffer(robot.info.robotEnergy - robot.bot.node.localBuffer)
        robot.updateInventorySize()
      case _ =>
    }
  }

  override def removedByPlayer(world: World, pos: BlockPos, player: EntityPlayer, willHarvest: Boolean): Boolean = {
    world.getTileEntity(pos) match {
      case proxy: tileentity.RobotProxy =>
        val robot = proxy.robot
        if (!world.isRemote) {
          if (robot.player == player) return false
          robot.node.remove()
          robot.saveComponents()
          InventoryUtils.spawnStackInWorld(BlockPosition(pos, world), robot.info.createItemStack())
        }
        robot.moveFrom.foreach(altPos => if (world.getBlockState(altPos).getBlock == api.Items.get("robotAfterimage").block) {
          world.setBlockState(altPos, net.minecraft.init.Blocks.air.getDefaultState, 1)
        })
      case _ =>
    }
    super.removedByPlayer(world, pos, player, willHarvest)
  }

  override def harvestBlock(world: World, player: EntityPlayer, pos: BlockPos, state: IBlockState, te: TileEntity): Unit = {
    if (moving.get.isEmpty) {
      super.harvestBlock(world, player, pos, state, te)
    }
  }
}
