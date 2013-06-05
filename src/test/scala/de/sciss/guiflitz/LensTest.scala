package de.sciss.guiflitz

import rillit.{dynamic, Lenser}
import language.existentials
import language.reflectiveCalls

object LensTest extends App {
  // case class Foo(name: String)

  val p1  = Person.Example
  val ls  = Lenser[Person]
  val p2  = ls.name.set(p1)("Paik")
  println(p2)
  val p3  = ls.refs.set(p2)(Vector(Person("Wolf", 80)))
  println(p3)

  // val ld  = dynamic.Lenser[Person]
  // val p4  = ld.name.set(p3)("Baek")
}