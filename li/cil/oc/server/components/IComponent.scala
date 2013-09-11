package li.cil.oc.server.components

import li.cil.oc.common.util.INBTSerializable

trait IComponent {
  private var _id = 0

  def id: Int = _id

  def id_=(value: Int) = _id = value
}