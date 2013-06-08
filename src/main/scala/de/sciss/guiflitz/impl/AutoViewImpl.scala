package de.sciss.guiflitz
package impl

import reflect.runtime.{universe => ru, currentMirror => cm}
import ru.{Type, TypeTag}
import javax.swing.SpinnerNumberModel
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
      case (i: Int    , Shape.Int               )  => mkIntSpinner(i)
      case (s: String , Shape.String            )  => mkTextField(s)
      case (p: Product, Shape.Product(tpe, args))  => mkProduct(p, tpe, args)
      case _                          => throw new IllegalArgumentException(s"Shape $shape has no supported view")
    }

  private def mkIntSpinner(init: Int): AutoView[Int] = {
    val cell  = Cell(init)
    val m     = new SpinnerNumberModel(init, Int.MinValue, Int.MaxValue, 1)
    val l: Cell.Listener[Int] = {
      case value => m.setValue(value)
    }
    cell.addListener(l)
    val comp  = new Spinner(m) {
      listenTo(this)
      reactions += {
        case ValueChanged(_) =>
          cell.removeListener(l)
          cell() = m.getNumber.intValue()
          cell.addListener(l)
      }
    }
    new Impl(cell, comp)
  }

  private def mkTextField(init: String): AutoView[String] = {
    val cell  = Cell(init)
    val comp  = new TextField(init, 16) {
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
    val cell  = Cell(init)
    val comp  = new GridPanel(args.size, 2) {
      border  = Swing.TitledBorder(Swing.EtchedBorder, tpe.typeSymbol.name.toString)
    }

    args.zipWithIndex.foreach { case (arg, idx) =>
      val lb      = new Label(s"${arg.name}:", null, Alignment.Trailing)
      // val at      = arg.shape.tpe
      val mGetter = tpe.member(ru.newTermName(s"copy$$default$$${idx+1}")).asMethod
      // val im      = cm.reflectClass(tpe) // .typeSymbol.asClass)
      val im      = cm.reflect(cell())
      val mm      = im.reflectMethod(mGetter)
      val ai      = mm()
      val av  = mkView(ai, arg.shape)
      comp.contents += lb
      comp.contents += av.component
    }
    new Impl(cell, comp)
  }

  private final class Impl[A](val cell: Cell[A], val component: Component) extends AutoView[A] {
    override def toString = s"AutoView@${hashCode().toHexString}"

    def value: A = cell()

    // private val lens = Lenser[A]
    // lens.name.set(value)("hallo")
  }}
