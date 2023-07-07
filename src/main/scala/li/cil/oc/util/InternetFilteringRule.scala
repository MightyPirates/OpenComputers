package li.cil.oc.util

import com.google.common.net.InetAddresses
import li.cil.oc.OpenComputers

import java.net.{Inet4Address, Inet6Address, InetAddress}
import scala.collection.mutable

class InternetFilteringRule(val ruleString: String) {
  private var _invalid: Boolean = false
  private val validator: (InetAddress, String) => Option[Boolean] = {
    try {
      val ruleParts = ruleString.split(' ')
      ruleParts.head match {
        case "allow" | "deny" =>
          val value = ruleParts.head.equals("allow")
          val predicates = mutable.MutableList[(InetAddress, String) => Boolean]()
          ruleParts.tail.foreach(f => {
            val filter = f.split(":", 2)
            filter.head match {
              case "default" =>
                if (!value) {
                  predicates += ((_: InetAddress, _: String) => { false })
                } else {
                  predicates += ((inetAddress: InetAddress, host: String) => {
                    InternetFilteringRule.defaultRules.map(r => r.apply(inetAddress, host)).collectFirst({ case Some(r) => r }).getOrElse(false)
                  })
                }
              case "private" =>
                predicates += ((inetAddress: InetAddress, _: String) => {
                  inetAddress.isAnyLocalAddress || inetAddress.isLoopbackAddress || inetAddress.isLinkLocalAddress || inetAddress.isSiteLocalAddress
                })
              case "bogon" =>
                predicates += ((inetAddress: InetAddress, _: String) => {
                  InternetFilteringRule.bogonMatchingRules.exists(rule => rule.matches(inetAddress))
                })
              case "ipv4" =>
                predicates += ((inetAddress: InetAddress, _: String) => {
                  inetAddress.isInstanceOf[Inet4Address]
                })
              case "ipv6" =>
                predicates += ((inetAddress: InetAddress, _: String) => {
                  inetAddress.isInstanceOf[Inet6Address]
                })
              case "ipv4-embedded-ipv6" =>
                predicates += ((inetAddress: InetAddress, _: String) => {
                  inetAddress.isInstanceOf[Inet6Address] && InetAddresses.hasEmbeddedIPv4ClientAddress(inetAddress.asInstanceOf[Inet6Address])
                })
              case "domain" =>
                val domain = filter(1)
                val addresses = InetAddress.getAllByName(domain)
                predicates += ((inetAddress: InetAddress, host: String) => {
                  host == domain || addresses.exists(a => a.equals(inetAddress))
                })
              case "ip" =>
                val ipStringParts = filter(1).split("/", 2)
                if (ipStringParts.length == 2) {
                  val ipRange = InetAddressRange.parse(ipStringParts(0), ipStringParts(1))
                  predicates += ((inetAddress: InetAddress, _: String) => ipRange.matches(inetAddress))
                } else {
                  val ipAddress = InetAddresses.forString(ipStringParts(0))
                  predicates += ((inetAddress: InetAddress, _: String) => ipAddress.equals(inetAddress))
                }
                predicates += ((inetAddress: InetAddress, _: String) => {
                  inetAddress.isAnyLocalAddress || inetAddress.isLoopbackAddress || inetAddress.isLinkLocalAddress || inetAddress.isSiteLocalAddress
                })
              case "all" =>
            }
          })
          (inetAddress: InetAddress, host: String) => {
            if (predicates.forall(p => p(inetAddress, host)))
              Some(value)
            else
              None
          }
        case "removeme" =>
          // Ignore this rule.
          (_: InetAddress, _: String) => None
      }
    } catch {
      case t: Throwable =>
        OpenComputers.log.error("Invalid Internet filteringRules rule in configuration: \"" + ruleString + "\".", t)
        _invalid = true
        (_: InetAddress, _: String) => Some(false)
    }
  }

  def invalid(): Boolean = _invalid

  def apply(inetAddress: InetAddress, host: String) = validator(inetAddress, host)
}

object InternetFilteringRule {
  private val defaultRules = Array(
    new InternetFilteringRule("deny private"),
    new InternetFilteringRule("deny bogon"),
    new InternetFilteringRule("allow all")
  )
  private val bogonMatchingRules = Array(
    "0.0.0.0/8",
    "10.0.0.0/8",
    "100.64.0.0/10",
    "127.0.0.0/8",
    "169.254.0.0/16",
    "172.16.0.0/12",
    "192.0.0.0/24",
    "192.0.2.0/24",
    "192.168.0.0/16",
    "198.18.0.0/15",
    "198.51.100.0/24",
    "203.0.113.0/24",
    "224.0.0.0/3",
    "::/128",
    "::1/128",
    "::ffff:0:0/96",
    "::/96",
    "100::/64",
    "2001:10::/28",
    "2001:db8::/32",
    "fc00::/7",
    "fe80::/10",
    "fec0::/10",
    "ff00::/8"
  ).map(s => s.split("/", 2)).map(s => InetAddressRange.parse(s(0), s(1)))
}