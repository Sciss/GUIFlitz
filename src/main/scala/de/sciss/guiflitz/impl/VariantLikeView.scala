package de.sciss.guiflitz.impl

import de.sciss.guiflitz.{Shape, Cell}
import scala.swing.{CheckBox, Component, ComboBox, FlowPanel, Swing, Orientation, BoxPanel}
import scala.swing.event.{ButtonClicked, SelectionChanged}
import de.sciss.guiflitz.AutoView.Config

abstract class VariantLikeView(init: Any, config: Config) {
  import AutoViewImpl.{log, mkView, revalidate, Tuple}

  protected def comp: Component

  protected def addChild(child: Component): Unit

  protected def removeChild(): Boolean

  protected def cellListener: Cell.Listener[Any]

  protected val cell  = Cell(init)
  private var ggVar   = Option.empty[Tuple]

  private lazy val subL: Cell.Listener[Any] = {
    case subV =>
      cell.removeListener(cellListener)
      cell() = subV
      cell.addListener   (cellListener)
  }

  final protected def updateGUI(newShape: Option[Shape]): Unit = {
    log("updateGUI")
    var dirty = false
    ggVar.foreach { case (oldCell, oldComp) =>
      log("remove old sub view")
      oldCell.removeListener(subL)
      ggVar = None
      dirty = removeChild()
    }
    newShape.foreach {
      case Shape.Other(_) =>  // no additional view
        log("new value has no view")
      case shp =>
        log("adding new sub view")
        val newView @ (newCell, newComp) = mkView(cell(), shp, config, nested = true)
        newCell.addListener(subL)
        addChild(newComp)
        ggVar = Some(newView)
        dirty = true
    }
    if (dirty) {
      revalidate(comp)
    }
  }

  cellListener(init)
  cell.addListener(cellListener)

  final def tuple: Tuple = (cell, comp)
}

final class VariantView(init: Any, v: Shape.Variant, config: Config) extends VariantLikeView(init, config) {
  private val items = v.sub.map(_.typeSymbol.name.toString)

  protected lazy val comp: BoxPanel = new BoxPanel(Orientation.Vertical) {
    override lazy val peer = {
      val p = new javax.swing.JPanel with SuperMixin {
        // XXX TODO: this should allow us to align the outer label properly
        // so that it always appears next to the combo box. Unfortunately, that doesn't work.
        override def getBaseline(width: Int, height: Int) = combo.peer.getBaseline(width, height)
      }
      val l = new javax.swing.BoxLayout(p, Orientation.Vertical.id)
      p.setLayout(l)
      p
    }
  }

  protected def addChild(child: Component): Unit = {
    comp.contents += Swing.VStrut(4)
    // there is a problem with directly adding an AutoView[Product] component
    // (i.e. SpringPanel) -- for whatever reason, if we put that into another
    // container, such as FlowPanel, we avoid layout garbage.
    comp.contents += new FlowPanel(child)  // Button("Hallo") {}
  }

  protected def removeChild(): Boolean =
    if (comp.contents.size == 3) {
      comp.contents.remove(1, 2)
      true
    } else {
      false
    }

  private lazy val combo: ComboBox[String] = new ComboBox(items) {
    listenTo(selection)
    reactions += gl
    peer.putClientProperty("JComboBox.isSquare", true)
  }

  private def updateGUI(idx: Int): Unit = {
    val shape = Shape.fromType(v.sub(idx))
    updateGUI(Some(shape))
  }

  protected lazy val cellListener: Cell.Listener[Any] = { case value =>
    val valTpe = v.find(value)
    val valIdx = valTpe.map(t => v.sub.indexOf(t)).getOrElse(-1)
    if (valIdx >= 0) {
      combo.reactions -= gl
      combo.selection.index = valIdx
      combo.reactions += gl
      updateGUI(valIdx)
    }
  }

  private lazy val gl: PartialFunction[Any, Unit] = {
    case SelectionChanged(_) =>
      cell.removeListener(cellListener)
      val idx = combo.selection.index
      if (idx >= 0) try {
        val subShape  = Shape.fromType(v.sub(idx))
        val subObj    = subShape.instantiate()
        cell()        = subObj
      } finally {
        cell.addListener(cellListener)
        updateGUI(idx)
      }
  }

  comp.contents += combo
}

final class OptionView(init: Any, childShape: Shape, config: Config) extends VariantLikeView(init, config) {
  // private val items = v.sub.map(_.typeSymbol.name.toString)

  protected lazy val comp: BoxPanel = new BoxPanel(Orientation.Horizontal)

  protected def addChild(child: Component): Unit = {
    comp.contents += Swing.VStrut(4)
    // there is a problem with directly adding an AutoView[Product] component
    // (i.e. SpringPanel) -- for whatever reason, if we put that into another
    // container, such as FlowPanel, we avoid layout garbage.
    comp.contents += new FlowPanel(child)  // Button("Hallo") {}
  }

  protected def removeChild(): Boolean =
    if (comp.contents.size == 3) {
      comp.contents.remove(1, 2)
      true
    } else {
      false
    }

  private lazy val check: CheckBox = new CheckBox {
    listenTo(this)
    reactions += gl
  }

  protected lazy val cellListener: Cell.Listener[Any] = { case value: Option[_] =>
    val selected     = value.isDefined
    check.reactions -= gl
    check.selected   = selected
    check.reactions += gl
    updateGUI(if (selected) Some(childShape) else None)
  }

  private lazy val gl: PartialFunction[Any, Unit] = {
    case ButtonClicked(_) =>
      cell.removeListener(cellListener)
      if (check.selected) try {
        val subObj    = childShape.instantiate()
        cell()        = subObj
      } finally {
        cell.addListener(cellListener)
        updateGUI(None)
      }
  }

  comp.contents += check
}