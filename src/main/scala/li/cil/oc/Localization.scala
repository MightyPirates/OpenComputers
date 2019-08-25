package li.cil.oc

import li.cil.oc.client.CommandHandler.SetClipboardCommand
import net.minecraft.util.text.ITextComponent
import net.minecraft.util.text.TextComponentString
import net.minecraft.util.text.TextComponentTranslation
import net.minecraft.util.text.event.ClickEvent
import net.minecraft.util.text.event.HoverEvent
import net.minecraft.util.text.translation.I18n
import net.minecraftforge.fml.common.event.FMLFingerprintViolationEvent

import scala.util.matching.Regex

object Localization {
  private val nl = Regex.quote("[nl]")

  private def resolveKey(key: String) = if (canLocalize(Settings.namespace + key)) Settings.namespace + key else key

  def canLocalize(key: String): Boolean = I18n.canTranslate(key)

  def localizeLater(formatKey: String, values: AnyRef*) = new TextComponentTranslation(resolveKey(formatKey), values: _*)

  def localizeLater(key: String) = new TextComponentTranslation(resolveKey(key))

  def localizeImmediately(formatKey: String, values: AnyRef*): String = I18n.translateToLocalFormatted(resolveKey(formatKey), values: _*).split(nl).map(_.trim).mkString("\n")

  def localizeImmediately(key: String): String = I18n.translateToLocal(resolveKey(key)).split(nl).map(_.trim).mkString("\n")

  object Analyzer {
    def Address(value: String): TextComponentTranslation = {
      val result = localizeLater("gui.Analyzer.Address", value)
      result.getStyle.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, s"/${SetClipboardCommand.name} $value"))
      result.getStyle.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, localizeLater("gui.Analyzer.CopyToClipboard")))
      result
    }

    def AddressCopied: TextComponentTranslation = localizeLater("gui.Analyzer.AddressCopied")

    def ChargerSpeed(value: Double): TextComponentTranslation = localizeLater("gui.Analyzer.ChargerSpeed", (value * 100).toInt + "%")

    def ComponentName(value: String): TextComponentTranslation = localizeLater("gui.Analyzer.ComponentName", value)

    def Components(count: Int, maxCount: Int): TextComponentTranslation = localizeLater("gui.Analyzer.Components", count + "/" + maxCount)

    def LastError(value: String): TextComponentTranslation = localizeLater("gui.Analyzer.LastError", localizeLater(value))

    def RobotOwner(owner: String): TextComponentTranslation = localizeLater("gui.Analyzer.RobotOwner", owner)

    def RobotName(name: String): TextComponentTranslation = localizeLater("gui.Analyzer.RobotName", name)

    def RobotXp(experience: Double, level: Int): TextComponentTranslation = localizeLater("gui.Analyzer.RobotXp", f"$experience%.2f", level.toString)

    def StoredEnergy(value: String): TextComponentTranslation = localizeLater("gui.Analyzer.StoredEnergy", value)

    def TotalEnergy(value: String): TextComponentTranslation = localizeLater("gui.Analyzer.TotalEnergy", value)

    def Users(list: Iterable[String]): TextComponentTranslation = localizeLater("gui.Analyzer.Users", list.mkString(", "))

    def WirelessStrength(value: Double): TextComponentTranslation = localizeLater("gui.Analyzer.WirelessStrength", value.toInt.toString)
  }

  object Assembler {
    def InsertTemplate: String = localizeImmediately("gui.Assembler.InsertCase")

    def CollectResult: String = localizeImmediately("gui.Assembler.Collect")

    def InsertCPU: TextComponentTranslation = localizeLater("gui.Assembler.InsertCPU")

    def InsertRAM: TextComponentTranslation = localizeLater("gui.Assembler.InsertRAM")

    def Complexity(complexity: Int, maxComplexity: Int): ITextComponent = {
      val message = localizeLater("gui.Assembler.Complexity", complexity.toString, maxComplexity.toString)
      if (complexity > maxComplexity) new TextComponentString("§4").appendSibling(message)
      else message
    }

    def Run: String = localizeImmediately("gui.Assembler.Run")

    def Progress(progress: Double, timeRemaining: String): String = localizeImmediately("gui.Assembler.Progress", progress.toInt.toString, timeRemaining)

    def Warning(name: String): ITextComponent = new TextComponentString("§7- ").appendSibling(localizeLater("gui.Assembler.Warning." + name))

    def Warnings: TextComponentTranslation = localizeLater("gui.Assembler.Warnings")
  }

  object Chat {
    def WarningLuaFallback: ITextComponent = new TextComponentString("§aOpenComputers§f: ").appendSibling(localizeLater("gui.Chat.WarningLuaFallback"))

    def WarningProjectRed: ITextComponent = new TextComponentString("§aOpenComputers§f: ").appendSibling(localizeLater("gui.Chat.WarningProjectRed"))

    def WarningFingerprint(event: FMLFingerprintViolationEvent): ITextComponent = new TextComponentString("§aOpenComputers§f: ").appendSibling(localizeLater("gui.Chat.WarningFingerprint", event.getExpectedFingerprint, event.getFingerprints.toArray.mkString(", ")))

    def WarningRecipes: ITextComponent = new TextComponentString("§aOpenComputers§f: ").appendSibling(localizeLater("gui.Chat.WarningRecipes"))

    def WarningClassTransformer: ITextComponent = new TextComponentString("§aOpenComputers§f: ").appendSibling(localizeLater("gui.Chat.WarningClassTransformer"))

    def WarningSimpleComponent: ITextComponent = new TextComponentString("§aOpenComputers§f: ").appendSibling(localizeLater("gui.Chat.WarningSimpleComponent"))

    def WarningLink(url: String): ITextComponent = new TextComponentString("§aOpenComputers§f: ").appendSibling(localizeLater("gui.Chat.WarningLink", url))

    def InfoNewVersion(version: String): ITextComponent = new TextComponentString("§aOpenComputers§f: ").appendSibling(localizeLater("gui.Chat.NewVersion", version))

    def TextureName(name: String): ITextComponent = new TextComponentString("§aOpenComputers§f: ").appendSibling(localizeLater("gui.Chat.TextureName", name))
  }

  object Computer {
    def TurnOff: String = localizeImmediately("gui.Robot.TurnOff")

    def TurnOn: String = localizeImmediately("gui.Robot.TurnOn")

    def Power: String = localizeImmediately("gui.Robot.Power")
  }

  object Drive {
    def Managed: String = localizeImmediately("gui.Drive.Managed")

    def Unmanaged: String = localizeImmediately("gui.Drive.Unmanaged")

    def Warning: String = localizeImmediately("gui.Drive.Warning")

    def ReadOnlyLock: String = localizeImmediately("gui.Drive.ReadOnlyLock")

    def LockWarning: String = localizeImmediately("gui.Drive.ReadOnlyLockWarning")
  }

  object Raid {
    def Warning: String = localizeImmediately("gui.Raid.Warning")
  }

  object Rack {
    def Top: String = localizeImmediately("gui.Rack.Top")

    def Bottom: String = localizeImmediately("gui.Rack.Bottom")

    def Left: String = localizeImmediately("gui.Rack.Left")

    def Right: String = localizeImmediately("gui.Rack.Right")

    def Back: String = localizeImmediately("gui.Rack.Back")

    def None: String = localizeImmediately("gui.Rack.None")

    def RelayEnabled: String = localizeImmediately("gui.Rack.Enabled")

    def RelayDisabled: String = localizeImmediately("gui.Rack.Disabled")
  }

  object Switch {
    def TransferRate: String = localizeImmediately("gui.Switch.TransferRate")

    def PacketsPerCycle: String = localizeImmediately("gui.Switch.PacketsPerCycle")

    def QueueSize: String = localizeImmediately("gui.Switch.QueueSize")
  }

  object Terminal {
    def InvalidKey: TextComponentTranslation = localizeLater("gui.Terminal.InvalidKey")

    def OutOfRange: TextComponentTranslation = localizeLater("gui.Terminal.OutOfRange")
  }

  object Tooltip {
    def DiskUsage(used: Long, capacity: Long): String = localizeImmediately("tooltip.diskusage", used.toString, capacity.toString)

    def DiskMode(isUnmanaged: Boolean): String = localizeImmediately(if (isUnmanaged) "tooltip.diskmodeunmanaged" else "tooltip.diskmodemanaged")

    def Materials: String = localizeImmediately("tooltip.materials")

    def DiskLock(lockInfo: String): String = if (lockInfo.isEmpty) "" else localizeImmediately("tooltip.disklocked", lockInfo)

    def Tier(tier: Int): String = localizeImmediately("tooltip.tier", tier.toString)

    def PrintBeaconBase: String = localizeImmediately("tooltip.print.BeaconBase")

    def PrintLightValue(level: Int): String = localizeImmediately("tooltip.print.LightValue", level.toString)

    def PrintRedstoneLevel(level: Int): String = localizeImmediately("tooltip.print.RedstoneLevel", level.toString)

    def MFULinked(isLinked: Boolean): String = localizeImmediately(if (isLinked) "tooltip.upgrademf.Linked" else "tooltip.upgrademf.Unlinked")

    def ExperienceLevel(level: Double): String = localizeImmediately("tooltip.robot_level", level.toString)
  }

}
