package de.sciss.guiflitz

import scala.swing.{FlowPanel, TextField, Label, Frame, SimpleSwingApplication}
import javax.swing.SpinnerNumberModel
import de.sciss.swingplus.{CloseOperation, Spinner}
import de.sciss.swingplus.Implicits._
import Springs._

object SpringPanelTest extends SimpleSwingApplication {
  lazy val top = new Frame {
    title = "SpringPanel"
    new FlowPanel()
    contents = new SpringPanel {
      val lbName  = new Label("Name:")
      val lbAge   = new Label( "Age:")
      val ggName  = new TextField(12)
      val mAge    = new SpinnerNumberModel(35, 0, 999, 1)
      val ggAge   = new Spinner(mAge)

      contents ++= Seq(lbName, lbAge, ggName, ggAge)

      // cons(lbName).x = 4
      // cons(lbName).y = 4

      hseq  (lbName, ggName)
      hseq  (lbAge , ggAge )
      vseq  (lbName, lbAge )
      valign(lbName, ggName)
      valign(lbName, ggAge )
    }

    pack().centerOnScreen()
    this.defaultCloseOperation = CloseOperation.Exit
    open()
  }
}