package li.cil.oc.util.mods

import cpw.mods.fml.common.Loader
import mrtjp.projectred.api.ProjectRedAPI

object ProjectRed {
  def isAvailable = Loader.isModLoaded("ProjRed|Transmission")

  def isAPIAvailable = classOf[ProjectRedAPI].getFields.exists(_.getName == "transmissionAPI")
}
