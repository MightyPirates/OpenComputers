package li.cil.oc.common.block.traits

import net.minecraft.block.Block
import net.minecraft.block.properties._
import net.minecraft.block.state.IBlockState
import net.minecraft.util.BlockPos
import net.minecraft.world.IBlockAccess
import net.minecraftforge.common.property.ExtendedBlockState
import net.minecraftforge.common.property.IExtendedBlockState
import net.minecraftforge.common.property.IUnlistedProperty

import scala.collection.convert.WrapAsScala._
import scala.collection.mutable

// Utility trait for blocks that use properties (e.g. for rotation).
// Provides automatic conversion to and from metadata and takes care of generic
// setup of stateful blocks with listed and unlisted properties.
trait Extended extends Block {
  // Keep track of our properties, used for automatic metadata conversion.
  private lazy val (listedProperties, unlistedProperties) = {
    val listed = mutable.ArrayBuffer.empty[IProperty[_ <: Comparable[AnyRef]]]
    val unlisted = mutable.ArrayBuffer.empty[IUnlistedProperty[_ <: Comparable[AnyRef]]]
    createProperties(listed, unlisted)
    (listed.toArray, unlisted.toArray)
  }

  // Some metadata one the properties we cache for performance.
  private lazy val propertyData: Map[IProperty[_ <: Comparable[AnyRef]], (Int, Array[_ <: Comparable[AnyRef]])] =
    listedProperties.map(property => (property, (propertySize(property), property.getAllowedValues.toArray.map(_.asInstanceOf[Comparable[AnyRef]]).sortWith((a, b) => a.compareTo(b.asInstanceOf[AnyRef]) < 0)))).toMap

  // Check if property<->meta conversions work as expected.
  // performSelfTest()

  // Gnaah, implementation limitations :-/ Can't access protected methods from
  // traits, so we require subclasses to implement this methods which simply
  // forwards to setDefaultState...
  protected def setDefaultExtendedState(state: IBlockState): Unit

  setDefaultExtendedState(getBlockState.getBaseState)

  override def createBlockState() = {
    new ExtendedBlockState(this, listedProperties.map(_.asInstanceOf[IProperty[_]]), unlistedProperties.map(_.asInstanceOf[IUnlistedProperty[_]]))
  }

  // We basically store state information as a packed struct in the metadata
  // bits. For each property we determine the number of bits required to store
  // it (using the number of allowed values) and shift per property.
  override def getMetaFromState(state: IBlockState): Int = {
    var meta = 0
    for (property <- listedProperties) {
      val (bits, values) = propertyData(property)
      if (bits > 0) {
        val value = state.getValue(property.asInstanceOf[IProperty[_]])
        meta = (meta << bits) | values.indexOf(value)
      }
    }
    meta
  }

  override def getStateFromMeta(meta: Int): IBlockState = {
    var currentMeta = meta
    var state = getDefaultState
    // When unpacking we work from back to front, so iterate backwards.
    for (property <- listedProperties.reverseIterator) {
      val (bits, values) = propertyData(property)
      if (bits > 0) {
        val value = currentMeta & (0xFFFF >>> (16 - bits))
        state = state.withProperty(property.asInstanceOf[IProperty[_]], values(value).asInstanceOf[Comparable[AnyRef]])
        currentMeta = currentMeta >>> bits
      }
    }
    state
  }

  // Delegates to subclasses to accumulate state information.
  override def getExtendedState(state: IBlockState, world: IBlockAccess, pos: BlockPos) =
    addExtendedState(state.asInstanceOf[IExtendedBlockState], world, pos).
      getOrElse(super.getExtendedState(state, world, pos))

  // Overridden in subclasses such that each subclass returns
  // state.withProperty for each of their properties, then calls
  // the parent with that modified. Call chain eventually returns
  // fully built state, or None if one fails.
  protected def addExtendedState(state: IBlockState, world: IBlockAccess, pos: BlockPos): Option[IBlockState] = Some(state)

  // Overridden in subclasses to accumulate properties used by this block type,
  // result is stored (this is only called once) and used for state generation
  // and metadata computation.
  protected def createProperties(listed: mutable.ArrayBuffer[IProperty[_ <: Comparable[AnyRef]]], unlisted: mutable.ArrayBuffer[IUnlistedProperty[_ <: Comparable[AnyRef]]]): Unit = {}

  // Called during construction to ensure all our states map properly to
  // metadata and back, to avoid unpleasant surprises during runtime.
  private def performSelfTest(): Unit = {
    val metas = mutable.Set.empty[Int]
    getBlockState.getValidStates.collect {
      case state: IBlockState =>
        val meta = getMetaFromState(state)
        if ((meta & 0xF) != meta) {
          throw new IllegalArgumentException("Invalid block definition, meta data out of bounds (uses more than 4 bits).")
        }
        if (metas.contains(meta)) {
          throw new IllegalArgumentException("Invalid block definition, duplicate metadata for different block states.")
        }
        metas += meta
        val stateFromMeta = getStateFromMeta(meta & 0xF)
        if (state.hashCode() != stateFromMeta.hashCode()) {
          throw new IllegalArgumentException("Invalid block definition, state from meta does not match state meta was from.")
        }
    }
  }

  // Computes number of bits required to store a property. Uses the naive and
  // slow but readable and understandable approach because it is only called
  // once per property during construction anyway.
  private def propertySize(property: IProperty[_]) = {
    var maxIndex = property.getAllowedValues.size - 1
    var bits = 0
    while (maxIndex > 0) {
      bits += 1
      maxIndex >>= 1
    }
    bits
  }
}
