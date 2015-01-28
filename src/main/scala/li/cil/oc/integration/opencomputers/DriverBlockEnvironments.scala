package li.cil.oc.integration.opencomputers

import li.cil.oc.api
import li.cil.oc.api.driver
import li.cil.oc.api.driver.EnvironmentAware
import li.cil.oc.api.network.Environment
import li.cil.oc.common
import li.cil.oc.common.tileentity
import li.cil.oc.integration.util.BundledRedstone
import li.cil.oc.server.component
import li.cil.oc.server.machine.Machine
import net.minecraft.block.Block
import net.minecraft.item.ItemBlock
import net.minecraft.item.ItemStack
import net.minecraft.util.BlockPos
import net.minecraft.world.World

/**
 * Provide static environment lookup for blocks that are components.
 * This allows showing their documentation in NEI, for example. Not
 * all blocks are present here, because some also serve as upgrades
 * and therefore have item drivers.
 */
object DriverBlockEnvironments extends driver.Block with EnvironmentAware {
  override def worksWith(world: World, pos: BlockPos) = false

  override def createEnvironment(world: World, pos: BlockPos) = null

  override def providedEnvironment(stack: ItemStack): Class[_ <: Environment] = stack.getItem match {
    case block: ItemBlock if block.getBlock != null =>
      if (isOneOf(block.getBlock, "accessPoint")) classOf[tileentity.AccessPoint]
      else if (isOneOf(block.getBlock, "assembler")) classOf[tileentity.Assembler]
      else if (isOneOf(block.getBlock, "case1", "case2", "case3", "caseCreative", "microcontroller")) classOf[Machine]
      else if (isOneOf(block.getBlock, "hologram1", "hologram2")) classOf[tileentity.Hologram]
      else if (isOneOf(block.getBlock, "motionSensor")) classOf[tileentity.MotionSensor]
      else if (isOneOf(block.getBlock, "redstone")) if (BundledRedstone.isAvailable) classOf[component.Redstone.Bundled] else classOf[component.Redstone.Vanilla]
      else if (isOneOf(block.getBlock, "screen1")) classOf[common.component.TextBuffer].asInstanceOf[Class[_ <: Environment]]
      else if (isOneOf(block.getBlock, "screen2", "screen3")) classOf[common.component.Screen]
      else if (isOneOf(block.getBlock, "robot")) classOf[component.robot.Robot].asInstanceOf[Class[_ <: Environment]]
      else if (isOneOf(block.getBlock, "drone")) classOf[component.Drone].asInstanceOf[Class[_ <: Environment]]
      else null
    case _ => null
  }

  private def isOneOf(block: Block, names: String*) = names.exists(api.Items.get(_).block == block)
}
