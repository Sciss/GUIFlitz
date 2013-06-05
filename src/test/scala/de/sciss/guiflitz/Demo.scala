package de.sciss.guiflitz

import scala.swing.{Frame, SimpleSwingApplication}
import de.sciss.swingplus.CloseOperation
import de.sciss.swingplus.Implicits._

object Demo extends SimpleSwingApplication {
  lazy val top = new Frame { me =>
    contents = AutoView(Person.Example)
    me.defaultCloseOperation = CloseOperation.Exit
    pack().centerOnScreen()
    open()
  }
}