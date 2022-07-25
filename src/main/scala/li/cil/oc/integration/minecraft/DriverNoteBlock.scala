package li.cil.oc.integration.minecraft

import li.cil.oc.api.Network
import li.cil.oc.api.driver.DriverBlock
import li.cil.oc.api.driver.EnvironmentProvider
import li.cil.oc.api.driver.NamedBlock
import li.cil.oc.api.machine.Arguments
import li.cil.oc.api.machine.Callback
import li.cil.oc.api.machine.Context
import li.cil.oc.api.network.ManagedEnvironment
import li.cil.oc.api.network.Visibility
import li.cil.oc.api.prefab.AbstractManagedEnvironment
import li.cil.oc.util.ResultWrapper.result
import net.minecraft.block.Block
import net.minecraft.block.Blocks
import net.minecraft.block.NoteBlock
import net.minecraft.item.ItemStack
import net.minecraft.util.Direction
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World


object DriverNoteBlock extends DriverBlock {
  override def worksWith(world: World, pos: BlockPos, side: Direction) = world.getBlockState(pos).is(Blocks.NOTE_BLOCK)

  override def createEnvironment(world: World, pos: BlockPos, side: Direction): ManagedEnvironment =
    new Environment(world, pos)

  final class Environment(val world: World, val pos: BlockPos) extends AbstractManagedEnvironment with NamedBlock {
    setNode(Network.newNode(this, Visibility.Network).
      withComponent(preferredName).
      create())

    override def preferredName = "note_block"

    override def priority = 0

    @Callback(direct = true, doc = "function():number -- Get the currently set pitch on this note block.")
    def getPitch(context: Context, args: Arguments): Array[AnyRef] = {
      val state = world.getBlockState(pos)
      if (!state.is(Blocks.NOTE_BLOCK)) {
        throw new IllegalArgumentException("block removed")
      }
      result(state.getValue(NoteBlock.NOTE).intValue + 1)
    }

    @Callback(doc = "function(value:number) -- Set the pitch for this note block. Must be in the interval [1, 25].")
    def setPitch(context: Context, args: Arguments): Array[AnyRef] = {
      setPitch(args.checkInteger(0))
      result(true)
    }

    @Callback(doc = "function([pitch:number]):boolean -- Triggers the note block if possible. Allows setting the pitch for to save a tick.")
    def trigger(context: Context, args: Arguments): Array[AnyRef] = {
      if (args.count > 0 && args.checkAny(0) != null) {
        setPitch(args.checkInteger(0))
      }
      else {
        val state = world.getBlockState(pos)
        if (!state.is(Blocks.NOTE_BLOCK)) {
          throw new IllegalArgumentException("block removed")
        }
      }
      val canTrigger = world.isEmptyBlock(pos.above)
      if (canTrigger) world.blockEvent(pos, Blocks.NOTE_BLOCK, 0, 0)
      result(canTrigger)
    }

    private def setPitch(value: Int): Unit = {
      val pitch = Int.box(value - 1)
      if (!NoteBlock.NOTE.getPossibleValues.contains(pitch)) {
        throw new IllegalArgumentException("invalid pitch")
      }
      val state = world.getBlockState(pos)
      if (!state.is(Blocks.NOTE_BLOCK)) {
        throw new IllegalArgumentException("block removed")
      }
      val newState = state.setValue(NoteBlock.NOTE, pitch)
      if (newState != state) world.setBlock(pos, newState, 3)
    }
  }

  object Provider extends EnvironmentProvider {
    override def getEnvironment(stack: ItemStack): Class[_] = {
      if (!stack.isEmpty && Block.byItem(stack.getItem) == Blocks.NOTE_BLOCK)
        classOf[Environment]
      else null
    }
  }

}
