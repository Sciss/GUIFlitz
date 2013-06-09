package de.sciss.guiflitz

import javax.swing.{SpringLayout, Spring}
import language.implicitConversions

object Springs {
  implicit def intToSpring(pref: Int): Spring = Spring.constant(pref)

  implicit final class RichSpring(val s: Spring) extends AnyVal {
    import Spring.{max => mx, _}
    def +  (that: Spring) : Spring = sum(s, that)
    def -  (that: Spring) : Spring = sum(s, minus(that))
    def min(that: Spring) : Spring = minus(mx(minus(s), minus(that)))
    def max(that: Spring) : Spring = mx(s, that)
    def unary_- : Spring           = minus(s)
    def *  (factor: Float): Spring = scale(s, factor)
  }

  implicit final class RichConstraints(val c: SpringLayout.Constraints) extends AnyVal {
    def x               : Spring  = c.getX
    def x_=       (value: Spring) { c.setX     (value) }
    def y               : Spring  = c.getY
    def y_=       (value: Spring) { c.setY     (value) }
    def width           : Spring  = c.getWidth
    def width_=   (value: Spring) { c.setWidth (value) }
    def height          : Spring  = c.getHeight
    def height_=  (value: Spring) { c.setHeight(value) }
    def left            : Spring  = c.getConstraint(SpringLayout.WEST             )
    def left_=    (value: Spring) { c.setConstraint(SpringLayout.WEST             , value) }
    def top             : Spring  = c.getConstraint(SpringLayout.NORTH            )
    def top_=     (value: Spring) { c.setConstraint(SpringLayout.NORTH            , value) }
    def right           : Spring  = c.getConstraint(SpringLayout.EAST             )
    def right_=   (value: Spring) { c.setConstraint(SpringLayout.EAST             , value) }
    def bottom          : Spring  = c.getConstraint(SpringLayout.SOUTH            )
    def bottom_=  (value: Spring) { c.setConstraint(SpringLayout.SOUTH            , value) }
    def baseline        : Spring  = c.getConstraint(SpringLayout.BASELINE         )
    def baseline_=(value: Spring) { c.setConstraint(SpringLayout.BASELINE         , value) }
    def centerX         : Spring  = c.getConstraint(SpringLayout.HORIZONTAL_CENTER)
    def centerX_= (value: Spring) { c.setConstraint(SpringLayout.HORIZONTAL_CENTER, value) }
    def centerY         : Spring  = c.getConstraint(SpringLayout.VERTICAL_CENTER  )
    def centerY_= (value: Spring) { c.setConstraint(SpringLayout.VERTICAL_CENTER  , value) }
  }
}