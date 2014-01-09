package de.sciss.guiflitz
package impl

import scala.collection.immutable.{IndexedSeq => Vec}
import AutoView.Config
import scala.swing.{Alignment, Label, Component, Swing}
import javax.swing.Spring
import scala.reflect.runtime.{universe => ru, currentMirror => cm}
import ru.Type
import de.sciss.swingplus.Implicits._

object ProductView {
  import AutoViewImpl.{Tuple, mkView}

  def apply(init: Product, tpe: Type, args: Vec[Shape.Arg], config: Config,
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
      val res             = m.reflectMethod(mApply)(v: _*)
      res.asInstanceOf[Product]
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
}
