/*
 *  VectorView.scala
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
package impl

import javax.swing.{JSeparator, JToolBar, SpringLayout}

import de.sciss.guiflitz.AutoView.Config

import scala.collection.immutable.{IndexedSeq => Vec}
import scala.swing.{BorderPanel, BoxPanel, Button, Component, FlowPanel, Orientation, ScrollPane}

import scala.language.existentials

object VectorView {
  import de.sciss.guiflitz.impl.AutoViewImpl.{Tuple, mkView, revalidate}

  def apply(init: Vec[Any], childShape: Shape, config: Config): Tuple = {
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
}

