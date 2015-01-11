package de.sciss.guiflitz

import de.sciss.model.Model

import scala.swing.event.ValueChanged
import scala.swing.{Button, BorderPanel, Slider, Frame, MainFrame, SimpleSwingApplication}

object CustomViewApp extends SimpleSwingApplication {
  // note: must not be a case class because it is automatically handled
  object Percent {
    def apply(value: Int): Percent = new Percent(value)
    def unapply(p: Percent): Option[Int] = Some(p.value)
  }
  class Percent(val value: Int)

  case class Task(name: String, progress: Percent)

  lazy val top: Frame = new MainFrame {
    val c = AutoView.Config()
    c.addViewFactory { (p: Cell[Percent]) =>
      new Slider { sl =>
        min   = 0
        max   = 100
        value = p().value

        val l: Model.Listener[Percent] = {
          case Percent(n) => value = n
        }
        p.addListener(l)

        listenTo(this)
        reactions += {
          case ValueChanged(_) =>
            p.removeListener(l)
            p() = Percent(sl.value)
            p.addListener   (l)
        }
      }
    }

    val av = AutoView(Task("foo", Percent(50)), c)
    val butRandom = Button("Random") {
      av.cell() = av.cell().copy(progress = Percent(util.Random.nextInt(100)))
    }
    av.cell.addListener {
      case Task(_, Percent(n)) => title = s"Value is $n"
    }

    contents = new BorderPanel {
      add(av.component, BorderPanel.Position.Center)
      add(butRandom   , BorderPanel.Position.South )
    }
  }
}
