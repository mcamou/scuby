/*
 * JRuby.scala
 *
 * Base classes/objects/traits for the scuby library
 */

package cc.abstra.scuby

import java.util.logging.{Logger, Level}

import org.jruby.{RubyObject => JRubyObject}
import org.jruby.embed.{ScriptingContainer,LocalContextScope,LocalVariableBehavior}
import org.jruby.exceptions.RaiseException
import org.jruby.{RubyInstanceConfig,CompatVersion}
import RubyInstanceConfig.CompileMode

import scala.collection.JavaConversions._
import scala.reflect.ClassTag

import scala.language.implicitConversions

/**
 * This trait is one of the main entry points into Scuby. Including it allows you to
 * evaluate arbitrary Ruby code, invoke JRuby functions and require Ruby files
 * contained in the CLASSPATH. The other entry points are RubyClass and RubyObj.
 * The JRuby object is also provided to give the option of extending the trait or
 * including the object.
 * @see RubyClass
 * @see RubyObj
 * @see JRuby
 */
trait JRuby {
  class IllegalTypeConversion(from: Class[_], to: Class[_])
    extends RuntimeException(s"Illegal type conversion. Ruby returned a ${from.getName}, call was expecting a ${to.getName}")

  class UnwrappedCallException(method: String)
    extends RuntimeException(s"If you expect to get back a RubyObj/RubyObject, use the specialized method $method")

  class NoReturnTypeException extends RuntimeException("No return type provided. Did you remember to include the generic in the call?")

  import JRuby.{handleException,verifyType,unwrap,ruby,verifyRubyObj}

  /* A few implicits to make the programmer's life easier. Some of these should be replaced with typeclasses */
  implicit def jrubyobj2rubyobj(jrobj:JRubyObject): RubyObj = new RubyObject(jrobj)
  implicit def str2sym (sym: String): Symbol = Symbol(sym)
  implicit def symbol2rubySymbol (sym: Symbol): RubyObj = %(sym)

  /**
   * Load a Ruby file from the CLASSPATH into the JRuby environment
   * @param file The file name to load, relative to the CLASSPATH
   */
  def require(file:String) = eval[Boolean]("require '%s'".format(file))

  /**
   * Evaluate an arbitrary Ruby expression, casting and wrapping the return value
   * @param T The expected class of the return value
   * @param expression The ruby expression to evaluate
   * @return The expression's return value.
   * @see org.jruby.embed.ScriptingContainer#runScriptlet
   * @see wrap
   */
  def eval[T](expression: String)(implicit tag: ClassTag[T]) = {
    handleException(verifyType[T](ruby.runScriptlet(expression), "evalRuby"))
  }

  /**
   * Evaluate an arbitrary Ruby expression, ignoring the return value
   * @param expression The ruby expression to evaluate
   * @return The expression's return value. If it's an org.jruby.RubyObj it's wrapped in a cc.abstra.scuby.RubyObj, otherwise it's returned as-is.
   * @see org.jruby.embed.ScriptingContainer#runScriptlet
   * @see wrap
   */
  def evalIgnore(expression: String) {
    handleException(ruby.runScriptlet(expression))
  }

  /**
   * Evaluate an arbitrary Ruby expression, expecting to get back a RubyObj
   * @param expression The ruby expression to evaluate
   * @return The expression's return value. If it's an org.jruby.RubyObj it's wrapped in a cc.abstra.scuby.RubyObj, otherwise it's returned as-is.
   * @see org.jruby.embed.ScriptingContainer#runScriptlet
   * @see wrap
   */
  def evalRuby(expression: String): RubyObj = {
    val obj = handleException(ruby.runScriptlet(expression))
    verifyRubyObj(obj)
  }
}

/**
 * This object is one of the main entry points into Scuby. Including it allows you to
 * evaluate arbitrary Ruby code, invoke JRuby functions and require Ruby files
 * contained in the CLASSPATH. The other entry points are RubyClass and RubyObj.
 * The JRuby trait is also provided to give the option of extending the trait or
 * including the object.
 * @see RubyClass
 * @see RubyObj
 */
object JRuby extends JRuby {
  val log = Logger getLogger "cc.abstra.scuby"

  private var ruby0: Option[ScriptingContainer] = None
  private var scope0 = LocalContextScope.SINGLETON
  private var localVariableBehavior0 = LocalVariableBehavior.TRANSIENT

  /**
   * Set the interpreter Scope. Must be called before any Ruby code is executed.
   * Default is Singleton scope.
   * @param scope The new scope
   * @see org.jruby.ScriptingContainer#new
   */
  def setScope (scope: LocalContextScope) {
    ruby0 match {
      case None => scope0 = scope
      case Some(r) => log warning "Scope specified for an already-created Ruby container - not changing"
    }
  }

  /**
   * Set the interpreter local variable behavior. Must be called before any Ruby code is executed.
   * Default is transient.
   * @param behavior The new local variable behavior
   * @see org.jruby.ScriptingContainer#new
   */
  def setLocalVariableBehavior (behavior: LocalVariableBehavior) {
    ruby0 match {
      case None => localVariableBehavior0 = behavior
      case Some(r) => log warning "LocalVariableBehavior specified for an already-created Ruby container - not changing"
    }
  }

  /**
   * Creates the Scripting container or returns the already-existing one. Note that
   * Scuby currently supports having a single Ruby interpreter.
   */
  def ruby: ScriptingContainer = ruby0 match {
    case Some(r) => r
    case None =>
      ruby0 = Some(new ScriptingContainer(scope0, localVariableBehavior0))
      ruby0.get
  }

  /**
   * Invoke a Ruby method on a Ruby object. The way of doing this in the public Scuby API is by
   * invoking RubyObj.send(name, args). Arguments are unwrapped and the result is wrapped again.
   * @param T The expected class of the return value
   * @param target The object on which to invoke the method
   * @param name The name of the method to invoke
   * @param args The arguments to the method
   * @return The method's return value. If it's an org.jruby.RubyObj it's wrapped in a cc.abstra.scuby.RubyObj, otherwise it's returned as-is.
   * @see javax.script.Invocable#invokeMethod
   * @see wrap[T]
   * @see unwrap[T
   */
  private[scuby] def send[T](target: JRubyObject, name: Symbol, args: Any*)(implicit tag: ClassTag[T]): T = {
    handleException(verifyType[T](ruby.callMethod(target, name.name, unwrap(args:_*):_*), "!"))
  }

  /**
   * Invoke a Ruby method on a Ruby object, ignoring the result. The way of doing this in the public Scuby API is by
   * invoking RubyObj.call(name, args). Arguments are unwrapped.
   * @param target The object on which to invoke the method
   * @param name The name of the method to invoke
   * @param args The arguments to the method
   * @return The method's return value. If it's an org.jruby.RubyObj it's wrapped in a cc.abstra.scuby.RubyObj, otherwise it's returned as-is.
   * @see javax.script.Invocable#invokeMethod
   * @see wrap[T]
   * @see unwrap[T
   */
  private[scuby] def sendUnit(target: JRubyObject, name: Symbol, args: Any*) {
    handleException(ruby.callMethod(target, name.name, unwrap(args:_*):_*))
  }

  /**
   * Invoke a Ruby method on a Ruby object, assuming the object returns a RubyObject. The way of doing this in the
   * public Scuby API is by invoking RubyObj.!(name, args). Arguments are unwrapped.
   * @param target The object on which to invoke the method
   * @param name The name of the method to invoke
   * @param args The arguments to the method
   * @return The method's return value. If it's an org.jruby.RubyObj it's wrapped in a cc.abstra.scuby.RubyObj, otherwise it's returned as-is.
   * @see javax.script.Invocable#invokeMethod
   * @see wrap[T]
   * @see unwrap[T
   */
  private[scuby] def sendRubyObj(target: JRubyObject, name: Symbol, args: Any*): RubyObj = {
    val obj = handleException(ruby.callMethod(target, name.name, unwrap(args:_*):_*))
    verifyRubyObj(obj)
  }

  /**
   * Unwraps a parameter list. Wrapped org.jruby.RubyObject's are extracted from their wrappers, Scala Symbols are
   * converted to Ruby Symbols and primitives are boxed. All other values are left as-is.
   * argument list.
   * @param args The parameter list
   * @return The unwrapped parameters
   * @see wrap
   */
  private[scuby] def unwrap(args: Any*):Seq[_ <: AnyRef] = {
    if (args == null) Array.empty[AnyRef]
    else args.map { _ match {
      case rbObj: RubyObj => rbObj.obj
      case sym: Symbol => %(sym).obj
      case x => x.asInstanceOf[AnyRef] // Needed so we wrap primitives to make the call into JRuby
    }
                  }
  }

  /**
   * Verifies that the object has the correct type or throws a more-readable exception than TypeConversionException.
   * If the parameter is of the correct type, returns it as-is, cast to the class given as type argument. This is
   * used to verify return values from Ruby calls.
   * @param T The expected class of the return value
   * @param obj The object to wrap
   * @return The wrapped object
   */
  private[scuby] def verifyType[T](obj: Any, method: String)(implicit tag: ClassTag[T]): T = {
    if (tag.runtimeClass == classOf[RubyObj]) throw new UnwrappedCallException(method)
    if (tag.runtimeClass == classOf[Nothing]) throw new NoReturnTypeException

    try {
      obj.asInstanceOf[T]
    } catch {
      // We do this instead of verifying the type beforehand so that implicit conversions kick in when calling instanceOf
      case _: ClassCastException => throw new IllegalTypeConversion(obj.getClass, tag.runtimeClass)
    }
  }

  /**
   * Wraps an org.jruby.RubyObject in a cc.abstra.scuby.RubyObj. If the parameter is not an
   * org.jruby.RubyObject or a cc.abstra.scuby.RubyObj, it throws an exception. This is used to verify return values
   * from Ruby calls.
   * @param obj The object to wrap
   * @return The wrapped object
   */
  private[scuby] def verifyRubyObj(obj: Any): RubyObj = {
    obj match {
      case obj: JRubyObject => new RubyObject(obj)
      case obj: RubyObj => obj
      case _ => throw new IllegalTypeConversion(obj.getClass, classOf[RubyObject])
    }
  }


  /**
   * Calls a Ruby expression/method/function. If there's a Ruby exception (marked as an
   * org.jruby.exceptions.RaiseException) it wraps it in a RubyException, otherwise it rethrows
   * the exception
   * @param func The code to evalate
   * @return whatever the code returns
   * @throws RubyException if the exception comes from Ruby
   * @throws Exception any other exception if the code throws another exception
   */
  private[scuby] def handleException[T](func: => T) = try {
    func
  } catch {
    case e:Throwable => e.getCause match {
      case raiseEx: RaiseException => throw RubyException(raiseEx.getException)
      case _ => throw e
    }
  }
}
