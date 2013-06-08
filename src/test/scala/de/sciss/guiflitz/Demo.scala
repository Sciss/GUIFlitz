package de.sciss.guiflitz

import scala.swing.{Button, BorderPanel, Frame, SimpleSwingApplication}
import de.sciss.swingplus.CloseOperation
import de.sciss.swingplus.Implicits._

object Demo extends SimpleSwingApplication {
  lazy val top = new Frame { me =>
    title     = "GUIFlitz"
    val view  = AutoView(Person.Example)
    contents  = new BorderPanel {
      add(Button("Post") {
        println(view.cell())
      }, BorderPanel.Position.North)
      add(view.component, BorderPanel.Position.Center)
    }
    me.defaultCloseOperation = CloseOperation.Exit
    pack().centerOnScreen()
    open()
  }
}