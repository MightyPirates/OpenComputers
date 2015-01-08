package li.cil.oc.common.tileentity.traits

import java.util

import li.cil.oc.common.asm.Injectable
import li.cil.oc.integration.Mods

@Injectable.Interface(value = "buildcraft.api.tiles.IHasWork", modid = Mods.IDs.BuildCraft)
trait StateAware {
  def currentState: util.EnumSet[State]

/* TODO BuildCraft
  @Optional.Method(modid = Mods.IDs.BuildCraft)
  def hasWork: Boolean = currentState.contains(State.CanWork) || currentState.contains(State.IsWorking)
*/
}
