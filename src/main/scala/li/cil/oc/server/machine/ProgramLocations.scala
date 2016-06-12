package li.cil.oc.server.machine

import scala.collection.mutable

object ProgramLocations {
  final val architectureLocations = mutable.Map.empty[String, mutable.Map[String, String]]
  final val globalLocations = mutable.Map.empty[String, String]

  def addMapping(program: String, label: String, architectures: String*): Unit = {
    if (architectures == null || architectures.isEmpty) {
      globalLocations += (program -> label)
    }
    else {
      architectures.foreach(architectureLocations.getOrElseUpdate(_, mutable.Map.empty[String, String]) += (program -> label))
    }
  }

  def getMappings(architecture: String) = architectureLocations.getOrElse(architecture, Iterable.empty) ++ globalLocations
}
