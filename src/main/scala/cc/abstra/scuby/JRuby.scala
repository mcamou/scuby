/*
 * JRuby.scala
 *
 * Base classes/objects/traits for the scuby library
 */

package cc.abstra.scuby

import java.util.logging.{Logger, Level}
import javax.script.{ScriptEngineManager, Invocable}
import org.jruby.{RubyObject => JRubyObject}
import org.jruby.exceptions.RaiseException

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
  import JRuby.{handleException,wrap,unwrap,engine}

  implicit def jrubyobj2rubyobj(jrobj:JRubyObject): RubyObject = new RubyObject(jrobj)
  implicit def str2sym (sym: String): Symbol = Symbol(sym)
  implicit def symbol2rubySymbol (sym: Symbol): RubyObj = %(sym)

  /**
   * Load a Ruby file from the CLASSPATH into the JRuby environment
   * @param file The file name to load, relative to the CLASSPATH
   */
  def require(file:String) = handleException(eval[Boolean]("require '" + file + "'"))

  /**
   * Evaluate an arbitrary Ruby expression
   * @param T The expected class of the return value
   * @param expression The ruby expression to evaluate
   * @return The expression's return value. If it's an org.jruby.RubyObj it's wrapped in a cc.abstra.scuby.RubyObj, otherwise it's returned as-is.
   * @see javax.script.ScriptEngine#eval
   */
  def eval[T](expression: String) = handleException(
    wrap[T](
      engine.eval(expression)
    )
  )

  /**
   * Invoke a Ruby function
   * @param T The expected class of the return value
   * @param name The name of the function to invoke
   * @param args The arguments to the function
   * @return The Ruby function's return value. If it's an org.jruby.RubyObj it's wrapped in a cc.abstra.scuby.RubyObj, otherwise it's returned as-is.
   * @see javax.script.Invocable#invokeFunction
   */
  def call[T] (name: Symbol, args: AnyRef*) = handleException(
    wrap[T](
      engine.asInstanceOf[Invocable].invokeFunction(name.name, unwrap(args:_*):_*)
    )
  )
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
  private val engine = new ScriptEngineManager().getEngineByName("jruby")

  /**
   * Invoke a Ruby method on a Ruby object. The way of doing this in the public API is by
   * invoking RubyObj.send(name, args).
   * @param T The expected class of the return value
   * @param target The object on which to invoke the method
   * @param name The name of the method to invoke
   * @param args The arguments to the method
   * @return The method's return value. If it's an org.jruby.RubyObj it's wrapped in a cc.abstra.scuby.RubyObj, otherwise it's returned as-is.
   * @see javax.script.Invocable#invokeMethod
   */
  private[scuby] def send[T](target: JRubyObject, name: Symbol, args: AnyRef*) = {
    handleException(
      wrap[T](
        engine.asInstanceOf[Invocable].invokeMethod(target, name.name, unwrap(args:_*):_*)
      )
    )
  }

  /**
   * Unwraps a parameter list. That is, for each parameter, if it's a cc.abstra.scuby.RubyObj it
   * extracts the embedded org.jruby.RubyObject, otherwise it leaves it as-is. Note that this is
   * not an exact inverse of wrap, since wrap takes a single object while unwrap takes a whole
   * argument list.
   * @param T The expected class of the return value
   * @param args The parameter list
   * @return The unwrapped parameters
   * @see wrap
   */
  private[scuby] def unwrap(args: AnyRef*) = args.map { (arg) =>
    arg match {
      case rbObj: RubyObj => rbObj.obj
      case sym: Symbol => %(sym).obj
      case _ => arg
    }
  }

  /**
   * Wraps an org.jruby.RubyObject in a cc.abstra.scuby.RubyObj. If the parameter is not an
   * org.jruby.RubyObject, it returns it as-is. Used to wrap return values from Ruby calls. Note
   * that this is not an exact inverse of unwrap since unwrap processes a parameter list while wrap
   * processes a single return value.
   * @param R The expected class of the return value
   * @param obj The object to wrap
   * @return The wrapped object
   */
  private[scuby] def wrap[T](obj: Any) = {
    (obj match {
      case jrObj: JRubyObject => new RubyObject(jrObj)
      case _ => obj
    }).asInstanceOf[T]
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
    case e => e.getCause match {
      case raiseEx: RaiseException => throw RubyException(raiseEx.getException)
      case _ => throw e
    }
  }
}
