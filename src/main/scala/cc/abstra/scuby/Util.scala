package cc.abstra.scuby

/**
 * Misc. utility methods that can be called either from Scala or JRuby
 */
object Util { 
  /**
   * Quick way to unwrap an Option to get around JRuby's pesky problems with $ in identifiers (i.e. can't access None because it's None$)
   */
  def unwrapOption[T](param: Option[T]): Any = param.getOrElse(null)
}
