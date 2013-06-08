package de.sciss.guiflitz

import de.sciss.model.Model
import de.sciss.model.impl.ModelImpl

object Cell {
  def apply[A](init: A): Cell[A] = new Impl(init)

  final class Impl[A](var _value: A) extends Cell[A] with ModelImpl[A] {
    override def toString = s"Cell(${this()})"

    def apply(): A = _value
    def update(value: A) {
      if (_value != value) {
        _value = value
        dispatch(value)
      }
    }
  }

  type Listener[A] = Model.Listener[A]
}
sealed trait Cell[A] extends Model[A] {
  def apply(): A
  def update(value: A): Unit
}