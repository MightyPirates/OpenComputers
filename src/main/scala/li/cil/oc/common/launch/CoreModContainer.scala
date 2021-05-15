package li.cil.oc.common.launch

import com.google.common.eventbus.EventBus
import net.minecraftforge.fml.common.DummyModContainer
import net.minecraftforge.fml.common.LoadController
import net.minecraftforge.fml.common.ModMetadata

class CoreModContainer extends DummyModContainer({
  val md = new ModMetadata()
  md.authorList.add("Sangar")
  md.modId = "opencomputers|core"
  md.version = "@VERSION@"
  md.name = "OpenComputers (Core)"
  md.url = "https://oc.cil.li/"
  md.description = "OC core mod used for class transformer and as API owner to avoid cyclic dependencies."
  md
}) {
  override def registerBus(bus: EventBus, controller: LoadController) = true
}
