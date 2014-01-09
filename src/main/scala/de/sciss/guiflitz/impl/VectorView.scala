/*
 *  VectorView.scala
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

import scala.swing.{BorderPanel, Button, ScrollPane, FlowPanel, Component, Orientation, BoxPanel}
import scala.collection.immutable.{IndexedSeq => Vec}
import javax.swing.{JToolBar, SpringLayout, JSeparator}
import AutoView.Config

object VectorView {
  import AutoViewImpl.{Tuple, revalidate, mkView}

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

