package de.sciss.guiflitz

import scala.reflect.runtime.{universe => ru}

object CakeTest extends App {
  trait Sys {
    type Global

    case class Container(global: Global)

    def mkContainer(): Container
  }

  object TestSys extends Sys {
    type Global = Unit

    def mkContainer() = new Container(())
  }

  test()

  def test(): Unit = {
    val con = TestSys.mkContainer()
    AutoView[TestSys.Container](con)
  }
}