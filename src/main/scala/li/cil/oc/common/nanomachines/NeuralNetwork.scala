package li.cil.oc.common.nanomachines

import li.cil.oc.OpenComputers
import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.api.Persistable
import li.cil.oc.api.nanomachines.Behavior
import li.cil.oc.api.nanomachines.BehaviorProvider
import li.cil.oc.server.PacketSender
import li.cil.oc.util.ExtendedNBT._
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.ServerPlayerEntity
import net.minecraft.nbt.CompoundNBT
import net.minecraft.util.Util
import net.minecraft.util.text.StringTextComponent
import net.minecraft.util.text.TextFormatting
import net.minecraftforge.common.util.Constants.NBT

import scala.collection.convert.WrapAsScala._
import scala.collection.mutable
import scala.util.Random

class NeuralNetwork(controller: ControllerImpl) extends Persistable {
  val triggers = mutable.ArrayBuffer.empty[TriggerNeuron]
  val connectors = mutable.ArrayBuffer.empty[ConnectorNeuron]
  val behaviors = mutable.ArrayBuffer.empty[BehaviorNeuron]

  val behaviorMap = mutable.Map.empty[Behavior, BehaviorNeuron]

  def inputs(behavior: Behavior) = behaviorMap.get(behavior) match {
    case Some(node) => node.inputs.count(_.isActive)
    case _ => 0
  }

  def reconfigure(): Unit = {
    // Rebuild list of valid behaviors.
    behaviors.clear()
    behaviors ++= api.Nanomachines.getProviders.
      map(p => (p, Option(p.createBehaviors(controller.player)).map(_.filter(_ != null)).orNull)). // Remove null behaviors.
      filter(_._2 != null). // Remove null lists..
      flatMap(pb => pb._2.map(b => new BehaviorNeuron(pb._1, b)))

    // Adjust length of trigger list and reset.
    while (triggers.length > behaviors.length * Settings.get.nanomachineTriggerQuota) {
      triggers.remove(triggers.length - 1)
    }
    triggers.foreach(_.isActive = false)
    while (triggers.length < behaviors.length * Settings.get.nanomachineTriggerQuota) {
      triggers += new TriggerNeuron()
    }

    // Adjust length of connector list and reset.
    while (connectors.length > behaviors.length * Settings.get.nanomachineConnectorQuota) {
      connectors.remove(connectors.length - 1)
    }
    connectors.foreach(_.inputs.clear())
    while (connectors.length < behaviors.length * Settings.get.nanomachineConnectorQuota) {
      connectors += new ConnectorNeuron()
    }

    // Build connections.
    val rng = new Random(controller.player.level.random.nextInt())

    def connect[Sink <: ConnectorNeuron, Source <: Neuron](sinks: Iterable[Sink], sources: mutable.ArrayBuffer[Source]): Unit = {
      // Shuffle sink list to give each entry the same chance.
      val sinkPool = rng.shuffle(sinks.toBuffer)
      for (sink <- sinkPool if sources.nonEmpty) {
        // Avoid connecting one sink to the same source twice.
        val blacklist = mutable.Set.empty[Source]
        for (n <- 0 to rng.nextInt(Settings.get.nanomachineMaxInputs) if sources.nonEmpty) {
          val baseIndex = rng.nextInt(sources.length)
          val sourceIndex = (sources.drop(baseIndex) ++ sources.take(baseIndex)).indexWhere(s => !blacklist.contains(s))
          if (sourceIndex >= 0) {
            val source = sources.remove((sourceIndex + baseIndex) % sources.length)
            blacklist += source
            sink.inputs += source
          }
        }
      }
    }

    // Connect connectors to triggers, then behaviors to connectors and/or remaining triggers.
    val sourcePool = mutable.ArrayBuffer.fill(Settings.get.nanomachineMaxOutputs)(triggers.map(_.asInstanceOf[Neuron])).flatten
    connect(connectors, sourcePool)
    sourcePool ++= mutable.ArrayBuffer.fill(Settings.get.nanomachineMaxOutputs)(connectors.map(_.asInstanceOf[Neuron])).flatten
    connect(behaviors, sourcePool)

    // Clean up dead nodes.
    val deadConnectors = connectors.filter(_.inputs.isEmpty)
    connectors --= deadConnectors
    behaviors.foreach(_.inputs --= deadConnectors)

    val deadBehaviors = behaviors.filter(_.inputs.isEmpty)
    behaviors --= deadBehaviors

    behaviorMap.clear()
    behaviorMap ++= behaviors.map(n => n.behavior -> n)
  }

  // Enter debug configuration, one input -> one behavior, and list mapping in console.
  def debug(): Unit = {
    val log = controller.player match {
      case playerMP: ServerPlayerEntity => (s: String) => PacketSender.sendClientLog(s, playerMP)
      case _ => (s: String) => OpenComputers.log.info(s)
    }
    log(s"Creating debug configuration for nanomachines in player ${controller.player.getDisplayName}.")

    behaviors.clear()
    behaviors ++= api.Nanomachines.getProviders.
      map(p => (p, Option(p.createBehaviors(controller.player)).map(_.filter(_ != null)).orNull)). // Remove null behaviors.
      filter(_._2 != null). // Remove null lists..
      flatMap(pb => pb._2.map(b => new BehaviorNeuron(pb._1, b)))

    connectors.clear()

    triggers.clear()
    for (i <- behaviors.indices) {
      val behavior = behaviors(i)
      val trigger = new TriggerNeuron()
      triggers += trigger
      behavior.inputs += trigger

      log(s"$i -> ${behavior.behavior.getNameHint} (${behavior.behavior.getClass.toString})")
    }
  }

  def print(player: PlayerEntity): Unit = {
    val sb = StringBuilder.newBuilder
    def colored(value: Any, enabled: Boolean) = {
      if (enabled) sb.append(TextFormatting.GREEN)
      else sb.append(TextFormatting.RED)
      sb.append(value)
      sb.append(TextFormatting.RESET)
    }
    for (behavior <- behaviors) {
      val name = Option(behavior.behavior.getNameHint).getOrElse(behavior.behavior.getClass.getSimpleName)
      colored(name, behavior.isActive)
      sb.append(" <- (")
      var first = true
      for (input <- behavior.inputs) {
        if (first) first = false else sb.append(", ")
        input match {
          case neuron: TriggerNeuron =>
            colored(triggers.indexOf(neuron) + 1, neuron.isActive)
          case neuron: ConnectorNeuron =>
            sb.append("(")
            first = true
            for (trigger <- neuron.inputs) {
              if (first) first = false else sb.append(", ")
              colored(triggers.indexOf(trigger) + 1, trigger.isActive)
            }
            first = false
            sb.append(")")
        }
      }
      sb.append(")")
      player.sendMessage(new StringTextComponent(sb.toString()), Util.NIL_UUID)
      sb.clear()
    }
  }

  override def saveData(nbt: CompoundNBT): Unit = {
    saveData(nbt, forItem = false)
  }

  private final val TriggersTag = "triggers"
  private final val IsActiveTag = "isActive"
  private final val ConnectorsTag = "connectors"
  private final val BehaviorsTag = "behaviors"
  private final val BehaviorTag = "behavior"
  private final val TriggerInputsTag = "triggerInputs"
  private final val ConnectorInputsTag = "connectorInputs"

  def saveData(nbt: CompoundNBT, forItem: Boolean): Unit = {
    nbt.setNewTagList(TriggersTag, triggers.map(t => {
      val nbt = new CompoundNBT()
      nbt.putBoolean(IsActiveTag, t.isActive && !forItem)
      nbt
    }))

    nbt.setNewTagList(ConnectorsTag, connectors.map(c => {
      val nbt = new CompoundNBT()
      nbt.putIntArray(TriggerInputsTag, c.inputs.map(triggers.indexOf(_)).filter(_ >= 0).toArray)
      nbt
    }))

    nbt.setNewTagList(BehaviorsTag, behaviors.map(b => {
      val nbt = new CompoundNBT()
      nbt.putIntArray(TriggerInputsTag, b.inputs.map(triggers.indexOf(_)).filter(_ >= 0).toArray)
      nbt.putIntArray(ConnectorInputsTag, b.inputs.map(connectors.indexOf(_)).filter(_ >= 0).toArray)
      nbt.put(BehaviorTag, b.provider.save(b.behavior))
      nbt
    }))
  }

  override def loadData(nbt: CompoundNBT): Unit = {
    triggers.clear()
    nbt.getList(TriggersTag, NBT.TAG_COMPOUND).foreach((t: CompoundNBT) => {
      val neuron = new TriggerNeuron()
      neuron.isActive = t.getBoolean(IsActiveTag)
      triggers += neuron
    })

    connectors.clear()
    nbt.getList(ConnectorsTag, NBT.TAG_COMPOUND).foreach((t: CompoundNBT) => {
      val neuron = new ConnectorNeuron()
      neuron.inputs ++= t.getIntArray(TriggerInputsTag).map(triggers.apply)
      connectors += neuron
    })

    behaviors.clear()
    nbt.getList(BehaviorsTag, NBT.TAG_COMPOUND).foreach((t: CompoundNBT) => {
      api.Nanomachines.getProviders.find(p => p.load(controller.player, t.getCompound(BehaviorTag)) match {
        case b: Behavior =>
          val neuron = new BehaviorNeuron(p, b)
          neuron.inputs ++= t.getIntArray(TriggerInputsTag).map(triggers.apply)
          neuron.inputs ++= t.getIntArray(ConnectorInputsTag).map(connectors.apply)
          behaviors += neuron
          true // Done.
        case _ =>
          false // Keep looking.
      })
    })

    behaviorMap.clear()
    behaviorMap ++= behaviors.map(n => n.behavior -> n)
  }

  trait Neuron {
    def isActive: Boolean
  }

  class TriggerNeuron extends Neuron {
    var isActive = false
  }

  class ConnectorNeuron extends Neuron {
    val inputs = mutable.ArrayBuffer.empty[Neuron]

    override def isActive = inputs.forall(_.isActive)
  }

  class BehaviorNeuron(val provider: BehaviorProvider, val behavior: Behavior) extends ConnectorNeuron

}
