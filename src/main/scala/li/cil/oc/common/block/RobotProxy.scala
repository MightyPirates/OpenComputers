package li.cil.oc.common.block

import java.util

import li.cil.oc.Constants
import li.cil.oc.OpenComputers
import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.client.KeyBindings
import li.cil.oc.common.GuiType
import li.cil.oc.common.item.data.RobotData
import li.cil.oc.common.tileentity
import li.cil.oc.integration.util.ItemBlacklist
import li.cil.oc.server.PacketSender
import li.cil.oc.server.agent
import li.cil.oc.util.BlockPosition
import li.cil.oc.util.InventoryUtils
import li.cil.oc.util.Rarity
import li.cil.oc.util.Tooltip
import net.minecraft.block.state.IBlockState
import net.minecraft.client.util.ITooltipFlag
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.EnumRarity
import net.minecraft.item.ItemStack
import net.minecraft.util._
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.RayTraceResult
import net.minecraft.util.math.Vec3d
import net.minecraft.world.IBlockAccess
import net.minecraft.world.World

class RobotProxy extends RedstoneAware with traits.StateAware {
  setLightOpacity(0)
  setCreativeTab(null)
  ItemBlacklist.hide(this)

  override val getTranslationKey = "robot"

  var moving = new ThreadLocal[Option[tileentity.Robot]] {
    override protected def initialValue = None
  }

  // ----------------------------------------------------------------------- //

  override def isOpaqueCube(state: IBlockState): Boolean = false

  override def isFullCube(state: IBlockState): Boolean = false

  override def shouldSideBeRendered(state: IBlockState, world: IBlockAccess, pos: BlockPos, side: EnumFacing) = false

  override def isBlockSolid(world: IBlockAccess, pos: BlockPos, side: EnumFacing) = false

  override def isSideSolid(state: IBlockState, world: IBlockAccess, pos: BlockPos, side: EnumFacing) = false

  override def getPickBlock(state: IBlockState, target: RayTraceResult, world: World, pos: BlockPos, player: EntityPlayer): ItemStack =
    world.getTileEntity(pos) match {
      case proxy: tileentity.RobotProxy => proxy.robot.info.copyItemStack()
      case _ => ItemStack.EMPTY
    }

  override def getBoundingBox(state: IBlockState, world: IBlockAccess, pos: BlockPos): AxisAlignedBB = {
    world.getTileEntity(pos) match {
      case proxy: tileentity.RobotProxy =>
        val robot = proxy.robot
        val bounds = new AxisAlignedBB(0.1, 0.1, 0.1, 0.9, 0.9, 0.9)
        if (robot.isAnimatingMove) {
          val remaining = robot.animationTicksLeft.toDouble / robot.animationTicksTotal.toDouble
          val blockPos = robot.moveFrom.get
          val vec = robot.getPos
          val delta = new BlockPos(blockPos.getX - vec.getX, blockPos.getY - vec.getY, blockPos.getZ - vec.getZ)
          bounds.offset(delta.getX * remaining, delta.getY * remaining, delta.getZ * remaining)
        }
        else bounds
      case _ => super.getBoundingBox(state, world, pos)
    }
  }

  // ----------------------------------------------------------------------- //

  override def rarity(stack: ItemStack): EnumRarity = {
    val data = new RobotData(stack)
    Rarity.byTier(data.tier)
  }

  override protected def tooltipHead(metadata: Int, stack: ItemStack, world: World, tooltip: util.List[String], advanced: ITooltipFlag) {
    super.tooltipHead(metadata, stack, world, tooltip, advanced)
    addLines(stack, tooltip)
  }

  override protected def tooltipBody(metadata: Int, stack: ItemStack, world: World, tooltip: util.List[String], advanced: ITooltipFlag) {
    tooltip.addAll(Tooltip.get("robot"))
  }

  override protected def tooltipTail(metadata: Int, stack: ItemStack, world: World, tooltip: util.List[String], flag: ITooltipFlag) {
    super.tooltipTail(metadata, stack, world, tooltip, flag)
    if (KeyBindings.showExtendedTooltips) {
      val info = new RobotData(stack)
      val components = info.containers ++ info.components
      if (components.length > 0) {
        tooltip.addAll(Tooltip.get("server.Components"))
        for (component <- components if !component.isEmpty) {
          tooltip.add("- " + component.getDisplayName)
        }
      }
    }
  }

  private def addLines(stack: ItemStack, tooltip: util.List[String]) {
    if (stack.hasTagCompound) {
      if (stack.getTagCompound.hasKey(Settings.namespace + "xp")) {
        val xp = stack.getTagCompound.getDouble(Settings.namespace + "xp")
        val level = Math.min((Math.pow(xp - Settings.get.baseXpToLevel, 1 / Settings.get.exponentialXpGrowth) / Settings.get.constantXpGrowth).toInt, 30)
        if (level > 0) {
          tooltip.addAll(Tooltip.get(getTranslationKey + "_level", level))
        }
      }
      if (stack.getTagCompound.hasKey(Settings.namespace + "storedEnergy")) {
        val energy = stack.getTagCompound.getInteger(Settings.namespace + "storedEnergy")
        if (energy > 0) {
          tooltip.addAll(Tooltip.get(getTranslationKey + "_storedenergy", energy))
        }
      }
    }
  }

  // ----------------------------------------------------------------------- //

  override def createNewTileEntity(world: World, metadata: Int): tileentity.RobotProxy = {
    moving.get match {
      case Some(robot) => new tileentity.RobotProxy(robot)
      case _ => new tileentity.RobotProxy()
    }
  }

  // ----------------------------------------------------------------------- //

  override def getExplosionResistance(entity: Entity) = 10f

  override def getDrops(world: IBlockAccess, pos: BlockPos, state: IBlockState, fortune: Int): util.ArrayList[ItemStack] = {
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

  override def collisionRayTrace(state: IBlockState, world: World, pos: BlockPos, start: Vec3d, end: Vec3d): RayTraceResult = {
    val bounds = getCollisionBoundingBox(state, world, pos)
    world.getTileEntity(pos) match {
      case proxy: tileentity.RobotProxy if proxy.robot.animationTicksLeft <= 0 && bounds.contains(start) => null
      case _ => super.collisionRayTrace(state, world, pos, start, end)
    }
  }

  // ----------------------------------------------------------------------- //

  override def localOnBlockActivated(world: World, pos: BlockPos, player: EntityPlayer, hand: EnumHand, heldItem: ItemStack, side: EnumFacing, hitX: Float, hitY: Float, hitZ: Float): Boolean = {
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
    else if (heldItem.isEmpty) {
      if (!world.isRemote) {
        world.getTileEntity(pos) match {
          case proxy: tileentity.RobotProxy if !proxy.machine.isRunning && proxy.isUsableByPlayer(player) => proxy.machine.start()
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
      case (player: agent.Player, proxy: tileentity.RobotProxy) =>
        Some((proxy.robot, player.agent.ownerName, player.agent.ownerUUID))
      case (player: EntityPlayer, proxy: tileentity.RobotProxy) =>
        Some((proxy.robot, player.getName, player.getGameProfile.getId))
      case _ => None
    }) match {
      case Some((robot, owner, uuid)) =>
        robot.ownerName = owner
        robot.ownerUUID = agent.Player.determineUUID(Option(uuid))
        robot.info.load(stack)
        robot.bot.node.changeBuffer(robot.info.robotEnergy - robot.bot.node.localBuffer)
        robot.updateInventorySize()
      case _ =>
    }
  }

  override def removedByPlayer(state: IBlockState, world: World, pos: BlockPos, player: EntityPlayer, willHarvest: Boolean): Boolean = {
    world.getTileEntity(pos) match {
      case proxy: tileentity.RobotProxy =>
        val robot = proxy.robot
        // Only allow breaking creative tier robots by allowed users.
        // Unlike normal robots, griefing isn't really a valid concern
        // here, because to get a creative robot you need creative
        // mode in the first place.
        if (robot.isCreative && (!player.capabilities.isCreativeMode || !robot.canInteract(player.getName))) return false
        if (!world.isRemote) {
          if (robot.player == player) return false
          robot.node.remove()
          robot.saveComponents()
          InventoryUtils.spawnStackInWorld(BlockPosition(pos, world), robot.info.createItemStack())
        }
        robot.moveFrom.foreach(fromPos => if (world.getBlockState(fromPos).getBlock == api.Items.get(Constants.BlockName.RobotAfterimage).block) {
          world.setBlockState(fromPos, net.minecraft.init.Blocks.AIR.getDefaultState, 1)
        })
      case _ =>
    }
    super.removedByPlayer(state, world, pos, player, willHarvest)
  }

  override def breakBlock(world: World, pos: BlockPos, state: IBlockState): Unit =
    if (moving.get.isEmpty)
      super.breakBlock(world, pos, state)
}
