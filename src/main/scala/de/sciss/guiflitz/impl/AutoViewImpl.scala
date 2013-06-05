package de.sciss.guiflitz
package impl

import rillit.Lenser

private[guiflitz] class AutoViewImpl[A](private var _value: A) extends AutoView[A] {
  def value: A = _value

  private val lens = Lenser[A]

  // lens.name.set(value)("hallo")
}