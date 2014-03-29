package li.cil.oc.util.mods

import mrtjp.projectred.api.ProjectRedAPI

object ProjectRed {
  def isAPIAvailable = classOf[ProjectRedAPI].getFields.exists(_.getName == "transmissionAPI")
}
