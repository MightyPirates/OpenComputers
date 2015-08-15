package li.cil.oc.common.tileentity.traits

import cpw.mods.fml.common.Optional
import li.cil.oc.api.internal
import li.cil.oc.common.asm.Injectable
import li.cil.oc.integration.Mods

@Injectable.Interface(value = "buildcraft.api.tiles.IHasWork", modid = Mods.IDs.BuildCraft)
trait StateAware extends internal.StateAware {
  @Optional.Method(modid = Mods.IDs.BuildCraft)
  def hasWork: Boolean = getCurrentState.contains(internal.StateAware.State.CanWork) || getCurrentState.contains(internal.StateAware.State.IsWorking)
}
