/*
 *  AutoViewImpl.scala
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
package impl

import reflect.runtime.{universe => ru, currentMirror => cm}
import ru.{Type, TypeTag}
import javax.swing.{SpringLayout, JToolBar, JSeparator, JComponent, Spring, SpinnerNumberModel}
import de.sciss.swingplus.Spinner
import de.sciss.swingplus.Implicits._
import scala.swing.event.{SelectionChanged, ButtonClicked, ValueChanged}
import collection.immutable.{IndexedSeq => Vec}
import scala.swing.{BorderPanel, Button, ScrollPane, FlowPanel, Alignment, Label, Component, Swing, CheckBox, BoxPanel, Orientation, TextField, ComboBox}
import language.existentials
import de.sciss.model.Model
import Swing._
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

  private def mkVector(init: Vec[Any], childShape: Shape, config: Config): Tuple = {
    type Child      = (Cell[Any], Cell.Listener[Any])

    val cell        = Cell(init)
    val pane        = new BoxPanel(Orientation.Vertical)
    var children    = Vec.empty[Child]

    lazy val lVec: Cell.Listener[Vec[Any]] = {
      case newVec =>
        // println(s"vec update")
        val trunc = math.max(0, children.size - newVec.size)
        val inc   = math.max(0, newVec.size - children.size)

        if (trunc > 0) {
          val (newChildren, toDispose) = children.splitAt(newVec.size)
          toDispose.foreach { case (childCell, lChild) => childCell.removeListener(lChild) }
          pane.contents.trimEnd(math.min(pane.contents.size, trunc * 2))
          children = newChildren
        }

        (children zip newVec).foreach { case ((childCell, lChild), elem) =>
          childCell.removeListener(lChild)
          childCell.update(elem)
          childCell.addListener(lChild)
        }

        if (inc > 0) {
          val off = children.size
          newVec.takeRight(inc).zipWithIndex.foreach { case (elem, idx) =>
            children :+= mkChild(off + idx, elem)
          }
        }

        if (trunc > 0 || inc > 0) {
          revalidate(pane)
        }
    }
    cell.addListener(lVec)

    def mkChild(idx: Int, elem: Any): Child = {
      if (idx > 0) {
        val sep        = Component.wrap(new JSeparator())
        // sep.border     = EmptyBorder(top = 2, left = 0, bottom = 2, right = 0)
        pane.contents += sep
      }
      val (childCell, childComp) = mkView(init = elem, shape = childShape, config = config, nested = false)
      pane.contents += (childComp.peer.getLayout match {
        case _: SpringLayout  => new FlowPanel(childComp)  // XXX TODO: bug with spring layout
        case _                => childComp
      })

      lazy val lChild: Cell.Listener[Any] = {
        case newElem =>
          cell.removeListener(lVec)
          cell() = cell().updated(idx, newElem)
          cell.addListener(lVec)
      }

      childCell.addListener(lChild)
      (childCell.asInstanceOf[Cell[Any]], lChild)
    }

    children = init.zipWithIndex.map { case (elem, idx) =>
      mkChild(idx, elem)
    }

    val scroll  = new ScrollPane(pane)
    val tbj     = new JToolBar
    tbj.setFloatable(false)
    tbj.setBorderPainted(false)
    val tb      = Component.wrap(tbj)
    val ggAdd   = Button("+") {
      cell() = cell() :+ childShape.instantiate()
    }
    ggAdd.peer.putClientProperty("JButton.buttonType", "roundRect")
    val ggRemove = Button("\u2212") {
      cell() = cell().dropRight(1)
    }
    ggRemove.peer.putClientProperty("JButton.buttonType", "roundRect")
    tbj.add(ggAdd   .peer)
    tbj.add(ggRemove.peer)

    val comp = new BorderPanel {
      layoutManager.setVgap(0)
      add(scroll, BorderPanel.Position.Center)
      add(tb    , BorderPanel.Position.South )
    }

    (cell, comp)
  }

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

  private def mkVariant(init: Any, v: Shape.Variant, config: Config): Tuple = new VariantView(init, v, config).tuple

  // ---------------------------------- Option ----------------------------------

  private def mkOption(init: Option[Any], childShape: Shape, config: Config): Tuple =
    new OptionView(init, childShape, config).tuple

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

  private def mkProduct(init: Product, tpe: Type, args: Vec[Shape.Arg], config: Config,
                        nested: Boolean): Tuple = {
    val cell    = Cell(init)
    val comp    = new SpringPanel
    val edge    = Swing.EtchedBorder
    comp.border = if (nested) edge else Swing.TitledBorder(edge, tpe.typeSymbol.name.toString)
    import comp.cons

    import Springs._

    class MaxWidthSpring(col: Int) extends Spring {
      private def reduce(op: Component => Int): Int = {
        comp.contents.zipWithIndex.foldLeft(0) { case (res, (c, idx)) =>
          if (idx % 2 == col) math.max(res, op(c)) else res
        }
      }

      private var _value = Spring.UNSET

      //      def getMinimumValue  : Int = reduce(_.minimumSize  .width)
      //      def getPreferredValue: Int = reduce(_.preferredSize.width)
      //      def getMaximumValue  : Int = reduce(_.maximumSize  .width)

      lazy val getMinimumValue  : Int = reduce(_.minimumSize  .width)
      lazy val getPreferredValue: Int = reduce(_.preferredSize.width)
      lazy val getMaximumValue  : Int = reduce(_.maximumSize  .width)

      def getValue         : Int = _value // if (_value == Spring.UNSET) reduce(_.value) else _value

      def setValue(value: Int) {
        if (_value != value) {
          _value = value
          // comp.contents.foreach(c => cons(c).width.value = value)
        }
      }
    }

    // dynamic max spring
    val lbLeftSpring  = 4: Spring
    val lbWidthSpring = new MaxWidthSpring(0)
    val ggLeftSpring  = lbLeftSpring + lbWidthSpring + 4
    val ggWidthSpring = new MaxWidthSpring(1)

    def invokeGetter(idx: Int): Any = {
      val mGetter     = tpe.member(ru.newTermName(s"copy$$default$$${idx+1}")).asMethod
      val im          = cm.reflect(cell())
      val mm          = im.reflectMethod(mGetter)
      mm()
    }

    def copy(idx: Int, value: Any): Product = {
      val v               = (0 until args.size).map(j => if (j == idx) value else invokeGetter(j))
      val (m, _, mApply)  = Shape.getApplyMethod(tpe.typeSymbol)
      m.reflectMethod(mApply)(v: _*).asInstanceOf[Product]
    }

    var bottomSpring = 0: Spring

    args.zipWithIndex.foreach { case (arg, idx) =>
      val lb = new Label(s"${arg.name.capitalize}:", null, Alignment.Trailing)
      // mkSmall(lb, config)
      val (avCell, avComp) = mkView(invokeGetter(idx), arg.shape, config, nested = true)

      lazy val l2: Cell.Listener[Any] = {
        case value =>
          cell.removeListener(l1)
          cell() = copy(idx, value)
          cell.addListener(l1)
      }
      avCell.addListener(l2)

      lazy val l1: Cell.Listener[Product] = {
        case value =>
          avCell.removeListener(l2)
          avCell.asInstanceOf[Cell[Any]].update(invokeGetter(idx))  // grmpff...
          avCell.addListener(l2)
      }
      cell.addListener(l1)

      val gg = avComp
      comp.contents  += lb
      comp.contents  += gg
      val clb         = cons(lb)
      val cgg         = cons(gg)
      clb.left        = lbLeftSpring
      val topSpring   = bottomSpring + 4
      clb.top         = topSpring
      clb.width       = lbWidthSpring
      cgg.left        = ggLeftSpring
      cgg.top         = topSpring
      if (gg.baseline >= 0) {
        clb.baseline  = cgg.baseline
      } else {
        clb.height    = cgg.height
      }

      bottomSpring    = clb.bottom max cgg.bottom
    }

    val ccomp = cons(comp)
    ccomp.right   = ggLeftSpring + ggWidthSpring + Spring.constant(4, 4, Int.MaxValue)
    ccomp.bottom  = bottomSpring + Spring.constant(4, 4, Int.MaxValue)

    (cell, comp)
  }

  // --------------------------------------------------------------------

  private[impl] def revalidate(comp: Component): Unit = {
    // XXX TODO: remove. this is here because of migration-manager (bin compat)
    /* @tailrec */ def loop(c: JComponent): Unit =
      // WTF? re-layout only works properly if we detach nested containers by putting them on successive EDT calls
      Swing.onEDT {
        c.revalidate()
        c.repaint()
        if (c.getClientProperty(PROP_TOP) != true) c.getParent match {
          case p: JComponent => loop(p)
          case _ =>
        }
      }

    //    @tailrec def loop(c: JComponent): Unit =
    //      // Swing.onEDT
    //      {
    //        c.revalidate()
    //        c.repaint()
    //        if (c.getClientProperty(PROP_TOP) != true) c.getParent match {
    //          case p: JComponent => loop(p)
    //          case _ =>
    //        }
    //      }

    @tailrec def loopNew(c: JComponent): Unit =
      if (c.getClientProperty(PROP_TOP) == true) {
        c.revalidate()
        c.repaint()
      } else c.getParent match {
        case p: JComponent => loopNew(p)
        case _ =>
      }

    loopNew(comp.peer)
  }

  private final class Impl[A](val config: Config, val cell: Cell[A], val component: Component) extends AutoView[A] {
    override def toString = s"AutoView@${hashCode().toHexString}"

    def value: A = cell()
  }
}