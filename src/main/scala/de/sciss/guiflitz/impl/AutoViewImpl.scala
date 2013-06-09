package de.sciss.guiflitz
package impl

import reflect.runtime.{universe => ru, currentMirror => cm}
import ru.{Type, TypeTag}
import javax.swing.{Spring, SpinnerNumberModel}
import de.sciss.swingplus.Spinner
import scala.swing._
import scala.swing.event.ValueChanged
import collection.immutable.{IndexedSeq => Vec}

private[guiflitz] object AutoViewImpl {

  def apply[A: TypeTag](init: A): AutoView[A] = {
    val shape = Shape[A]
    val view  = mkView(init, shape)
    view.asInstanceOf[AutoView[A]]
  }

  private def mkView(init: Any, shape: Shape): AutoView[_] =
    (init, shape) match {
      case (i: Int    , Shape.Int               )  =>
        mkSpinner[Int](new SpinnerNumberModel(i, Int.MinValue, Int.MaxValue, 1))(_.intValue())
      case (d: Double , Shape.Double            )  =>
        mkSpinner[Double](new SpinnerNumberModel(d, Double.MinValue, Double.MaxValue, 1))(_.doubleValue())
      case (s: String , Shape.String            )  => mkTextField(s)
      case (p: Product, Shape.Product(tpe, args))  => mkProduct(p, tpe, args)
      case _                          => throw new IllegalArgumentException(s"Shape $shape has no supported view")
    }

  private def mkSpinner[A](m: SpinnerNumberModel)(fun: Number => A): AutoView[A] = {
    val cell  = Cell(fun(m.getNumber))
    // val m     = new SpinnerNumberModel(init, Double.PositiveInfinity, Double.NegativeInfinity, 1)
    val l: Cell.Listener[A] = {
      case value => m.setValue(value)
    }
    cell.addListener(l)
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
    new Impl(cell, comp)
  }

  private def mkTextField(init: String): AutoView[String] = {
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
    }
    new Impl(cell, comp)
  }

  private def mkProduct(init: Product, tpe: Type, args: Vec[Shape.Arg]): AutoView[Product] = {
    val cell    = Cell(init)
    val comp    = new SpringPanel
    comp.border = Swing.TitledBorder(Swing.EtchedBorder, tpe.typeSymbol.name.toString)
    import comp.cons

    import Springs._

    class MaxWidthSpring(col: Int) extends Spring {
      private def reduce(op: Component => Int): Int = {
        comp.contents.zipWithIndex.foldLeft(0) { case (res, (c, idx)) =>
          if (idx % 2 == col) math.max(res, op(c)) else res
        }
      }

      private var _value = Spring.UNSET

      def getMinimumValue  : Int = reduce(_.minimumSize  .width)
      def getPreferredValue: Int = reduce(_.preferredSize.width)
      def getMaximumValue  : Int = reduce(_.maximumSize  .width)
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
      val av = mkView(invokeGetter(idx), arg.shape).asInstanceOf[AutoView[Any]]

      lazy val l2: Cell.Listener[Any] = {
        case value =>
          cell.removeListener(l1)
          cell() = copy(idx, value)
          cell.addListener(l1)
      }
      av.cell.addListener(l2)

      lazy val l1: Cell.Listener[Product] = {
        case value =>
          av.cell.removeListener(l2)
          av.cell() = invokeGetter(idx)
          av.cell.addListener(l2)
      }
      cell.addListener(l1)

      val gg = av.component
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
      clb.baseline    = cgg.baseline

      bottomSpring    = clb.bottom max cgg.bottom
    }

    val ccomp = cons(comp)
    ccomp.right   = ggLeftSpring + ggWidthSpring + Spring.constant(4, 4, Int.MaxValue)
    ccomp.bottom  = bottomSpring + Spring.constant(4, 4, Int.MaxValue)

    new Impl(cell, comp)
  }

  private final class Impl[A](val cell: Cell[A], val component: Component) extends AutoView[A] {
    override def toString = s"AutoView@${hashCode().toHexString}"

    def value: A = cell()

    // private val lens = Lenser[A]
    // lens.name.set(value)("hallo")
  }}
