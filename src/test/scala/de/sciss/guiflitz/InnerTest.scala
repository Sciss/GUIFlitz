package de.sciss.guiflitz

import reflect.runtime.{currentMirror => cm, universe => ru}

case class Outer()

trait Foo {
  type Inner
}

object Bar extends Foo {
  case class Inner()
}

trait BarLike extends Foo {
  case class Inner()
}

object Baz extends BarLike

/** Note: this fails for `Baz`. */
object InnerTest extends App {
  def getApplyMethod[A: ru.TypeTag]: ru.MethodSymbol = {
    val sym     = ru.typeOf[A].typeSymbol
    val clazz   = sym.asClass
    val mod     = clazz.companionSymbol.asModule
    if (!mod.isStatic) println(s"Oh oh... $mod")
    val im      = cm.reflect(cm.reflectModule(mod).instance)
    val ts      = im.symbol.typeSignature
    val mApply  = ts.member(ru.newTermName("apply")).asMethod
    mApply
  }

  getApplyMethod[Outer]
  getApplyMethod[Bar.Inner]
  getApplyMethod[Baz.Inner]

//  val toc   = ru.typeOf[Outer].typeSymbol.asClass
//  val tom   = toc.companionSymbol.asModule
//  val tomm  = cm.reflectModule(tom)
//  val tommi = cm.reflect(tomm.instance)
//
//  val tic   = ru.typeOf[Bar.Inner].typeSymbol.asClass
//  val tim   = tic.companionSymbol.asModule
//  val timm  = cm.reflectModule(tim)
//  val timmi = cm.reflect(timm.instance)
//
//  println("Ok.")
}