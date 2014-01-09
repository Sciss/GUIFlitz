//package de.sciss.guiflitz
//
//import scala.swing.{Alignment, FlowPanel, TextField, Label, Frame, SimpleSwingApplication}
//import javax.swing.{UIManager, Spring, SpinnerNumberModel}
//import de.sciss.swingplus.{CloseOperation, Spinner}
//import de.sciss.swingplus.Implicits._
//import Springs._
//
//object SpringPanelTest extends SimpleSwingApplication {
//  // UIManager.setLookAndFeel("com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel")
//
//  lazy val top = new Frame {
//    title = "SpringPanel"
//    new FlowPanel()
//    contents = new SpringPanel {
//      val mAge    = new SpinnerNumberModel(35, 0, 999, 1)
//      val lbName  = new Label("Name:", null, Alignment.Trailing)
//      val lbAge   = new Label( "Age:", null, Alignment.Trailing)
//      val ggName  = new TextField(12) // new Spinner(mAge) // new TextField(12)
//      val ggAge   = new Spinner(mAge)
//
//      contents ++= Seq(lbName, lbAge, ggName, ggAge)
//
//      cons(ggName).y = 4
//      val lbw = cons(lbName).width max cons(lbAge).width
//      val lbr =  lbw + 4
//      cons(lbName).right  = lbr
//      cons(lbAge ).right  = lbr
//      val ggl = lbr + 4
//      cons(ggName).left = ggl
//      cons(ggAge ).left = ggl
//      // linkHeight(lbName, ggName, lbAge, ggAge)
//      // val b1 = cons(lbName).baseline max cons(ggName).baseline
//      cons(lbName).y        = 4 // cons(ggName).y
//      cons(lbName).baseline = cons(ggName).baseline
//      // cons(ggName).baseline = b1
//      val ggb1 = cons(lbName).bottom max cons(ggName).bottom
//      val ggt2 = ggb1 + 4
//      // cons(lbAge).top = ggt2
//      cons(ggAge).top = ggt2
//      // val b2 = cons(lbAge).baseline max cons(ggAge).baseline
//      cons(lbAge).y = cons(ggAge).y
//      cons(lbAge).baseline = cons(ggAge).baseline
//      // cons(lbAge).baseline = b2
//      // cons(ggAge).baseline = b2
//      cons(this).right  = (cons(ggName).right max cons(ggAge).right)  + Spring.constant(4, 4, Int.MaxValue)
//      cons(this).bottom = (cons(lbAge).bottom max cons(ggAge).bottom) + Spring.constant(4, 4, Int.MaxValue)
//    }
//
//    pack().centerOnScreen()
//    this.defaultCloseOperation = CloseOperation.Exit
//    open()
//  }
//}