package li.cil.oc.integration.opencomputers

import li.cil.oc.Constants
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
      if (isOneOf(block.field_150939_a, Constants.BlockName.AccessPoint)) classOf[tileentity.AccessPoint]
      else if (isOneOf(block.field_150939_a, Constants.BlockName.Assembler)) classOf[tileentity.Assembler]
      else if (isOneOf(block.field_150939_a, Constants.BlockName.CaseTier1, Constants.BlockName.CaseTier2, Constants.BlockName.CaseTier3, Constants.BlockName.CaseCreative, Constants.BlockName.Microcontroller)) classOf[Machine]
      else if (isOneOf(block.field_150939_a, Constants.BlockName.HologramTier1, Constants.BlockName.HologramTier2)) classOf[tileentity.Hologram]
      else if (isOneOf(block.field_150939_a, Constants.BlockName.MotionSensor)) classOf[tileentity.MotionSensor]
      else if (isOneOf(block.field_150939_a, Constants.BlockName.Redstone)) if (BundledRedstone.isAvailable) classOf[component.Redstone.Bundled] else classOf[component.Redstone.Vanilla]
      else if (isOneOf(block.field_150939_a, Constants.BlockName.ScreenTier1)) classOf[common.component.TextBuffer]: Class[_ <: Environment]
      else if (isOneOf(block.field_150939_a, Constants.BlockName.ScreenTier2, Constants.BlockName.ScreenTier3)) classOf[common.component.Screen]
      else if (isOneOf(block.field_150939_a, Constants.BlockName.Robot)) classOf[component.Robot]: Class[_ <: Environment]
      else null
    case _ =>
      if (api.Items.get(stack) == api.Items.get(Constants.ItemName.Drone)) classOf[component.Drone]: Class[_ <: Environment]
      else null
  }

  private def isOneOf(block: Block, names: String*) = names.exists(api.Items.get(_).block == block)
}
