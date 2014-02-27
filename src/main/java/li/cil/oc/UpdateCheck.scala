package li.cil.oc

import argo.jdom.{JsonNode, JsonRootNode, JdomParser}
import java.net.{HttpURLConnection, URL}
import java.io.{InputStreamReader, BufferedReader}
import argo.saj.InvalidSyntaxException
import cpw.mods.fml.common.network.Player
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.util.ChatMessageComponent
import scala.collection.convert.WrapAsScala._
import cpw.mods.fml.common.versioning.{VersionParser, DefaultArtifactVersion}

class UpdateCheck(var version: String, player: EntityPlayerMP) extends Thread {
  start()

  override def run() {
    try {
    val jsonString: String = getHTML("https://api.github.com/repos/MightyPirates/OpenComputers/releases")
    val parser: JdomParser = new JdomParser
      val n = parser.parse(jsonString)
      val currentNumbers = version.split("\\.")
      val mcVersion = currentNumbers(0)
      val ocVersionCurrent = currentNumbers(1).toInt
      val minorVersionCurrent = currentNumbers(2).toInt

      n.getArrayNode().find(node => node.getStringValue("tag_name").split("v")(1).split("\\.")(0).equals(mcVersion) && !node.getBooleanValue("prerelease")) match {
        case Some(node) =>
          val version = node.getStringValue("tag_name")
          val versions = version.split("v")(1).split("\\.")
          val ocVersion = versions(1).toInt
          val minorVersion = versions(2).toInt
          if (ocVersionCurrent < ocVersion || (ocVersion == ocVersionCurrent && minorVersion > minorVersionCurrent)) {
            player.sendChatToPlayer(ChatMessageComponent.createFromText("[OpenComputers] A new Version " + version + " is available!"))
          }
        case _ =>
      }
    }
    catch {
      //Ignore not connected exceptions and stuff
      case e: Exception =>
    }
  }


  def getHTML(urlToRead: String): String = {
    var url: URL = null
    var conn: HttpURLConnection = null
    var rd: BufferedReader = null
    var line: String = null
    val builder: StringBuilder = new StringBuilder

    url = new URL(urlToRead)
    conn = url.openConnection.asInstanceOf[HttpURLConnection]
    conn.setRequestMethod("GET")
    rd = new BufferedReader(new InputStreamReader(conn.getInputStream))
    while ( {
      line = rd.readLine
      line
    } != null) {
      builder.append(line.trim).append("\n")
    }
    rd.close()
    builder.toString()
  }

}

