package li.cil.oc.util

import java.io.InputStreamReader
import java.net.URL

import com.google.gson.Gson
import com.google.gson.stream.JsonReader
import cpw.mods.fml.common.Loader
import cpw.mods.fml.common.versioning.ComparableVersion
import li.cil.oc.OpenComputers
import li.cil.oc.Settings

import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object UpdateCheck {
  private val releasesUrl = new URL("https://api.github.com/repos/MightyPirates/OpenComputers/releases")

  var info = Future {
    initialize()
  }

  private def initialize(): Option[Release] = {
    // Keep the version template split up so it's not replaced with the actual version...
    if (Settings.get.updateCheck && OpenComputers.Version != ("@" + "VERSION" + "@")) {
      try {
        OpenComputers.log.info("Starting OpenComputers version check.")
        val reader = new JsonReader(new InputStreamReader(releasesUrl.openStream()))
        reader.beginArray()
        val candidates = mutable.ArrayBuffer.empty[Release]
        while (reader.hasNext) {
          val release: Release = new Gson().fromJson(reader, classOf[Release])
          if (!release.prerelease) {
            candidates += release
          }
        }
        reader.endArray()
        if (candidates.nonEmpty) {
          val latest = candidates.maxBy(release => new ComparableVersion(release.tag_name.stripPrefix("v")))
          val remoteVersion = new ComparableVersion(latest.tag_name.stripPrefix("v"))
          val localVersion = new ComparableVersion(Loader.instance.getIndexedModList.get(OpenComputers.ID).getVersion)
          if (remoteVersion.compareTo(localVersion) > 0) {
            OpenComputers.log.info(s"A newer version of OpenComputers is available: ${latest.tag_name}.")
            return Some(latest)
          }
        }
        OpenComputers.log.info("Running the latest OpenComputers version.")
      }
      catch {
        case t: Throwable => OpenComputers.log.warn("Update check for OpenComputers failed.", t)
      }
    }
    None
  }

  class Release {
    var tag_name = ""
    var body = ""
    var prerelease = false
  }

}
