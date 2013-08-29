package de.sciss.guiflitz

import scala.swing.{Swing, BoxPanel, Orientation, Button, BorderPanel, Frame, SimpleSwingApplication}
import Swing._
import de.sciss.swingplus.CloseOperation
import de.sciss.swingplus.Implicits._
import javax.swing.UIManager

object Demo extends SimpleSwingApplication {
  val useNimbus = false

  val nimbusOption = if (!useNimbus) None else UIManager.getInstalledLookAndFeels.collectFirst {
    case info if info.getName == "Nimbus" => info.getClassName
  }
  nimbusOption.foreach(UIManager.setLookAndFeel)

  lazy val view = {
    val c = AutoView.Config()
    c.small = true
    AutoView(Person.Example, c)
  }

  lazy val postBut = {
    val res = Button("Post") { println(view.cell()) }
    res.peer.putClientProperty("JButton.buttonType", "segmentedCapsule")
    res.peer.putClientProperty("JButton.segmentPosition", "only")
    res
  }

  lazy val top = new Frame { me =>
    title     = "GUIFlitz"
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