package de.sciss.guiflitz

import reflect.runtime.{universe => ru}

/** Note: this does _not_ work. */
object ProjectionTest extends App {
  trait Sys[S <: Sys[S]] {
    type Global
    implicit def selfTag: ru.TypeTag[S]

    def mkContainer(): Container[S]
  }

  case class Container[S <: Sys[S]](global: S#Global)

  def test[S <: Sys[S]](system: S): Unit = {
    val con = system.mkContainer()
    import system.selfTag
    AutoView[Container[S]](con)
  }

  class TestSys extends Sys[TestSys] {
    type Global = Unit
    val selfTag = ru.typeTag[TestSys]

    def mkContainer() = Container[TestSys]()
  }

  test(new TestSys)
}