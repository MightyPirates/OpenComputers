package li.cil.oc.integration.agricraft

import com.InfinityRaider.AgriCraft

object ApiHandler {
  lazy val Api = AgriCraft.api.API.getAPI(1) match {
    case api: AgriCraft.api.v1.APIv1 if isApiUsable(api) => Option(api)
    case _ => None
  }

  private def isApiUsable(api: AgriCraft.api.APIBase) = {
    val status = api.getStatus
    status == AgriCraft.api.APIStatus.OK ||
      status == AgriCraft.api.APIStatus.BACKLEVEL_OK ||
      status == AgriCraft.api.APIStatus.BACKLEVEL_LIMITED
  }
}
