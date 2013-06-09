package de.sciss.guiflitz

object Person {
  val Example = Person("Nam June", 80, 3.1415) // , Vector(Person("John", 100)))
}
case class Person(name: String, age: Int, num: Double) // , refs: Vector[Person] = Vector.empty)
