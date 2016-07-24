package li.cil.oc.integration.versionchecker

import li.cil.oc.OpenComputers
import li.cil.oc.integration.ModProxy
import li.cil.oc.integration.Mods
import li.cil.oc.util.UpdateCheck
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.fml.common.event.FMLInterModComms

import scala.concurrent.ExecutionContext.Implicits.global

object ModVersionChecker extends ModProxy {
  override def getMod = Mods.VersionChecker

  override def initialize() {
    UpdateCheck.info onSuccess {
      case Some(release) =>
        val nbt = new NBTTagCompound()
        nbt.setString("newVersion", release.tag_name)
        nbt.setString("updateUrl", "https://github.com/MightyPirates/OpenComputers/releases")
        nbt.setBoolean("isDirectLink", false)
        if (release.body != null) {
          nbt.setString("changeLog", release.body.replaceAll("\r\n", "\n"))
        }
        FMLInterModComms.sendRuntimeMessage(OpenComputers.ID, Mods.IDs.VersionChecker, "addUpdate", nbt)
    }
  }
}
