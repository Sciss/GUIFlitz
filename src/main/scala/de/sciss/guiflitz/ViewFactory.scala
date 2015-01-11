package de.sciss.guiflitz

import de.sciss.model.Model

import scala.swing.Component

object ViewFactory {
  implicit def fromFunction[A](fun: A => (Model[Any], Component)): ViewFactory[A] = new ViewFactory[A] {
    def makeView(init: A): (Model[Any], Component) = fun(init)
  }
}
trait ViewFactory[A] {
  def makeView(init: A): (Model[Any], Component)
}
