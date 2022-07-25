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
import net.minecraft.block.AbstractBlock.Properties
import net.minecraft.block.BlockState
import net.minecraft.block.material.Material
import net.minecraft.client.util.ITooltipFlag
import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.fluid.FluidState
import net.minecraft.item
import net.minecraft.item.ItemStack
import net.minecraft.loot.LootContext
import net.minecraft.loot.LootParameters
import net.minecraft.util.ActionResultType
import net.minecraft.util.Direction
import net.minecraft.util.Hand
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.RayTraceResult
import net.minecraft.util.math.vector.Vector3d
import net.minecraft.util.text.ITextComponent
import net.minecraft.util.text.StringTextComponent
import net.minecraft.world.IBlockReader
import net.minecraft.world.World

import scala.collection.convert.ImplicitConversionsToScala._

class RobotProxy(props: Properties = Properties.of(Material.STONE).strength(2, 10).noOcclusion()) extends RedstoneAware(props) with traits.StateAware {
  setCreativeTab(null)
  ItemBlacklist.hide(this)

  override val getDescriptionId = "robot"

  var moving = new ThreadLocal[Option[tileentity.Robot]] {
    override protected def initialValue = None
  }

  // ----------------------------------------------------------------------- //

  override def getPickBlock(state: BlockState, target: RayTraceResult, world: IBlockReader, pos: BlockPos, player: PlayerEntity): ItemStack =
    world.getBlockEntity(pos) match {
      case proxy: tileentity.RobotProxy => proxy.robot.info.copyItemStack()
      case _ => ItemStack.EMPTY
    }

  override def getBoundingBox(state: BlockState, world: IBlockReader, pos: BlockPos): AxisAlignedBB = {
    world.getBlockEntity(pos) match {
      case proxy: tileentity.RobotProxy =>
        val robot = proxy.robot
        val bounds = new AxisAlignedBB(0.1, 0.1, 0.1, 0.9, 0.9, 0.9)
        if (robot.isAnimatingMove) {
          val remaining = robot.animationTicksLeft.toDouble / robot.animationTicksTotal.toDouble
          val blockPos = robot.moveFrom.get
          val vec = robot.getBlockPos
          val delta = new BlockPos(blockPos.getX - vec.getX, blockPos.getY - vec.getY, blockPos.getZ - vec.getZ)
          bounds.move(delta.getX * remaining, delta.getY * remaining, delta.getZ * remaining)
        }
        else bounds
      case _ => super.getBoundingBox(state, world, pos)
    }
  }

  // ----------------------------------------------------------------------- //

  override def rarity(stack: ItemStack): item.Rarity = {
    val data = new RobotData(stack)
    Rarity.byTier(data.tier)
  }

  override protected def tooltipHead(stack: ItemStack, world: IBlockReader, tooltip: util.List[ITextComponent], advanced: ITooltipFlag) {
    super.tooltipHead(stack, world, tooltip, advanced)
    addLines(stack, tooltip)
  }

  override protected def tooltipBody(stack: ItemStack, world: IBlockReader, tooltip: util.List[ITextComponent], advanced: ITooltipFlag) {
    for (curr <- Tooltip.get("robot")) {
      tooltip.add(new StringTextComponent(curr))
    }
  }

  override protected def tooltipTail(stack: ItemStack, world: IBlockReader, tooltip: util.List[ITextComponent], flag: ITooltipFlag) {
    super.tooltipTail(stack, world, tooltip, flag)
    if (KeyBindings.showExtendedTooltips) {
      val info = new RobotData(stack)
      val components = info.containers ++ info.components
      if (components.length > 0) {
        for (curr <- Tooltip.get("server.Components")) {
          tooltip.add(new StringTextComponent(curr))
        }
        for (component <- components if !component.isEmpty) {
          tooltip.add(new StringTextComponent("- " + component.getDisplayName))
        }
      }
    }
  }

  private def addLines(stack: ItemStack, tooltip: util.List[ITextComponent]) {
    if (stack.hasTag) {
      if (stack.getTag.contains(Settings.namespace + "xp")) {
        val xp = stack.getTag.getDouble(Settings.namespace + "xp")
        val level = Math.min((Math.pow(xp - Settings.get.baseXpToLevel, 1 / Settings.get.exponentialXpGrowth) / Settings.get.constantXpGrowth).toInt, 30)
        if (level > 0) {
          for (curr <- Tooltip.get(getDescriptionId + "_level", level)) {
            tooltip.add(new StringTextComponent(curr))
          }
        }
      }
      if (stack.getTag.contains(Settings.namespace + "storedEnergy")) {
        val energy = stack.getTag.getInt(Settings.namespace + "storedEnergy")
        if (energy > 0) {
          for (curr <- Tooltip.get(getDescriptionId + "_storedenergy", energy)) {
            tooltip.add(new StringTextComponent(curr))
          }
        }
      }
    }
  }

  // ----------------------------------------------------------------------- //

  override def newBlockEntity(world: IBlockReader): tileentity.RobotProxy = {
    moving.get match {
      case Some(robot) => new tileentity.RobotProxy(robot)
      case _ => new tileentity.RobotProxy()
    }
  }

  // ----------------------------------------------------------------------- //

  override def getDrops(state: BlockState, ctx: LootContext.Builder): util.List[ItemStack] = {
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
    ctx.getParameter(LootParameters.BLOCK_ENTITY) match {
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

  // ----------------------------------------------------------------------- //

  override def localOnBlockActivated(world: World, pos: BlockPos, player: PlayerEntity, hand: Hand, heldItem: ItemStack, side: Direction, hitX: Float, hitY: Float, hitZ: Float): Boolean = {
    if (!player.isCrouching) {
      if (!world.isClientSide) {
        // We only send slot changes to nearby players, so if there was no slot
        // change since this player got into range he might have the wrong one,
        // so we send him the current one just in case.
        world.getBlockEntity(pos) match {
          case proxy: tileentity.RobotProxy if proxy.robot.node.network != null =>
            PacketSender.sendRobotSelectedSlotChange(proxy.robot)
            OpenComputers.openGui(player, GuiType.Robot.id, world, pos.getX, pos.getY, pos.getZ)
          case _ =>
        }
      }
      true
    }
    else if (heldItem.isEmpty) {
      if (!world.isClientSide) {
        world.getBlockEntity(pos) match {
          case proxy: tileentity.RobotProxy if !proxy.machine.isRunning && proxy.stillValid(player) => proxy.machine.start()
          case _ =>
        }
      }
      true
    }
    else false
  }

  override def setPlacedBy(world: World, pos: BlockPos, state: BlockState, entity: LivingEntity, stack: ItemStack) {
    super.setPlacedBy(world, pos, state, entity, stack)
    if (!world.isClientSide) ((entity, world.getBlockEntity(pos)) match {
      case (player: agent.Player, proxy: tileentity.RobotProxy) =>
        Some((proxy.robot, player.agent.ownerName, player.agent.ownerUUID))
      case (player: PlayerEntity, proxy: tileentity.RobotProxy) =>
        Some((proxy.robot, player.getName.getString, player.getGameProfile.getId))
      case _ => None
    }) match {
      case Some((robot, owner, uuid)) =>
        robot.ownerName = owner
        robot.ownerUUID = agent.Player.determineUUID(Option(uuid))
        robot.info.loadData(stack)
        robot.bot.node.changeBuffer(robot.info.robotEnergy - robot.bot.node.localBuffer)
        robot.updateInventorySize()
      case _ =>
    }
  }

  override def removedByPlayer(state: BlockState, world: World, pos: BlockPos, player: PlayerEntity, willHarvest: Boolean, fluid: FluidState): Boolean = {
    world.getBlockEntity(pos) match {
      case proxy: tileentity.RobotProxy =>
        val robot = proxy.robot
        // Only allow breaking creative tier robots by allowed users.
        // Unlike normal robots, griefing isn't really a valid concern
        // here, because to get a creative robot you need creative
        // mode in the first place.
        if (robot.isCreative && (!player.isCreative || !robot.canInteract(player.getName.getString))) return false
        if (!world.isClientSide) {
          if (robot.player == player) return false
          robot.node.remove()
          robot.saveComponents()
          InventoryUtils.spawnStackInWorld(BlockPosition(pos, world), robot.info.createItemStack())
        }
        robot.moveFrom.foreach(fromPos => if (world.getBlockState(fromPos).getBlock == api.Items.get(Constants.BlockName.RobotAfterimage).block) {
          world.setBlock(fromPos, net.minecraft.block.Blocks.AIR.defaultBlockState, 1)
        })
      case _ =>
    }
    super.removedByPlayer(state, world, pos, player, willHarvest, fluid)
  }

  override def onRemove(state: BlockState, world: World, pos: BlockPos, newState: BlockState, moved: Boolean): Unit =
    if (moving.get.isEmpty)
      super.onRemove(state, world, pos, newState, moved)
}
