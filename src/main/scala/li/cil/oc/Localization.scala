package li.cil.oc

import li.cil.oc.client.CommandHandler.SetClipboardCommand
import net.minecraft.event.ClickEvent
import net.minecraft.event.HoverEvent
import net.minecraft.util.ChatComponentText
import net.minecraft.util.ChatComponentTranslation
import net.minecraft.util.StatCollector
import net.minecraftforge.fml.common.event.FMLFingerprintViolationEvent

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
    def Address(value: String) = {
      val result = localizeLater("gui.Analyzer.Address", value)
      result.getChatStyle.setChatClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, s"/${SetClipboardCommand.name} $value"))
      result.getChatStyle.setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, localizeLater("gui.Analyzer.CopyToClipboard")))
      result
    }

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

    def WarningClassTransformer = new ChatComponentText("§aOpenComputers§f: ").appendSibling(localizeLater("gui.Chat.WarningClassTransformer"))

    def WarningSimpleComponent = new ChatComponentText("§aOpenComputers§f: ").appendSibling(localizeLater("gui.Chat.WarningSimpleComponent"))

    def WarningLink(url: String) = new ChatComponentText("§aOpenComputers§f: ").appendSibling(localizeLater("gui.Chat.WarningLink", url))

    def InfoNewVersion(version: String) = new ChatComponentText("§aOpenComputers§f: ").appendSibling(localizeLater("gui.Chat.NewVersion", version))

    def TextureName(name: String) = new ChatComponentText("§aOpenComputers§f: ").appendSibling(localizeLater("gui.Chat.TextureName", name))
  }

  object Computer {
    def TurnOff = localizeImmediately("gui.Robot.TurnOff")

    def TurnOn = localizeImmediately("gui.Robot.TurnOn")

    def Power = localizeImmediately("gui.Robot.Power")
  }

  object Drive {
    def Managed = localizeImmediately("gui.Drive.Managed")

    def Unmanaged = localizeImmediately("gui.Drive.Unmanaged")

    def Warning = localizeImmediately("gui.Drive.Warning")
  }

  object Raid {
    def Warning = localizeImmediately("gui.Raid.Warning")
  }

  object Rack {
    def Top = localizeImmediately("gui.Rack.Top")

    def Bottom = localizeImmediately("gui.Rack.Bottom")

    def Left = localizeImmediately("gui.Rack.Left")

    def Right = localizeImmediately("gui.Rack.Right")

    def Back = localizeImmediately("gui.Rack.Back")

    def None = localizeImmediately("gui.Rack.None")

    def RelayEnabled = localizeImmediately("gui.Rack.Enabled")

    def RelayDisabled = localizeImmediately("gui.Rack.Disabled")
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
    def DiskUsage(used: Long, capacity: Long) = localizeImmediately("tooltip.DiskUsage", used.toString, capacity.toString)

    def DiskMode(isUnmanaged: Boolean) = localizeImmediately(if (isUnmanaged) "tooltip.DiskModeUnmanaged" else "tooltip.DiskModeManaged")

    def Materials = localizeImmediately("tooltip.Materials")

    def Tier(tier: Int) = localizeImmediately("tooltip.Tier", tier.toString)

    def PrintBeaconBase = localizeImmediately("tooltip.Print.BeaconBase")

    def PrintLightValue(level: Int) = localizeImmediately("tooltip.Print.LightValue", level.toString)

    def PrintRedstoneLevel(level: Int) = localizeImmediately("tooltip.Print.RedstoneLevel", level.toString)
  }

}
