package de.sciss.guiflitz

import scala.swing.{Button, BorderPanel, Frame, SimpleSwingApplication}
import de.sciss.swingplus.CloseOperation
import de.sciss.swingplus.Implicits._
import javax.swing.UIManager

object Demo extends SimpleSwingApplication {
  val useNimbus = false

  val nimbusOption = if (!useNimbus) None else UIManager.getInstalledLookAndFeels.collectFirst {
    case info if info.getName == "Nimbus" => info.getClassName
  }
  nimbusOption.foreach(UIManager.setLookAndFeel _)

  lazy val top = new Frame { me =>
    title     = "GUIFlitz"
    val view  = AutoView(Person.Example)
    contents  = new BorderPanel {
      add(view.component, BorderPanel.Position.Center)
      add(Button("Post") {
        println(view.cell())
      }, BorderPanel.Position.South)
    }
    me.defaultCloseOperation = CloseOperation.Exit
    pack().centerOnScreen()
    open()
  }
}