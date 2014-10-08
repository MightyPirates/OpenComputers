package li.cil.oc.server.driver.block

import li.cil.oc.api
import li.cil.oc.api.driver
import li.cil.oc.api.driver.EnvironmentAware
import li.cil.oc.api.network.Environment
import li.cil.oc.common
import li.cil.oc.common.tileentity
import li.cil.oc.server.component
import li.cil.oc.server.machine.Machine
import li.cil.oc.util.mods.BundledRedstone
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
object EnvironmentProvider extends driver.Block with EnvironmentAware {
  override def worksWith(world: World, x: Int, y: Int, z: Int) = false

  override def createEnvironment(world: World, x: Int, y: Int, z: Int) = null

  override def providedEnvironment(stack: ItemStack): Class[_ <: Environment] = stack.getItem match {
    case block: ItemBlock =>
      if (isOneOf(block.field_150939_a, "accessPoint")) classOf[tileentity.AccessPoint]
      else if (isOneOf(block.field_150939_a, "robotAssembler")) classOf[tileentity.Assembler]
      else if (isOneOf(block.field_150939_a, "case1", "case2", "case3", "caseCreative")) classOf[Machine]
      else if (isOneOf(block.field_150939_a, "hologram1", "hologram2")) classOf[tileentity.Hologram]
      else if (isOneOf(block.field_150939_a, "motionSensor")) classOf[tileentity.MotionSensor]
      else if (isOneOf(block.field_150939_a, "redstone")) if (BundledRedstone.isAvailable) classOf[component.Redstone.Bundled] else classOf[component.Redstone.Simple]
      else if (isOneOf(block.field_150939_a, "screen2", "screen3")) classOf[common.component.Screen]
      else null
    case _ => null
  }

  private def isOneOf(block: Block, names: String*) = names.exists(api.Items.get(_).block == block)
}
