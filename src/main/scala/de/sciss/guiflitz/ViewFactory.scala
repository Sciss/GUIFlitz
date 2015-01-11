/*
 *  ViewFactory.scala
 *  (GUIFlitz)
 *
 *  Copyright (c) 2013-2015 Hanns Holger Rutz. All rights reserved.
 *
 *  This software is published under the GNU Lesser General Public License v2.1+
 *
 *  For further information, please contact Hanns Holger Rutz at
 *  contact@sciss.de
 */

package de.sciss.guiflitz

import scala.language.implicitConversions
import scala.swing.Component

object ViewFactory {
  implicit def fromFunction[A](fun: Cell[A] => Component): ViewFactory[A] = new ViewFactory[A] {
    def makeView(cell: Cell[A]): Component = fun(cell)
  }
}
trait ViewFactory[A] {
  def makeView(cell: Cell[A]): Component
}
