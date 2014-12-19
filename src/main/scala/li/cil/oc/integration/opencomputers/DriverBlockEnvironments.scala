package li.cil.oc.integration.opencomputers

import li.cil.oc.api
import li.cil.oc.api.driver
import li.cil.oc.api.driver.EnvironmentAware
import li.cil.oc.api.network.Environment
import li.cil.oc.api.network.ManagedEnvironment
import li.cil.oc.common
import li.cil.oc.common.tileentity
import li.cil.oc.integration.util.BundledRedstone
import li.cil.oc.server.component
import li.cil.oc.server.machine.Machine
import net.minecraft.block.Block
import net.minecraft.item.ItemBlock
import net.minecraft.item.ItemStack
import net.minecraft.world.World

/**
 * Provide static environment lookup for blocks that are components.
 * This allows showing their documentation in NEI, for example. Not
 * all blocks are present here, because some also serve as upgrades
 * and therefore have item drivers.
 */
object DriverBlockEnvironments extends driver.Block with EnvironmentAware {
  override def worksWith(world: World, x: Int, y: Int, z: Int) = false

  override def createEnvironment(world: World, x: Int, y: Int, z: Int) = null

  override def providedEnvironment(stack: ItemStack): Class[_ <: Environment] = stack.getItem match {
    case block: ItemBlock if block.field_150939_a != null =>
      if (isOneOf(block.field_150939_a, "accessPoint")) classOf[tileentity.AccessPoint]
      else if (isOneOf(block.field_150939_a, "assembler")) classOf[tileentity.Assembler]
      else if (isOneOf(block.field_150939_a, "case1", "case2", "case3", "caseCreative", "microcontroller")) classOf[Machine]
      else if (isOneOf(block.field_150939_a, "hologram1", "hologram2")) classOf[tileentity.Hologram]
      else if (isOneOf(block.field_150939_a, "motionSensor")) classOf[tileentity.MotionSensor]
      else if (isOneOf(block.field_150939_a, "redstone")) if (BundledRedstone.isAvailable) classOf[component.Redstone.Bundled] else classOf[component.Redstone.Simple]
      else if (isOneOf(block.field_150939_a, "screen1")) classOf[common.component.TextBuffer].asInstanceOf[Class[_ <: Environment]]
      else if (isOneOf(block.field_150939_a, "screen2", "screen3")) classOf[common.component.Screen]
      else if (isOneOf(block.field_150939_a, "robot")) classOf[component.robot.Robot].asInstanceOf[Class[_ <: Environment]]
      else if (isOneOf(block.field_150939_a, "drone")) classOf[component.Drone].asInstanceOf[Class[_ <: Environment]]
      else null
    case _ => null
  }

  private def isOneOf(block: Block, names: String*) = names.exists(api.Items.get(_).block == block)
}
