package li.cil.oc

import cpw.mods.fml.common.event.FMLFingerprintViolationEvent
import net.minecraft.util.{ChatMessageComponent, StatCollector}

object Localization {
  private def resolveKey(key: String) = if (StatCollector.func_94522_b(Settings.namespace + key)) Settings.namespace + key else key

  def localizeLater(formatKey: String, values: AnyRef*) = ChatMessageComponent.createFromTranslationWithSubstitutions(resolveKey(formatKey), values: _*)

  def localizeLater(key: String) = ChatMessageComponent.createFromTranslationKey(resolveKey(key))

  def localizeImmediately(formatKey: String, values: AnyRef*) = StatCollector.translateToLocalFormatted(resolveKey(formatKey), values: _*)

  def localizeImmediately(key: String) = StatCollector.translateToLocal(resolveKey(key))

  object Analyzer {
    def Address(value: String) = localizeLater("gui.Analyzer.Address", value)

    def AddressCopied = localizeLater("gui.Analyzer.AddressCopied")

    def ChargerSpeed(value: Double) = localizeLater("gui.Analyzer.ChargerSpeed", (value * 100).toInt + "%")

    def ComponentName(value: String) = localizeLater("gui.Analyzer.ComponentName", value)

    def Components(count: Int, maxCount: Int) = localizeLater("gui.Analyzer.Components", count + "/" + maxCount)

    def LastError(value: String) = localizeLater("gui.Analyzer.LastError", localizeLater(value))

    def RobotOwner(owner: String) = localizeLater("gui.Analyzer.RobotOwner", owner)

    def RobotName(name: String) = localizeLater("gui.Analyzer.RobotName", name)

    def RobotXp(experience: Double, level: Int) = localizeLater("gui.Analyzer.RobotXp", "%.2f".format(experience), level.toString)

    def StoredEnergy(value: String) = localizeLater("gui.Analyzer.StoredEnergy", value)

    def TotalEnergy(value: String) = localizeLater("gui.Analyzer.TotalEnergy", value)

    def Users(list: Iterable[String]) = localizeLater("gui.Analyzer.Users", list.mkString(", "))

    def WirelessStrength(value: Double) = localizeLater("gui.Analyzer.WirelessStrength", value.toInt.toString)
  }

  object Chat {
    def WarningLuaFallback = ChatMessageComponent.createFromText("§aOpenComputers§f: ").appendComponent(localizeLater("gui.Chat.WarningLuaFallback"))

    def WarningProjectRed = ChatMessageComponent.createFromText("§aOpenComputers§f: ").appendComponent(localizeLater("gui.Chat.WarningProjectRed"))

    def WarningPower = ChatMessageComponent.createFromText("§aOpenComputers§f: ").appendComponent(localizeLater("gui.Chat.WarningPower"))

    def WarningFingerprint(event: FMLFingerprintViolationEvent) = ChatMessageComponent.createFromText("§aOpenComputers§f: ").appendComponent(localizeLater("gui.Chat.WarningFingerprint", event.expectedFingerprint, event.fingerprints.toArray.mkString(", ")))

    def InfoNewVersion(version: String) = ChatMessageComponent.createFromText("§aOpenComputers§f: ").appendComponent(localizeLater("gui.Chat.NewVersion", version))
  }

  object Robot {
    def TurnOff = localizeImmediately("gui.Robot.TurnOff")

    def TurnOn = localizeImmediately("gui.Robot.TurnOn")

    def Power = localizeImmediately("gui.Robot.Power")
  }

  object RobotAssembler {
    def InsertCase = localizeImmediately("gui.RobotAssembler.InsertCase")

    def InsertCPU = localizeImmediately("gui.RobotAssembler.InsertCPU")

    def InsertRAM = localizeImmediately("gui.RobotAssembler.InsertRAM")

    def Complexity(complexity: Int, maxComplexity: Int) = localizeImmediately("gui.RobotAssembler.Complexity", complexity.toString, maxComplexity.toString)

    def Run = localizeImmediately("gui.RobotAssembler.Run")

    def CollectRobot = localizeImmediately("gui.RobotAssembler.CollectRobot")

    def Progress(progress: Double, timeRemaining: String) = localizeImmediately("gui.RobotAssembler.Progress", progress.toInt.toString, timeRemaining)

    def Warning(name: String) = "§7- " + localizeImmediately("gui.RobotAssembler.Warning." + name)

    def Warnings = localizeImmediately("gui.RobotAssembler.Warnings")
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
    def InvalidKey = localizeImmediately("gui.Terminal.InvalidKey")

    def OutOfRange = localizeImmediately("gui.Terminal.OutOfRange")
  }

  object Tooltip {
    def Materials = localizeImmediately("tooltip.Materials")

    def Tier(tier: Int) = localizeImmediately("tooltip.Tier", tier.toString)
  }

}
