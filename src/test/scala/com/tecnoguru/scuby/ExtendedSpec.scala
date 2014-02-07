package com.tecnoguru.scuby

import org.specs2.mutable.Specification
import org.specs2.specification.BeforeExample
import com.tecnoguru.scuby.JRuby._
import javax.swing.JLabel

class ExtendedSpec extends Specification with BeforeExample {
  var backend:RubyObj = null

  def before = {
    require("test.rb")
    backend = RubyClass('Backend)
  }

  "Scuby" should {
    "create an object with parameters using the RubyObject constructor" in {
      val person = new RubyObject('Person, "Eccentrica", "Gallumbits")
      val firstName: String = person send 'firstname
      val lastName: String = person send 'lastname
      firstName === "Eccentrica"
      lastName === "Gallumbits"
    }

    "create an object with parameters using the RubyClass new method" in {
      val slarti1 = RubyClass('Person) ! ('new, "Slartibartfast", "<No last name>")
      val slarti2 = new RubyObject('Person, "Slartibartfast", "<No last name>")
      slarti1 === slarti2
    }

    "forward toString to to_s" in {
      val deepThought = new RubyObject('Person, "Deep", "Thought")
      deepThought.toString === "Person(Thought, Deep)"
    }

    "forward hashCode to hash" in {
      val vogon = new RubyObject('Person, "Prostetnic", "Vogon Jeltz")
      val hash1 = vogon.hashCode
      val hash2:Long = vogon send 'hash
      hash1 === hash2
    }

    "add elements to an Array" in {
      val trillian = new RubyObject('Person, "Tricia", "McMillan")
      val people = backend ! 'get_people
      people(3) = trillian

      val length:Long = people send 'length
      length === 4

      people(3) === trillian
    }

    "call a method on a Ruby object or class and get back another Ruby object" in {
      val ford1 = backend ! ('get_person, "Ford")
      val ford2 = new RubyObject('Person, "Ford", "Prefect")
      ford1 === ford2
    }

    "call a method with no parameters" in {
      val people = backend ! 'get_people
      val zaphod1 = people(0).asInstanceOf[RubyObj]
      val zaphod2 = new RubyObject('Person, "Zaphod", "Beeblebrox")
      zaphod1 === zaphod2
    }

    "call a Ruby method which returns a Java object" in {
      val zaphod = new RubyObject('Person, "Zaphod", "Beeblebrox")
      val label: JLabel = zaphod.send[JLabel]('get_label)
      label.getText === "Zaphod Beeblebrox"
    }

    "wrap a JRuby object in a Scala trait" in {
      trait Person {
        def firstname: String
        def firstname_=(f: String): Unit
        def lastname: String
        def lastname_=(l: String): Unit
        def fullname: String
        def getLabel: JLabel
      }

      val zaphod:Person = new RubyObject('Person, "Zaphod", "Beeblebrox").as[Person]
      zaphod.firstname === "Zaphod"
      zaphod.firstname = "The Zeeb"
      zaphod.firstname === "The Zeeb"
      zaphod.fullname === "The Zeeb Beeblebrox"
      zaphod.getLabel.getText === "The Zeeb Beeblebrox"
    }
  }
}
