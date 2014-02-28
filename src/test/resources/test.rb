require  'java'

class Person
  attr_accessor :firstname, :lastname
  def initialize (firstname, lastname)
    @firstname = firstname
    @lastname = lastname
  end

  def fullname
    "#{firstname} #{lastname}"
  end

  def get_label
    javax.swing.JLabel.new(fullname)
  end

  def ==(other)
    other.firstname == firstname and other.lastname == lastname
  end

  def to_s
    "Person(#{lastname}, #{firstname})"
  end

  def hash
    firstname.hash ^ lastname.hash
  end
end

class Backend
  def self.get_people
    [
        Person.new("Zaphod","Beeblebrox"),
        Person.new("Arthur","Dent"),
        Person.new("Ford","Prefect")
    ]
  end

  def self.get_data
    { :people => get_people, :other_data => get_other_data }
  end

  def self.get_person(firstname)
    p = get_people.select { | p | p.firstname == firstname}
    if p.length > 0
      p[0]
    else
      nil
    end
  end

  def self.get_other_data
    {
        :system_name => "Deep Thought",
        :answer => 42
    }
  end
end

def get_empty_array
  []
end

module TestModule
  module Inner
    class Test
      def initialize(msg)
        @msg = msg
      end

      def test_me
        "Hello, #{msg}"
      end
    end
  end
end