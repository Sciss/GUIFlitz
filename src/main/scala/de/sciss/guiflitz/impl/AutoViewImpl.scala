/*
 *  AutoViewImpl.scala
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
package impl

import reflect.runtime.{universe => ru}
import ru.{Type, TypeTag}
import javax.swing.{JComponent, SpinnerNumberModel}
import de.sciss.swingplus.Spinner
import scala.swing.event.{ButtonClicked, ValueChanged}
import collection.immutable.{IndexedSeq => Vec}
import scala.swing.{ScrollPane, Component, Swing, CheckBox, TextField}
import language.existentials
import de.sciss.model.Model
import scala.annotation.tailrec

private[guiflitz] object AutoViewImpl {
  private final val PROP_TOP = "de.sciss.guiflitz.top"

  import AutoView.Config

  private[impl] type Tuple = (Model[Any], Component)

  private[impl] def log(what: => String): Unit =
    if (AutoView.showLog) println(s"<auto-view> $what")

  def apply[A: TypeTag](init: A, config: Config): AutoView[A] = {
    val shape         = Shape[A]
    val (cell, comp)  = mkView(init, shape, config, nested = false)
    comp.peer.putClientProperty(PROP_TOP, true)
    val cellC         = cell.asInstanceOf[Cell[A]]  // grmpff....
    val compS         = if (config.scroll) {
      val scroll    = new ScrollPane(comp)
      scroll.border = Swing.EmptyBorder
      scroll
    } else {
      comp
    }
    new Impl(config, cellC, compS)
  }

  @inline private def mkSmall(comp: Component, config: Config): Unit =
    if (config.small) {
      def apply(c: JComponent): Unit = {
        c.putClientProperty("JComponent.sizeVariant", "small")
        c.getComponents.foreach {
          case jc: JComponent => apply(jc)
          case _ =>
        }
      }
      apply(comp.peer)
    }

  private[impl] def mkView(init: Any, shape: Shape, config: Config, nested: Boolean): Tuple = {
    val res: Tuple = (init, shape) match {
      case (i: Int      , Shape.Int               )  => mkIntSpinner   (i)
      case (d: Double   , Shape.Double            )  => mkDoubleSpinner(d)
      case (s: String   , Shape.String            )  => mkTextField    (s)
      case (b: Boolean  , Shape.Boolean           )  => mkCheckBox     (b)
      case (_           , Shape.Unit              )  => mkDummy        ()
      case (p: Product  , Shape.Product(tpe, args))  => mkProduct      (p, tpe, args, config, nested = nested)
      case (_           , v @ Shape.Variant(_, _ ))  => mkVariant      (init, v, config)
      case (v: Vec[_]   , Shape.Vector(_, cs)     )  => mkVector       (v, cs, config)
      case (o: Option[_], Shape.Option(_, cs)     )  => mkOption       (o, cs, config)
      // case (_,          Shape.Other(tpe)        )  => mkLabel(tpe)
      case _ => throw new IllegalArgumentException(s"Shape $shape has no supported view")
    }
    mkSmall(res._2, config)
    res
  }

  private def mkDummy(): Tuple = {
    val cell  = Cell[Unit]()
    val comp  = Swing.HStrut(0)
    (cell, comp)
  }

  // ---------------------------------- Vector ----------------------------------

  @inline private def mkVector(init: Vec[Any], childShape: Shape, config: Config): Tuple =
    VectorView(init, childShape, config)

  // ---------------------------------- Int / Double Spinner ----------------------------------

  private def mkIntSpinner(init: Int): Tuple =
    mkSpinner[Int   ](new SpinnerNumberModel(init, Int   .MinValue, Int   .MaxValue, 1))(_.intValue   ())

  private def mkDoubleSpinner(init: Double): Tuple =
    mkSpinner[Double](new SpinnerNumberModel(init, Double.MinValue, Double.MaxValue, 1))(_.doubleValue())

  private def mkSpinner[A](m: SpinnerNumberModel)(fun: Number => A): Tuple = {
    val cell  = Cell(fun(m.getNumber))
    // val m     = new SpinnerNumberModel(init, Double.PositiveInfinity, Double.NegativeInfinity, 1)
    val l: Cell.Listener[A] = {
      case value => m.setValue(value)
    }
    val comp  = new Spinner(m) {
      preferredSize = {
        val d   = preferredSize
        d.width = 128
        d
      }
      listenTo(this)
      reactions += {
        case ValueChanged(_) =>
          cell.removeListener(l)
          cell() = fun(m.getNumber)
          cell.addListener(l)
      }
    }
    cell.addListener(l)
    (cell, comp)
  }

  // ---------------------------------- Variant ----------------------------------

  @inline private def mkVariant(init: Any, v: Shape.Variant, config: Config): Tuple =
    new VariantView(init, v, config).tuple

  // ---------------------------------- Option ----------------------------------

  @inline private def mkOption(init: Option[Any], sub: Type, config: Config): Tuple =
    new OptionView(init, sub, config).tuple

  // ---------------------------------- Text Field ----------------------------------

  private def mkTextField(init: String): Tuple = {
    val cell  = Cell(init)
    val comp  = new TextField(init, 10) {
      val l: Cell.Listener[String] = {
        case value => text = value
      }
      listenTo(this)
      reactions += {
        case ValueChanged(_) =>
          cell.removeListener(l)
          cell() = text
          cell.addListener(l)
      }
      cell.addListener(l)
    }

    (cell, comp)
  }

  // ---------------------------------- Check Box ----------------------------------

  private def mkCheckBox(init: Boolean): Tuple = {
    val cell  = Cell(init)
    val comp  = new CheckBox {
      selected = init
      val l: Cell.Listener[Boolean] = {
        case value => selected = value
      }
      listenTo(this)
      reactions += {
        case ButtonClicked(_) =>
          cell.removeListener(l)
          cell() = selected
          cell.addListener(l)
      }
      cell.addListener(l)
    }
    (cell, comp)
  }

  // ---------------------------------- Product ----------------------------------

  @inline private def mkProduct(init: Product, tpe: Type, args: Vec[Shape.Arg], config: Config,
                                nested: Boolean): Tuple = ProductView(init, tpe, args, config, nested)

  // --------------------------------------------------------------------

  private[impl] def revalidate(comp: Component): Unit = {
    @tailrec def loop(c: JComponent): Unit =
      if (c.getClientProperty(PROP_TOP) == true) {
        c.revalidate()
        c.repaint()
      } else c.getParent match {
        case p: JComponent => loop(p)
        case _ =>
      }

    loop(comp.peer)
  }

  private final class Impl[A](val config: Config, val cell: Cell[A], val component: Component) extends AutoView[A] {
    override def toString = s"AutoView@${hashCode().toHexString}"

    def value: A = cell()
  }
}