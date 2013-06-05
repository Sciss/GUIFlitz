package de.sciss.guiflitz

import impl.{AutoViewImpl => Impl}
import scala.swing.{Panel, Component}

object AutoView {
  def apply[A](init: A): AutoView[A] = new Impl(init)
}
abstract class AutoView[A] extends Panel {
  def value: A
}