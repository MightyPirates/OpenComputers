package li.cil.oc.util

import li.cil.oc.util.StackOption._
import net.minecraft.item.ItemStack

object StackOption {

  //implicit def extendedStack(stack: ItemStack): StackOption = if (stack.isEmpty) Empty else new StackOption(stack)

  implicit def extendedOption(opt: Option[ItemStack]): ExtendedOption = ExtendedOption(opt)

  // Mostly stolen from Option
  implicit def stack2Iterable(so: StackOption): Iterable[ItemStack] = so.toList

  def apply(stack: ItemStack): StackOption = if (stack == null || stack.isEmpty) EmptyStack else SomeStack(stack)

  def apply(stack: Option[ItemStack]): StackOption = if (stack == null || stack.isEmpty || stack.get.isEmpty) EmptyStack else SomeStack(stack.get)

  def empty: StackOption = EmptyStack

  case object EmptyStack extends StackOption(ItemStack.EMPTY)

  final case class SomeStack(stack: ItemStack) extends StackOption(stack)
}

final case class ExtendedOption(opt : Option[ItemStack]) {
  def asStackOption = StackOption(opt)
}

sealed abstract class StackOption(stack: ItemStack) extends Product with Serializable {
  self =>

  def isEmpty: Boolean = stack == null || stack.isEmpty

  def get: ItemStack = stack

  def isDefined: Boolean = !isEmpty

  def getOrElse(default: ItemStack): ItemStack = if (isEmpty) default else this.get

  def orEmpty: ItemStack = if(isEmpty) EmptyStack.get else this.get

  def map(f: ItemStack => ItemStack): StackOption = if (isEmpty) EmptyStack else SomeStack(f(this.get))

  //def map[B](f: ItemStack => B): Option[B] = if (isEmpty) None else Some(f(this.get))

  def fold[B](ifEmpty: => B)(f: ItemStack => B): B = if (isEmpty) ifEmpty else f(this.get)

  def flatMap[B](f: ItemStack => Option[B]): Option[B] = if (isEmpty) None else f(this.get)

  def flatten[B](implicit ev: ItemStack <:< Option[B]): Option[B] = if (isEmpty) None else ev(this.get)

  def filter(p: ItemStack => Boolean): StackOption = if (isEmpty || p(this.get)) this else EmptyStack

  def filterNot(p: ItemStack => Boolean): StackOption = if (isEmpty || !p(this.get)) this else EmptyStack

  def nonEmpty: Boolean = isDefined

  def withFilter(p: ItemStack => Boolean): WithFilter = new WithFilter(p)

  class WithFilter(p: ItemStack => Boolean) {
    //def map[B](f: ItemStack => B): Option[B] = self filter p map f

    //def flatMap[B](f: ItemStack => Option[B]): Option[B] = self filter p flatMap f

    def foreach[U](f: ItemStack => U): Unit = self filter p foreach f

    def withFilter(q: ItemStack => Boolean): WithFilter = new WithFilter(x => p(x) && q(x))
  }

  def contains[A1 >: ItemStack](elem: A1): Boolean = !isEmpty && this.get == elem

  def exists(p: ItemStack => Boolean): Boolean = !isEmpty && p(this.get)

  def forall(p: ItemStack => Boolean): Boolean = isEmpty || p(this.get)

  def foreach[U](f: ItemStack => U) {
    if (!isEmpty) f(this.get)
  }

  def collect[B](pf: PartialFunction[ItemStack, B]): Option[B] = if (!isEmpty) pf.lift(this.get) else None

  def orElse[B >: ItemStack](alternative: => StackOption): StackOption =
    if (isEmpty) alternative else this

  def iterator: Iterator[ItemStack] =
    if (isEmpty) collection.Iterator.empty else collection.Iterator.single(this.get)

  def toList: List[ItemStack] =
    if (isEmpty) List() else new ::(this.get, Nil)

  def toRight[X](left: => X): Either[X, ItemStack] =
    if (isEmpty) Left(left) else Right(this.get)

  def toLeft[X](right: => X): Either[ItemStack, X] =
    if (isEmpty) Right(right) else Left(this.get)
}
