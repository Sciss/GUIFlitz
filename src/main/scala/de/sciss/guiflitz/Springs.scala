///*
// *  Springs.scala
// *  (GUIFlitz)
// *
// *  Copyright (c) 2013 Hanns Holger Rutz. All rights reserved.
// *
// *  This software is free software; you can redistribute it and/or
// *  modify it under the terms of the GNU General Public License
// *  as published by the Free Software Foundation; either
// *  version 2, june 1991 of the License, or (at your option) any later version.
// *
// *  This software is distributed in the hope that it will be useful,
// *  but WITHOUT ANY WARRANTY; without even the implied warranty of
// *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
// *  General Public License for more details.
// *
// *  You should have received a copy of the GNU General Public
// *  License (gpl.txt) along with this software; if not, write to the Free Software
// *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
// *
// *
// *  For further information, please contact Hanns Holger Rutz at
// *  contact@sciss.de
// */
//
//package de.sciss.guiflitz
//
//import javax.swing.{SpringLayout, Spring}
//import language.implicitConversions
//
//object Springs {
//  implicit def intToSpring(pref: Int): Spring = Spring.constant(pref)
//
//  implicit final class RichSpring(val s: Spring) extends AnyVal {
//    import Spring.{max => mx, _}
//    def +  (that: Spring) : Spring = sum(s, that)
//    def -  (that: Spring) : Spring = sum(s, minus(that))
//    def min(that: Spring) : Spring = minus(mx(minus(s), minus(that)))
//    def max(that: Spring) : Spring = mx(s, that)
//    def unary_- : Spring           = minus(s)
//    def *  (factor: Float): Spring = scale(s, factor)
//
//    def value     : Int         = s.getValue
//    def value_= (i: Int): Unit  = s.setValue(i)
//    def minValue: Int           = s.getMinimumValue
//    def maxValue: Int           = s.getMaximumValue
//    def preferredValue: Int     = s.getPreferredValue
//  }
//
//  implicit final class RichConstraints(val c: SpringLayout.Constraints) extends AnyVal {
//    def x               : Spring        = c.getX
//    def x_=       (value: Spring): Unit = c.setX     (value)
//    def y               : Spring        = c.getY
//    def y_=       (value: Spring): Unit = c.setY     (value)
//    def width           : Spring        = c.getWidth
//    def width_=   (value: Spring): Unit = c.setWidth (value)
//    def height          : Spring        = c.getHeight
//    def height_=  (value: Spring): Unit = c.setHeight(value)
//    def left            : Spring        = c.getConstraint(SpringLayout.WEST             )
//    def left_=    (value: Spring): Unit = c.setConstraint(SpringLayout.WEST             , value)
//    def top             : Spring        = c.getConstraint(SpringLayout.NORTH            )
//    def top_=     (value: Spring): Unit = c.setConstraint(SpringLayout.NORTH            , value)
//    def right           : Spring        = c.getConstraint(SpringLayout.EAST             )
//    def right_=   (value: Spring): Unit = c.setConstraint(SpringLayout.EAST             , value)
//    def bottom          : Spring        = c.getConstraint(SpringLayout.SOUTH            )
//    def bottom_=  (value: Spring): Unit = c.setConstraint(SpringLayout.SOUTH            , value)
//    def baseline        : Spring        = c.getConstraint(SpringLayout.BASELINE         )
//    def baseline_=(value: Spring): Unit = c.setConstraint(SpringLayout.BASELINE         , value)
//    def centerX         : Spring        = c.getConstraint(SpringLayout.HORIZONTAL_CENTER)
//    def centerX_= (value: Spring): Unit = c.setConstraint(SpringLayout.HORIZONTAL_CENTER, value)
//    def centerY         : Spring        = c.getConstraint(SpringLayout.VERTICAL_CENTER  )
//    def centerY_= (value: Spring): Unit = c.setConstraint(SpringLayout.VERTICAL_CENTER  , value)
//  }
//}