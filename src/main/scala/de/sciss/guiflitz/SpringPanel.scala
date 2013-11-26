/*
 *  SpringPanel.scala
 *  (GUIFlitz)
 *
 *  Copyright (c) 2013 Hanns Holger Rutz. All rights reserved.
 *
 *  This software is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License
 *  as published by the Free Software Foundation; either
 *  version 2, june 1991 of the License, or (at your option) any later version.
 *
 *  This software is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public
 *  License (gpl.txt) along with this software; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 *
 *  For further information, please contact Hanns Holger Rutz at
 *  contact@sciss.de
 */

package de.sciss.guiflitz

import scala.swing.{SequentialContainer, Orientation, Component, Panel}
import Springs._
import language.implicitConversions
import javax.swing.{SpringLayout, SwingConstants, LayoutStyle, Spring}
import java.awt.Container

object SpringPanel {
  sealed trait HorizontalAlignment
  sealed trait VerticalAlignment
  case object Left      extends HorizontalAlignment
  case object Center    extends HorizontalAlignment with VerticalAlignment
  // case object Justified extends HorizontalAlignment with VerticalAlignment
  case object Right     extends HorizontalAlignment
  case object Top       extends VerticalAlignment
  case object Bottom    extends VerticalAlignment
  case object Baseline  extends VerticalAlignment

  object Gap {
    implicit def wrap[A <% Spring](spring: A): Gap = Wrap(spring)

    case class Wrap(spring: Spring) extends Gap
  }
  sealed trait Gap

  sealed trait Relation extends Gap {
    private[SpringPanel] def toJava: LayoutStyle.ComponentPlacement
  }
  case object Indent extends Relation {
    private[SpringPanel] def toJava = LayoutStyle.ComponentPlacement.INDENT
  }
  case object Related extends Relation {
    private[SpringPanel] def toJava = LayoutStyle.ComponentPlacement.RELATED
  }
  case object Unrelated extends Relation {
    private[SpringPanel] def toJava = LayoutStyle.ComponentPlacement.UNRELATED
  }

  //  def toSpring(c1: Component, c2: Component, orient: Orientation.Value, relation: Relation, parent: SpringPanel): Spring
  private final class RelationSpring(c1: Component, c2: Component, orient: Orientation.Value, relation: Relation,
                                     parent: SpringPanel) extends Spring {
    def getMinimumValue  : Int = getPreferredValue
    def getMaximumValue  : Int = getPreferredValue

    def getPreferredValue: Int =
      LayoutStyle.getInstance().getPreferredGap(c1.peer, c2.peer, relation.toJava,
        if (orient == Orientation.Horizontal) SwingConstants.EAST else SwingConstants.SOUTH, parent.peer)

    private var _value = Spring.UNSET

    def getValue      : Int         = if (_value != Spring.UNSET) _value else getPreferredValue
    def setValue(value: Int): Unit  = _value = value
  }

  private final val DEBUG = false

  private def debug(what: => String): Unit = if (DEBUG) println(s"[spring] $what")
}
class SpringPanel extends Panel with SequentialContainer.Wrapper { me =>
  import SpringPanel._

  private lazy val lay = new SpringLayout {
    override def invalidateLayout(p: Container): Unit = {
      debug(s"invalidateLayout(${p.hashCode().toHexString})")
      super.invalidateLayout(p)
    }

    override def layoutContainer(p: Container): Unit = {
      debug(s"layoutContainer(${p.hashCode().toHexString})")
      super.layoutContainer(p)
    }
  }
  override lazy val peer: javax.swing.JPanel = new javax.swing.JPanel(lay) with SuperMixin

  // lay.getConstraint()

  /** Retrieves the layout constraints of a component. */
  def cons(component: Component): SpringLayout.Constraints = lay.getConstraints(component.peer)

  private def pairWiseDo(xs: Seq[Component])(fun: (Component, Component) => Unit): Unit = {
    val it = xs.iterator
    if (it.isEmpty) return
    var succ = it.next()
    while (it.nonEmpty) {
      val pred  = succ
      succ      = it.next()
      fun(pred, succ)
    }
  }

  def hseq(xs: Component*): Unit = hseq(Related)(xs: _*)

  /** Arranges components in horizontal succession. */
  def hseq(pad: Gap)(xs: Component*): Unit =
    pairWiseDo(xs) { (pred, succ) =>
      val pads = pad match {
        case w: Gap.Wrap => w.spring
        case r: Relation => new RelationSpring(pred, succ, Orientation.Horizontal, r, me)
      }
      cons(succ).left = cons(pred).right + pads
    }

  def vseq(xs: Component*): Unit = vseq(Related)(xs: _*)

  /** Arranges components in vertical succession. */
  def vseq(pad: Gap)(xs: Component*): Unit =
    pairWiseDo(xs) { (pred, succ) =>
      val pads = pad match {
        case w: Gap.Wrap => w.spring
        case r: Relation => new RelationSpring(pred, succ, Orientation.Vertical, r, me)
      }
      cons(succ).top = cons(pred).bottom + pads
    }

  /** Left aligns components horizontally. */
  def halign(xs: Component*): Unit = halign(Left)(xs: _*)

  /** Aligns components horizontally. */
  def halign(value: HorizontalAlignment)(xs: Component *): Unit = {
    val fun: (Component, Component) => Unit = value match {
      case Left      => (pred, succ) => cons(succ).left      = cons(pred).left
      case Center    => (pred, succ) => cons(succ).centerX   = cons(pred).centerX
      case Right     => (pred, succ) => cons(succ).right     = cons(pred).right
      //      case Justified => (pred, succ) =>
      //        cons(succ).left      = cons(pred).left
      //        cons(succ).right     = cons(pred).right
    }
    pairWiseDo(xs)(fun)
  }

  /** Aligns components vertically to their baselines. */
  def valign(xs: Component*): Unit = valign(Baseline)(xs: _*)

  /** Aligns components vertically. */
  def valign(value: VerticalAlignment)(xs: Component *): Unit = {
    val fun: (Component, Component) => Unit = value match {
      case Top      => (pred, succ) => cons(succ).top       = cons(pred).top
      case Center   => (pred, succ) => cons(succ).centerY   = cons(pred).centerY
      case Bottom   => (pred, succ) => cons(succ).bottom    = cons(pred).bottom
      case Baseline => (pred, succ) => cons(succ).baseline  = cons(pred).baseline
    }
    pairWiseDo(xs)(fun)
  }

  def linkWidth(xs: Component*): Unit = {
    val w = ((0: Spring) /: xs)(_ max cons(_).width)
    xs.foreach(cons(_).width = w)
  }

  def linkHeight(xs: Component*): Unit = {
    val h = ((0: Spring) /: xs)(_ max cons(_).height)
    xs.foreach(cons(_).height = h)
  }

  def linkSize(xs: Component*): Unit = {
    linkWidth (xs: _*)
    linkHeight(xs: _*)
  }
}