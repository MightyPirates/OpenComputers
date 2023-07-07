import com.typesafe.config.ConfigFactory
import li.cil.oc.Settings
import li.cil.oc.server.component.InternetCard
import org.junit.runner.RunWith
import org.scalatest.{FlatSpec, FunSpec, WordSpec}
import org.scalatest.Matchers.{be, convertToAnyShouldWrapper}
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar

import java.net.InetAddress
import scala.compat.Platform.EOL
import scala.io.{Codec, Source}

@RunWith(classOf[JUnitRunner])
class InternetFilteringRuleTest extends FunSpec with MockitoSugar {
  val config = autoClose(classOf[Settings].getResourceAsStream("/application.conf")) { in =>
    val configStr = Source.fromInputStream(in)(Codec.UTF8).getLines().mkString("", EOL, EOL)
    ConfigFactory.parseString(configStr)
  }
  val settings = new Settings(config.getConfig("opencomputers"))


  describe("The default AddressValidators") {
    // Many of these payloads are pulled from PayloadsAllTheThings
    // https://github.com/swisskyrepo/PayloadsAllTheThings/blob/master/Server%20Side%20Request%20Forgery/README.md
    it("should accept a valid external address") {
      isUriBlacklisted("https://google.com") should be(false)
    }
    it("should reject localhost") {
      isUriBlacklisted("http://localhost") should be(true)
    }
    it("should reject the local host in IPv4 format") {
      isUriBlacklisted("http://127.0.0.1") should be(true)
      isUriBlacklisted("http://127.0.1") should be(true)
      isUriBlacklisted("http://127.1") should be(true)
      isUriBlacklisted("http://0") should be (true)
    }
    it("should reject the local host in IPv6") {
      isUriBlacklisted("http://[::1]") should be(true)
      isUriBlacklisted("http://[::]") should be(true)
    }
    it("should reject IPv6/IPv4 Address Embedding") {
      isUriBlacklisted("http://[0:0:0:0:0:ffff:127.0.0.1]") should be(true)
      isUriBlacklisted("http://[::ffff:127.0.0.1]") should be(true)
    }
    it("should reject an attempt to bypass using a decimal IP location") {
      isUriBlacklisted("http://2130706433") should be(true) // 127.0.0.1
      isUriBlacklisted("http://3232235521") should be(true) // 192.168.0.1
      isUriBlacklisted("http://3232235777") should be(true) // 192.168.1.1
    }
    it("should reject the IMDS address in IPv4 format") {
      isUriBlacklisted("http://169.254.169.254") should be(true)
      isUriBlacklisted("http://2852039166") should be(true) // 169.254.169.254
    }
    it("should reject the IMDS address in IPv6 format") {
      isUriBlacklisted("http://[fd00:ec2::254]") should be(true)
    }
    it("should reject the IMDS in for Oracle Cloud") {
      isUriBlacklisted("http://192.0.0.192") should be(true)
    }
    it("should reject the IMDS in for Alibaba Cloud") {
      isUriBlacklisted("http://100.100.100.200") should be(true)
    }
  }

  def isUriBlacklisted(uri: String): Boolean = {
    val uriObj = new java.net.URI(uri)
    val resolved = InetAddress.getByName(uriObj.getHost)
    !InternetCard.isRequestAllowed(settings, resolved, uriObj.getHost)
  }

  def autoClose[A <: AutoCloseable, B](closeable: A)(fun: (A) ⇒ B): B = {
    var t: Throwable = null
    try {
      fun(closeable)
    } catch {
      case funT: Throwable ⇒
        t = funT
        throw t
    } finally {
      if (t != null) {
        try {
          closeable.close()
        } catch {
          case closeT: Throwable ⇒
            t.addSuppressed(closeT)
            throw t
        }
      } else {
        closeable.close()
      }
    }
  }

}
