package cc.abstra.scuby

import org.jruby.{RubyObject => JRubyObject}
import JRuby.str2sym

/**
 * A wrapped Ruby object. Adds convenience methods to call the JRuby methods.
 */
trait RubyObj {
  /**
   * The wrapped object
   */
  val obj: JRubyObject

  /**
   * Convenience method. Call a method on the wrapped object indicating its return type
   * @param T The expected class of the return value
   * @param name The method name
   * @param args The method parameters
   * @return The wrapped return value of the method
   * @throw ClassCastException if the return value is not of the expected class
   */
  def send[T](name: Symbol, args: Any*) = (new RubyMethod[T](this, name))(args:_*)

  /**
   * Convenience method to call a method on the wrapped object, expecting a RubyObj as a return value
   * @param name The method name
   * @param args The method parameters
   * @return The wrapped return value of the method
   * @throw ClassCastException if the return value is not a RubyObj
   * @see send
   */
  def ! (name: Symbol, args: AnyRef*) = send[RubyObj](name, args:_*)

  /**
   * Get an object that represents a Ruby method for this object, expecting an AnyRef as return value
   * @param name The method name
   * @return The object that represents the method
   */
  def --> (name: Symbol) = new RubyMethod[AnyRef](this, name)

  /**
   * Convenient access to Array- or Hash-like Ruby objects.
   * - If the object supports the "call" or "invoke" method (i.e. a block) it will call it with the given parameters
   * - Otherwise, for one parameter will call the [] method on the object and return the result as an AnyRef
   * - For more than one parameters, will call the [] method successively passing each subsequent parameter
   *   and return the result of the last call as an AnyRef (useful for Hashes of Hashes or Arrays of Arrays)
   */
  def apply(args: Any*): AnyRef = { 
    if (respondTo_?('call)) send[AnyRef]('call, args:_*)
    else if (respondTo_?('invoke)) send[AnyRef]('invoke, args:_*)
    else { 
      val last = (this /: args.slice(0, args.length - 1)) { (result, key) => send[RubyObj]("[]", key) }

      // The last/only one is excluded from the fold because of the return type (AnyRef vs. RubyObj)
      last.send[AnyRef]("[]",args(args.length - 1))
    }
  }
  
  /**
   * Convenience method to see if an object responds to a method
   */
  def respondTo_?(method: Symbol): Boolean = send("respond_to?", %(method))
  
  /**
   * Convenience method to see if an object belongs to a Ruby class
   */
  def isA_?(klazzName: Symbol): Boolean = send("is_a?", RubyClass(klazzName))
  
  /**
   * Convenient access to Array- or Hash-like Ruby objects
   */
  def update[T](key: Any, value: T): Unit = send[T]("[]=", key, value)

  /**
   * Delegate toString to the Ruby object's to_s
   */
  override def toString = send[String]('to_s)


  /**
   * Delegate equals to the Ruby object's ==
   */
  override def equals(other:Any) = other match {
    case otherRef: RubyObj => send[Boolean]("==", otherRef.obj)
    case _ => false
  }

  /**
   * Delegate hashCode to the Ruby object's hash. Ruby's hash returns a Long while Java's hashCode
   * returns an int.
   */
  override def hashCode = send[Long]('hash).intValue
}

/**
 * Wraps a JRuby object with our own convenience methods
 * @param obj the object to wrap
 */
class RubyObject (val obj: JRubyObject) extends RubyObj {
  /**
   * Auxiliary constructor for a new JRuby Object of a given class.
   * @param rubyClassName Name of the Ruby class to create the object
   * @param args Any parameters to the constructor of the Ruby class
   */
  def this(rubyClassName: Symbol, args: AnyRef*) = this(RubyClass(rubyClassName).send[RubyObj]('new, args:_*).obj)
}

/**
 * Factory for Ruby Symbols
 */
object % {
  // TODO Handle a cache of Scala symbol -> Ruby symbol mappings
  /**
   * Creates a Ruby Symbol from a Scala Symbol.
   * @param sym the Scala symbol
   * @return a RubyObj that represents the corresponding Ruby Symbol
   */
  def apply (sym: Symbol) = JRuby.eval[RubyObj](":'"+sym.name+"'")
  def apply (sym: String) = JRuby.eval[RubyObj](":'"+sym+"'")
}

/**
 * Factory for Ruby class objects
 */
object RubyClass {
  // TODO Handle a cache of Scala symbol -> Ruby class mappings
  // TODO Allow extending a Ruby class from Scala
  /**
   * Returns the Ruby Class object for the given class
   * @param name The required class name
   * @return The associated Class object as a RubyObj
   */
  def apply(name: Symbol) = JRuby.eval[RubyObj](name.name)
}

/**
 * Represents a callable Ruby method
 * @param T The expected class of the return value
 */
class RubyMethod[T] private[scuby](target: RubyObj, name: Symbol) {
  import JRuby._

  /**
   * Call the method
   * @param args The method parameters
   * @return The wrapped results from the method
   */
  def apply(args: Any*): T = send[T](target.obj, name.name, args:_*)
}
