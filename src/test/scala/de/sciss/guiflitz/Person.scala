package de.sciss.guiflitz

object Person {
  val Example = Person("Nam June", 80, Vector(Person("John", 100)))
}
case class Person(name: String, age: Int, refs: Vector[Person] = Vector.empty)
