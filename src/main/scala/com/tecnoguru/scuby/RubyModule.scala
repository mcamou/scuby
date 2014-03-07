package com.tecnoguru.scuby

import org.jruby.{RubyModule => JRubyModule}

private[scuby] trait ModuleHelper {
  def getModule(path: Seq[String]): JRubyModule = {
    val runtime = JRuby.runtime

    val headModule = runtime.getModule(path.head)
    if (headModule == null) throw new IllegalArgumentException(s"Ruby Module ${path.head} not found")

    //TODO JRubyModule#defineModuleUnder will actually create the module if it doesn't exist, and there doesn't seem to be a JRubyModule#getModule method, so we can't actually check for errors
    path.tail.foldLeft(headModule) { (memo, n) => memo.defineModuleUnder(n) }
  }
}

/**
 * Factory for Ruby class objects
 */
object RubyClass extends ModuleHelper {
  // TODO Handle a cache of Scala symbol -> Ruby class mappings
  /**
   * Returns the Ruby Class object for the given class
   * @param name The required class name
   * @return The associated Class object as a RubyObj
   */
  def apply(name: Symbol): RubyObj = apply(name.name)

  /**
   * Returns the Ruby Class object for the given class
   * @param name The required class name
   * @return The associated Class object as a RubyObj
   */
  def apply(name: String): RubyObj = {
    val path = name.split("::")
    val runtime = JRuby.runtime

    val klazz = if (path.length == 1) runtime.getClass(name)
    else getModule(path.init).getClass(path.last)

    if (klazz == null) throw new IllegalArgumentException(s"Ruby Class $name not found")

    new RubyObject(klazz)
  }
}

/**
 * Factory for Ruby Module objects
 */
object RubyModule extends ModuleHelper {
  // TODO Handle a cache of Scala symbol -> Ruby module mappings
  /**
   * Returns the Ruby Module object for the given module
   * @param name The required module name
   * @return The associated Module object as a RubyObj
   */
  def apply(name: Symbol): RubyObj = apply(name.name)

  /**
   * Returns the Ruby Module object for the given module
   * @param name The required module name
   * @return The associated Module object as a RubyObj
   */
  def apply(name: String): RubyObj = new RubyObject(getModule(name.split("::")))
}

