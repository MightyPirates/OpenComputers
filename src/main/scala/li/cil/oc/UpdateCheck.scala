package li.cil.oc

import java.net.{HttpURLConnection, URL}

import cpw.mods.fml.common.Loader
import cpw.mods.fml.common.versioning.ComparableVersion
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.util.{ChatComponentText, ChatComponentTranslation}
import org.apache.logging.log4j.Level

import scala.io.Source
import scala.util.parsing.json.{JSON, JSONArray, JSONObject}

object UpdateCheck {
  val releasesUrl = new URL("https://api.github.com/repos/MightyPirates/OpenComputers/releases")

  val version = Loader.instance.getIndexedModList.get("OpenComputers").getVersion
  val majorVersion = try version.split('.')(0).toInt catch {
    case _: Throwable => 0
  }

  // Lazy to make initialize() execute once from the first thread that tries to
  // read it. If other threads are spawned while it's running they will wait,
  // because lazy initializers are synchronized.
  lazy val result = initialize()

  def checkForPlayer(player: EntityPlayerMP) = if (Settings.get.updateCheck) {
    new Thread() {
      override def run() = result(player)
    }.start()
  }

  def initialize(): EntityPlayerMP => Unit = {
    try {
      OpenComputers.log.info("Starting version check.")
      releasesUrl.openConnection match {
        case conn: HttpURLConnection =>
          conn.setRequestMethod("GET")
          conn.setDoOutput(false)
          JSON.parseRaw(Source.fromInputStream(conn.getInputStream).mkString) match {
            case Some(array: JSONArray) =>
              val candidates = array.list.filter(_.isInstanceOf[JSONObject]).map(node => {
                val obj = node.asInstanceOf[JSONObject].obj
                (obj("tag_name").asInstanceOf[String], !obj("prerelease").asInstanceOf[Boolean])
              }).filter {
                case (tag, isRelease) => matchesVersion(tag) && isRelease
              }
              if (candidates.nonEmpty) {
                val newest = candidates.maxBy {
                  case (tagName, _) => new ComparableVersion(tagName.stripPrefix("v"))
                }
                val tag = newest._1
                val tagVersion = new ComparableVersion(tag.stripPrefix("v"))
                val modVersion = new ComparableVersion(version)
                if (tagVersion.compareTo(modVersion) > 0) {
                  OpenComputers.log.info(s"A newer version is available: ($tag})")
                  return (player: EntityPlayerMP) =>
                    player.addChatMessage(new ChatComponentText("§aOpenComputers§f: ").appendSibling(
                      new ChatComponentTranslation(Settings.namespace + "gui.Chat.NewVersion", tag)))
                }
              }
              OpenComputers.log.info("Running the latest version.")
            case _ => OpenComputers.log.warn("Unexpected response from Github.")
          }
        case _ => OpenComputers.log.warn("Failed to connect to Github.")
      }
    }
    catch {
      case t: Throwable => OpenComputers.log.log(Level.WARN, "Update check failed.", t)
    }
    // Nothing to do, return dummy callback.
    p =>
  }

  def matchesVersion(tag: String) = try {
    tag.stripPrefix("v").split('.')(0).toInt == majorVersion
  }
  catch {
    case _: Throwable => false
  }

}
