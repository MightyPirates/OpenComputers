package li.cil.oc.util

import scala.collection.mutable

class RTree[Data](private val M: Int)(implicit val coordinate: Data => (Double, Double, Double)) {
  if (M < 2) throw new IllegalArgumentException("maxEntries must be larger or equal to 2.")

  // Used for quick checks whether values are in the tree, e.g. for updates.
  private val entries = mutable.Map.empty[Data, Leaf]

  private val m = math.max(M / 2, 1)

  private var root = new NonLeaf()

  def apply(value: Data): Option[(Double, Double, Double)] = this.synchronized {
    entries.get(value).fold(None: Option[(Double, Double, Double)])(position => Some(position.bounds.min.asTuple))
  }

  // Allows debug rendering of the tree.
  def allBounds = this.synchronized(root.allBounds(0))

  def add(value: Data): Boolean = this.synchronized {
    val replaced = remove(value)
    val entry = new Leaf(value, new Point(value))
    entries += value -> entry
    root.add(entry) match {
      case newNode if newNode != root => root = new NonLeaf(newNode, root)
      case _ =>
    }
    !replaced
  }

  def remove(value: Data): Boolean = this.synchronized {
    entries.remove(value) match {
      case Some(node) =>
        val change = root.remove(node)
        assert(change.contains(node) || change.contains(root))
        root.children.headOption match {
          case Some(nonLeaf: NonLeaf) if root.children.size == 1 =>
            root = nonLeaf
          case _ =>
            root.bounds = Rectangle.around(root.children)
        }
        true
      case _ => false
    }
  }

  def query(from: (Double, Double, Double), to: (Double, Double, Double)) = this.synchronized {
    root.query(new Rectangle(new Point(from), new Point(to)))
  }

  private abstract class Node {
    def bounds: Rectangle

    def allBounds(level: Int) = Iterable((bounds.asTuple, level))

    def isLeaf = true

    def add(value: Node): Node

    def remove(value: Node): Option[Node]

    def query(query: Rectangle): Iterable[Data]
  }

  private class NonLeaf extends Node {
    def this(nodes: Node*) = {
      this()
      for (child <- nodes) {
        children += child
        bounds = bounds.including(child.bounds)
      }
    }

    val children = mutable.Set.empty[Node]

    var bounds = new Rectangle(Point.PositiveInfinity, Point.NegativeInfinity)

    override def allBounds(level: Int) = super.allBounds(level) ++ children.flatMap(_.allBounds(level + 1))

    override def isLeaf = children.headOption.exists(_.isInstanceOf[Leaf])

    def add(value: Node): Node = {
      assert(value != this)
      uncheckedAdd(value)
      if (children.size > M) {
        split()
      }
      else {
        bounds = bounds.including(value.bounds)
        this
      }
    }

    private def uncheckedAdd(value: Node) {
      var bestChild: Option[Node] = null
      var bestGrowth = Double.PositiveInfinity
      var bestVolume = Double.PositiveInfinity
      for (child <- children if !child.isLeaf || value.isInstanceOf[Leaf]) {
        val oldVolume = child.bounds.volume
        val volume = child.bounds.including(value.bounds).volume
        val growth = volume - oldVolume
        if (growth < bestGrowth || (growth == bestGrowth && volume < bestVolume)) {
          bestChild = Some(child)
          bestGrowth = growth
          bestVolume = volume
        }
      }
      bestChild match {
        case Some(child) =>
          children += child.add(value)
        case _ =>
          // Empty root or node while inserting children of removing child node.
          children += value
      }
    }

    def remove(value: Node): Option[Node] = {
      if (bounds.intersects(value.bounds)) for (child <- children) {
        child.remove(value) match {
          case Some(change) =>
            if (change == child) {
              // Underflow after removing node or child was the node to remove.
              children -= child
              child match {
                case node: NonLeaf =>
                  for (child <- node.children) {
                    uncheckedAdd(child)
                  }
                  if (children.size > M) {
                    // Escalate overflow.
                    return Some(split())
                  }
                case leaf: Leaf => assert(leaf == value)
              }
              if (children.size < m) {
                // Escalate underflow.
                return Some(this)
              }
              // Done handling tree adjustment, bubble result up.
              bounds = Rectangle.around(children)
              return Some(value)
            }
            else if (change == value) {
              // Removal, bubble result up.
              bounds = Rectangle.around(children)
              return Some(value)
            }
            else {
              // Overflow due to split after underflow.
              assert(change.isInstanceOf[NonLeaf])
              uncheckedAdd(change)
              if (children.size > M) {
                // Escalate overflow.
                return Some(split())
              }
              else {
                // Done handling tree adjustment, bubble result up.
                bounds = Rectangle.around(children)
                return Some(value)
              }
            }
          case _ =>
        }
      }
      None
    }

    def query(query: Rectangle) =
      if (query.intersects(bounds))
        children.foldRight(Iterable.empty[Data])((child, result) => result ++ child.query(query))
      else Iterable.empty[Data]

    private def split() = {
      val values = children.toArray
      var seed1: Option[Node] = None
      var seed2: Option[Node] = None
      var worst = Double.NegativeInfinity
      for (i <- values.indices) {
        val si = values(i)
        for (j <- i + 1 until values.length) {
          val sj = values(j)
          val vol1 = si.bounds.volume
          val vol2 = sj.bounds.volume
          val vol = si.bounds.including(sj.bounds).volume
          val d = vol - vol1 - vol2
          if (d > worst) {
            seed1 = Some(si)
            seed2 = Some(sj)
            worst = d
          }
        }
      }
      (seed1, seed2) match {
        case (Some(s1), Some(s2)) =>
          val r1 = new SplitResult(mutable.Set(s1), s1.bounds)
          val r2 = new SplitResult(mutable.Set(s2), s2.bounds)

          val list = mutable.Set.empty ++ values
          list -= s1
          list -= s2
          while (list.nonEmpty) {
            if (m - r1.set.size >= list.size) {
              list.foreach(r1.add)
              list.clear()
            }
            else if (m - r2.set.size >= list.size) {
              list.foreach(r2.add)
              list.clear()
            }
            else {
              var bestValue: Option[Node] = None
              var r = r1
              var best = Double.NegativeInfinity
              for (value <- list) {
                val newVol1 = r1.volumeIncluding(value)
                val newVol2 = r2.volumeIncluding(value)
                val growth1 = newVol1 - r1.volume
                val growth2 = newVol2 - r2.volume
                val d = math.abs(growth2 - growth1)
                if (d > best) {
                  bestValue = Some(value)
                  r = if (growth1 < growth2 || (growth1 == growth2 && newVol1 < newVol2)) r1 else r2
                  best = d
                }
              }
              bestValue match {
                case Some(value) =>
                  list -= value
                  r.add(value)
                case _ => throw new AssertionError()
              }
            }
          }

          children.clear()
          children ++= r1.set
          bounds = r1.bounds

          val LL = new NonLeaf()
          LL.children ++= r2.set
          LL.bounds = r2.bounds
          LL
        case _ => throw new AssertionError()
      }
    }
  }

  private class Leaf(val data: Data, point: Point) extends Node {
    val bounds = new Rectangle(point, point)

    def entries = Iterable(this)

    def add(value: Node) = value

    def remove(value: Node) =
      if (value == this) Some(this)
      else None

    def query(query: Rectangle) =
      if (query.intersects(bounds)) Iterable(data)
      else Iterable.empty
  }

  private class Point(val x: Double, val y: Double, val z: Double) {
    def this(p: (Double, Double, Double)) = this(p._1, p._2, p._3)

    def min(other: Point) = new Point(math.min(x, other.x), math.min(y, other.y), math.min(z, other.z))

    def max(other: Point) = new Point(math.max(x, other.x), math.max(y, other.y), math.max(z, other.z))

    def asTuple = (x, y, z)
  }

  private object Point {
    val NegativeInfinity = new Point(Double.NegativeInfinity, Double.NegativeInfinity, Double.NegativeInfinity)
    val PositiveInfinity = new Point(Double.PositiveInfinity, Double.PositiveInfinity, Double.PositiveInfinity)
  }

  private class Rectangle(val min: Point, val max: Point) {
    def including(value: Rectangle) = new Rectangle(value.min min min, value.max max max)

    def intersects(value: Rectangle) =
      value.min.x <= max.x && value.min.y <= max.y && value.min.z <= max.z &&
        value.max.x >= min.x && value.max.y >= min.y && value.max.z >= min.z

    def volume = {
      val sx = max.x - min.x
      val sy = max.y - min.y
      val sz = max.z - min.z
      sx * sy * sz
    }

    def asTuple = ((min.x, min.y, min.z), (max.x, max.y, max.z))
  }

  private object Rectangle {
    def around(values: Iterable[Node]) = {
      var min = Point.PositiveInfinity
      var max = Point.NegativeInfinity
      for (value <- values) {
        min = value.bounds.min min min
        max = value.bounds.max max max
      }
      new Rectangle(min, max)
    }
  }

  private class SplitResult(val set: mutable.Set[Node], var bounds: Rectangle) {
    def add(value: Node) {
      set += value
      bounds = bounds.including(value.bounds)
    }

    def volume = bounds.volume

    def volumeIncluding(value: Node) = bounds.including(value.bounds).volume
  }

}
