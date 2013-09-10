package li.cil.oc.server.components

trait IComponent {
  private var _id = 0

  def id: Int = _id

  def id_=(value: Int) = _id = value
}