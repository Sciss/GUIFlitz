/*
 *  ProductView.scala
 *  (GUIFlitz)
 *
 *  Copyright (c) 2013-2014 Hanns Holger Rutz. All rights reserved.
 *
 *  This software is published under the GNU Lesser General Public License v2.1+
 *
 *  For further information, please contact Hanns Holger Rutz at
 *  contact@sciss.de
 */

package de.sciss.guiflitz
package impl

import scala.collection.immutable.{IndexedSeq => Vec}
import AutoView.Config
import scala.swing.{Insets, TextField, CheckBox, ComboBox, GridBagPanel, Alignment, Label, Swing}
import scala.reflect.runtime.{universe => ru, currentMirror => cm}
import ru.Type
import scala.swing.GridBagPanel.{Anchor, Fill}
import de.sciss.swingplus.Spinner

// import de.sciss.swingplus.Implicits._

object ProductView {
  import AutoViewImpl.{Tuple, mkView}

  private val insLabel  = new Insets(1, 8, 1, 2)
  private val insView   = new Insets(1, 2, 1, 4)

  def apply(init: Product, tpe: Type, args: Vec[Shape.Arg], config: Config,
                        nested: Boolean): Tuple = {
    val cell    = Cell(init)
    val comp    = new GridBagPanel
    val cons    = new comp.Constraints()
    // cons.ipadx  = 8
    // cons.ipady  = 8
    // cons.insets = new Insets(1, 2, 1, 2)
    val edge    = Swing.EtchedBorder
    comp.border = if (nested) edge else Swing.TitledBorder(edge, tpe.typeSymbol.name.toString)

    def invokeGetter(idx: Int): Any = {
      val mGetter     = tpe.member(ru.newTermName(s"copy$$default$$${idx+1}")).asMethod
      val im          = cm.reflect(cell())
      val mm          = im.reflectMethod(mGetter)
      mm()
    }

    def copy(idx: Int, value: Any): Product = {
      val v               = (0 until args.size).map(j => if (j == idx) value else invokeGetter(j))
      val (m, _, mApply)  = Shape.getApplyMethod(tpe.typeSymbol)
      val res             = m.reflectMethod(mApply)(v: _*)
      res.asInstanceOf[Product]
    }

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

      cons.gridx  = 0
      cons.gridy  = idx
      cons.insets = insLabel
      cons.fill   = Fill.None
      cons.anchor = Anchor.East
      comp.layout(lb) = cons
      cons.gridx  = 1
      cons.insets = insView
      cons.fill   = avComp match {
        case _: ComboBox[_] => Fill.None
        case _: Spinner     => Fill.Horizontal
        case _: CheckBox    => Fill.Horizontal
        case _: TextField   => Fill.Horizontal
        case _              => Fill.Both
      }
      cons.anchor = Anchor.West
      comp.layout(gg) = cons
   }

    (cell, comp)
  }
}
