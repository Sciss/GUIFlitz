package de.sciss.guiflitz

import rillit.{dynamic, Lenser}
import language.existentials
import language.reflectiveCalls

object LensTest extends App {
  val p1  = Person.Example
  val ls  = Lenser[Person]
  val p2  = ls.name.set(p1)("Paik")
  println(p2)
  val p3  = ls.refs.set(p2)(Vector(Person("Wolf", 80)))
  println(p3)

  // case class Foo(name: String)
  // val lf = dynamic.Lenser[Foo]()
  // lf.name.get
}