package de.sciss.guiflitz

import scala.swing.{Frame, MainFrame, SimpleSwingApplication}

object CustomViewApp extends SimpleSwingApplication {
  case class Percent(n: Int)

  case class Task(name: String, progress: Percent)

  lazy val top: Frame = new MainFrame {
    val c = AutoView.Config()
    ??? // c.putViewFactory((p: Percent) => )
  }
}
