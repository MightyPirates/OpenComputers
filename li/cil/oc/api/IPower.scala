package li.cil.oc.api

/**
 * This interface can be mixed into a normal driver implementation to make the
 * component the driver controls use or provide power.
 *
 * Generally this will be used for components using power by providing a
 * non-negative power consumption in {@link #getPowerConsumption}, and by
 * components providing power by providing a negative value. If a computer is
 * insufficiently powered it will immediately turn off.
 *
 * Note that a computer can use multiple power providing components. The order
 * in which they are used is determined by their priority.
 */
trait IPower {
  /**
   * The power the component requires.
   *
   * The unit of power used for computers is roughly equivalent to IC2's EC.
   *
   * This is queried every tick, so you may change this on the fly, for example
   * to implement a power-saver mode. Components may return negative numbers to
   * act as generators. In that case the actual value does not matter: the
   * component will be asked to try and provide a certain amount of power at a
   * later stage. See {@link #consumePower}.
   *
   * @return the current power consumption per tick.
   */
  def getPowerConsumption(): Double

  /**
   * The priority of this component when used as a power supply.
   *
   * This is called each tick for all components providing power, to determine
   * in which order to call their {@link #consumePower} functions until the
   * computer's power need is satisfied. Components with a higher priority will
   * be used first. If two components have the same priority the order is
   * unspecified.
   *
   * @return the priority with which to use this component as a generator.
   */
  def getPriority(): Byte

  /**
   * This is called if the component acts as a power supply to get some power.
   *
   * If this component's {@link #getPowerConsumption} returned a negative value
   * and more power is still needed to keep the computer running, this function
   * will be called. The order is determined by the component's priority, as
   * defined via {@link #getPriority}. The component will be asked for as much
   * power as is required for this tick. The component returns how much power
   * it could actually generate. The component should always consume its fuel.
   * If there is not enough power to keep the computer running, fuel will go to
   * waste. This is by design.
   *
   * @param amount the power required to keep the computer running.
   * @return the amount of power this component could generate.
   */
  def consumePower(amount: Double): Double
}