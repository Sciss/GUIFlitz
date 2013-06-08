package de.sciss.guiflitz

import impl.{AutoViewImpl => Impl}
import scala.swing.{Component, Panel}
import reflect.runtime.{universe => ru}
import ru.TypeTag

object AutoView {
  def apply[A: TypeTag](init: A): AutoView[A] = Impl(init)
}
trait AutoView[A] {
  def value: A
  def cell: Cell[A]
  def component: Component
}