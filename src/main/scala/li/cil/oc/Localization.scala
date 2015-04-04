package li.cil.oc

import cpw.mods.fml.common.event.FMLFingerprintViolationEvent
import net.minecraft.util.ChatComponentText
import net.minecraft.util.ChatComponentTranslation
import net.minecraft.util.StatCollector

import scala.util.matching.Regex

object Localization {
  private val nl = Regex.quote("[nl]")

  private def resolveKey(key: String) = if (canLocalize(Settings.namespace + key)) Settings.namespace + key else key

  def canLocalize(key: String) = StatCollector.canTranslate(key)

  def localizeLater(formatKey: String, values: AnyRef*) = new ChatComponentTranslation(resolveKey(formatKey), values: _*)

  def localizeLater(key: String) = new ChatComponentTranslation(resolveKey(key))

  def localizeImmediately(formatKey: String, values: AnyRef*) = StatCollector.translateToLocalFormatted(resolveKey(formatKey), values: _*).split(nl).map(_.trim).mkString("\n")

  def localizeImmediately(key: String) = StatCollector.translateToLocal(resolveKey(key)).split(nl).map(_.trim).mkString("\n")

  object Analyzer {
    def Address(value: String) = localizeLater("gui.Analyzer.Address", value)

    def AddressCopied = localizeLater("gui.Analyzer.AddressCopied")

    def ChargerSpeed(value: Double) = localizeLater("gui.Analyzer.ChargerSpeed", (value * 100).toInt + "%")

    def ComponentName(value: String) = localizeLater("gui.Analyzer.ComponentName", value)

    def Components(count: Int, maxCount: Int) = localizeLater("gui.Analyzer.Components", count + "/" + maxCount)

    def LastError(value: String) = localizeLater("gui.Analyzer.LastError", localizeLater(value))

    def RobotOwner(owner: String) = localizeLater("gui.Analyzer.RobotOwner", owner)

    def RobotName(name: String) = localizeLater("gui.Analyzer.RobotName", name)

    def RobotXp(experience: Double, level: Int) = localizeLater("gui.Analyzer.RobotXp", f"$experience%.2f", level.toString)

    def StoredEnergy(value: String) = localizeLater("gui.Analyzer.StoredEnergy", value)

    def TotalEnergy(value: String) = localizeLater("gui.Analyzer.TotalEnergy", value)

    def Users(list: Iterable[String]) = localizeLater("gui.Analyzer.Users", list.mkString(", "))

    def WirelessStrength(value: Double) = localizeLater("gui.Analyzer.WirelessStrength", value.toInt.toString)
  }

  object Assembler {
    def InsertTemplate = localizeImmediately("gui.Assembler.InsertCase")

    def CollectResult = localizeImmediately("gui.Assembler.Collect")

    def InsertCPU = localizeLater("gui.Assembler.InsertCPU")

    def InsertRAM = localizeLater("gui.Assembler.InsertRAM")

    def Complexity(complexity: Int, maxComplexity: Int) = {
      val message = localizeLater("gui.Assembler.Complexity", complexity.toString, maxComplexity.toString)
      if (complexity > maxComplexity) new ChatComponentText("§4").appendSibling(message)
      else message
    }

    def Run = localizeImmediately("gui.Assembler.Run")

    def Progress(progress: Double, timeRemaining: String) = localizeImmediately("gui.Assembler.Progress", progress.toInt.toString, timeRemaining)

    def Warning(name: String) = new ChatComponentText("§7- ").appendSibling(localizeLater("gui.Assembler.Warning." + name))

    def Warnings = localizeLater("gui.Assembler.Warnings")
  }

  object Chat {
    def WarningLuaFallback = new ChatComponentText("§aOpenComputers§f: ").appendSibling(localizeLater("gui.Chat.WarningLuaFallback"))

    def WarningProjectRed = new ChatComponentText("§aOpenComputers§f: ").appendSibling(localizeLater("gui.Chat.WarningProjectRed"))

    def WarningPower = new ChatComponentText("§aOpenComputers§f: ").appendSibling(localizeLater("gui.Chat.WarningPower"))

    def WarningFingerprint(event: FMLFingerprintViolationEvent) = new ChatComponentText("§aOpenComputers§f: ").appendSibling(localizeLater("gui.Chat.WarningFingerprint", event.expectedFingerprint, event.fingerprints.toArray.mkString(", ")))

    def WarningRecipes = new ChatComponentText("§aOpenComputers§f: ").appendSibling(localizeLater("gui.Chat.WarningRecipes"))

    def InfoNewVersion(version: String) = new ChatComponentText("§aOpenComputers§f: ").appendSibling(localizeLater("gui.Chat.NewVersion", version))

    def TextureName(name: String) = new ChatComponentText("§aOpenComputers§f: ").appendSibling(localizeLater("gui.Chat.TextureName", name))
  }

  object Computer {
    def TurnOff = localizeImmediately("gui.Robot.TurnOff")

    def TurnOn = localizeImmediately("gui.Robot.TurnOn")

    def Power = localizeImmediately("gui.Robot.Power")
  }

  object Raid {
    def Warning = localizeImmediately("gui.Raid.Warning")
  }

  object ServerRack {
    def Top = localizeImmediately("gui.ServerRack.Top")

    def Bottom = localizeImmediately("gui.ServerRack.Bottom")

    def Left = localizeImmediately("gui.ServerRack.Left")

    def Right = localizeImmediately("gui.ServerRack.Right")

    def Back = localizeImmediately("gui.ServerRack.Back")

    def None = localizeImmediately("gui.ServerRack.None")

    def SwitchExternal = localizeImmediately("gui.ServerRack.SwitchExternal")

    def SwitchInternal = localizeImmediately("gui.ServerRack.SwitchInternal")

    def WirelessRange = localizeImmediately("gui.ServerRack.WirelessRange")
  }

  object Switch {
    def TransferRate = localizeImmediately("gui.Switch.TransferRate")

    def PacketsPerCycle = localizeImmediately("gui.Switch.PacketsPerCycle")

    def QueueSize = localizeImmediately("gui.Switch.QueueSize")
  }

  object Terminal {
    def InvalidKey = localizeLater("gui.Terminal.InvalidKey")

    def OutOfRange = localizeLater("gui.Terminal.OutOfRange")
  }

  object Tooltip {
    def Materials = localizeImmediately("tooltip.Materials")

    def Tier(tier: Int) = localizeImmediately("tooltip.Tier", tier.toString)
  }

}
