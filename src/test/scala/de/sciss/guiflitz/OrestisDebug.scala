package de.sciss.guiflitz

import collection.immutable.{IndexedSeq => Vec}
import scala.swing.{Swing, Button, Orientation, BoxPanel, BorderPanel, Frame, SimpleSwingApplication}
import Swing._
import de.sciss.swingplus.CloseOperation
import de.sciss.swingplus.Implicits._
import javax.swing.UIManager

object OrestisDebug extends SimpleSwingApplication {
  val DefaultVoices = Vec(
    Voice(lowest = 48, highest = 96, maxUp = 6, maxDown = 6),
    Voice(lowest = 36, highest = 84, maxUp = 6, maxDown = 6),
    Voice(lowest = 24, highest = 72, maxUp = 6, maxDown = 6)
  )

  case class Voice(maxUp: Int = 2, maxDown: Int = 2, lowest: Int = 36, highest: Int = 96)

  sealed trait VerticalConstraint

  case class ForbiddenInterval(steps: Int = 12) extends VerticalConstraint

  case class GlobalImpl(voices: Vec[Voice] = DefaultVoices)

  //  case class GlobalImpl(voices  : Vec[Voice] = DefaultVoices,
  //                        vertical: Vec[VerticalConstraint] = Vec(ForbiddenInterval(12)),
  //                        length  : Int = 12)

  // case class GenerationImpl(size: Int = 100, global: GlobalImpl = GlobalImpl(), seed: Int = 0)

  case class GenerationImpl(global: GlobalImpl = GlobalImpl())

  /////////////////////////////////

  val useNimbus = false

  val nimbusOption = if (!useNimbus) None else UIManager.getInstalledLookAndFeels.collectFirst {
    case info if info.getName == "Nimbus" => info.getClassName
  }
  nimbusOption.foreach(UIManager.setLookAndFeel)

  lazy val view = {
    val c = AutoView.Config()
    c.small = true
    AutoView(GenerationImpl(), c)
    // AutoView(GlobalImpl(), c)
  }

  lazy val postBut = {
    val res = Button("Post") { println(view.cell()) }
    res.peer.putClientProperty("JButton.buttonType", "segmentedCapsule")
    res.peer.putClientProperty("JButton.segmentPosition", "only")
    res
  }

  lazy val top = new Frame { me =>
    title     = "GUIFlitz - Debug"
    contents  = new BorderPanel {
      add(view.component, BorderPanel.Position.Center)
      add(new BoxPanel(Orientation.Horizontal) {
        contents += HGlue
        contents += postBut
        contents += HGlue
      }, BorderPanel.Position.South)
    }
    me.defaultCloseOperation = CloseOperation.Exit
    pack().centerOnScreen()
    open()
  }
}
