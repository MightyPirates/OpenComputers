package li.cil.oc.integration.util

import mrtjp.projectred.api.ProjectRedAPI

object ProjectRed {
  def isAPIAvailable = classOf[ProjectRedAPI].getFields.exists(_.getName == "transmissionAPI")
}
