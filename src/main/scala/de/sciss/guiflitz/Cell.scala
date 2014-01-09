/*
 *  Cell.scala
 *  (GUIFlitz)
 *
 *  Copyright (c) 2013-2014 Hanns Holger Rutz. All rights reserved.
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  For further information, please contact Hanns Holger Rutz at
 *  contact@sciss.de
 */

package de.sciss.guiflitz

import de.sciss.model.Model
import de.sciss.model.impl.ModelImpl

object Cell {
  def apply[A](init: A): Cell[A] = new Impl(init)

  final class Impl[A](var _value: A) extends Cell[A] with ModelImpl[A] {
    override def toString = s"Cell(${this()})"

    def apply(): A = _value
    def update(value: A): Unit =
      if (_value != value) {
        _value = value
        dispatch(value)
      }
  }

  type Listener[A] = Model.Listener[A]
}
sealed trait Cell[A] extends Model[A] {
  def apply(): A
  def update(value: A): Unit
}