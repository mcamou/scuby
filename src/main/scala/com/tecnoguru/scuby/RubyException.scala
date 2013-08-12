package com.tecnoguru.scuby

import org.jruby.{RubyException => JRubyException}

/**
 * Wrapper around an org.jruby.RubyException that shows both the Java and the JRuby backtraces
 * @param exception the contained org.jruby.RubyException
 */
case class RubyException(val exception:JRubyException) extends Exception(exception.toString){
  override def printStackTrace() = printStackTrace(System.err)

  override def printStackTrace(w: java.io.PrintWriter) = {
    w.println(backtrace)
  }

  override def printStackTrace(w: java.io.PrintStream) = {
    super.printStackTrace(w)
    w.println("========== Ruby backtrace:")
    exception.printBacktrace(w)
  }

  def backtrace: String = { 
    val traceStream = new java.io.ByteArrayOutputStream
    val printStream = new java.io.PrintStream(traceStream)
    printStackTrace(printStream)
    printStream.close
    traceStream.close
    traceStream.toString
  }
}
