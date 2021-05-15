package li.cil.oc.util

import scala.annotation.tailrec

/**
 * https://gist.github.com/viktorklang/1057513
 */
trait ScalaEnum {

  import java.util.concurrent.atomic.AtomicReference

  //Concurrency paranoia

  type EnumVal <: Value //This is a type that needs to be found in the implementing class

  private val _values = new AtomicReference(Vector[EnumVal]()) //Stores our enum values

  //Adds an EnumVal to our storage, uses CCAS to make sure it's thread safe, returns the ordinal
  @tailrec private final def addEnumVal(newVal: EnumVal): Int = {
    import _values.get
    import _values.{compareAndSet => CAS}
    val oldVec = get
    val newVec = oldVec :+ newVal
    if ((get eq oldVec) && CAS(oldVec, newVec))
      newVec.indexWhere(_ eq newVal)
    else
      addEnumVal(newVal)
  }

  def values: Vector[EnumVal] = _values.get

  //Here you can get all the enums that exist for this type

  //This is the trait that we need to extend our EnumVal type with, it does the book-keeping for us
  protected trait Value {
    self: EnumVal =>
    //Enforce that no one mixes in Value in a non-EnumVal type
    final val ordinal = addEnumVal(this) //Adds the EnumVal and returns the ordinal

    def name: String //All enum values should have a name

    override def toString = name //And that name is used for the toString operation

    override def equals(other: Any) = this eq other.asInstanceOf[AnyRef]

    override def hashCode = 31 * (this.getClass.## + name.## + ordinal)
  }

}