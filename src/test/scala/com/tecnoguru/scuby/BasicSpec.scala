package com.tecnoguru.scuby

import org.specs2.mutable.Specification

import JRuby._

class BasicSpec extends Specification {
  "Scuby" should {
    "evaluate ruby code" in {
      eval[Long]("1 + 1") === 2
    }

    "send methods to Ruby objects" in {
      val array1 = evalRuby("[1,2,3]")
      val length = array1.send[Long]('length)
      length === 3
    }

    "send methods to Ruby objects when the method exists" in {
      val array1 = evalRuby("[1,2,3]")
      val length = array1.sendOpt[Long]('length)
      length === Some(3)
    }

    "Forward equals to ==" in {
      val array1 = evalRuby("[1, 2, 3]")
      val array2 = evalRuby("[1, 2, 3]")
      array1 === array2
    }

    "create Ruby objects of a given class with new RubyObject" in {
      val array1 = evalRuby("[]")
      val array2 = new RubyObject('Array)
      array1 === array2
    }

    "create Ruby objects of a given class with RubyClass" in {
      val array1 = evalRuby("[]")
      val array2 = RubyClass('Array) ! 'new
      array1 === array2
    }

    "create Ruby Symbols" in {
      val sym = evalRuby(":foo")
      %('foo) === sym
    }

    "implicitly convert Scala symbols to Ruby symbols" in {
      val sym = evalRuby(":foo")
      val sym2: RubyObj = 'foo
      sym === sym2
    }

    "forward require to Ruby" in {
      require("test.rb")
      val array1: RubyObj = evalRuby("[]")
      val array2: RubyObj = evalRuby("get_empty_array")
      array1 === array2
    }

    "return a Ruby Class object using RubyClass" in {
      require("test.rb")
      val testClass = RubyClass("TestModule::Inner::Test")
      testClass.send[String]('name) === "TestModule::Inner::Test"
      (testClass ! 'class).send[String]('name) === "Class"
    }

    "be able to check if a Ruby object is of a given class" in {
      val array = evalRuby("[]")
      array.isA_?('Array) must beTrue
      array.isA_?('Hash) must beFalse
      array.isA_?('Object) must beTrue
    }

    "be able to check if a Ruby object responds to a method" in {
      val array = evalRuby("[]")
      array.respondTo_?('length) must beTrue
      array.respondTo_?('foo) must beFalse
      array.respondTo_?("[]") must beTrue
    }

    "retrieve Ruby Array elements with parentheses" in {
      val array1 = evalRuby("['foo','bar','baz']")
      array1(0) === "foo"
      array1(1) === "bar"
      array1(2) === "baz"
    }

    "retrieve Ruby Hash elements with parentheses" in {
      val hash = evalRuby("{ :foo => 1, :bar => 2, :baz => 3 }")
      hash('foo) === 1
    }

    "chain Hash calls" in {
      val hash = evalRuby(
                           """{ :foo => {:x => 'foo x', :y => 'foo y'},
                                :bar => {:x => 'bar x', :y => 'bar y'},
                                :baz => {:x => 'baz x', :y => 'baz y'} }""")
      hash('foo, 'x) === "foo x"
      hash('bar, 'y) === "bar y"
    }
  }
}

