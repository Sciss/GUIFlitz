package de.sciss.guiflitz

import reflect.runtime.universe._
import reflect.runtime.{currentMirror => cm}

object VariantTest extends App {
  // object Foo {
    case object Bar extends Foo
  // }
  sealed trait Foo

  def getModule(tpe: Type): Any = {
    val classSymbol = tpe.typeSymbol.asClass
    val moduleSymbol = classSymbol.companionSymbol.asModule
    val moduleMirror = cm.reflectModule(moduleSymbol)
    moduleMirror.instance
  }

  val tpeOther = typeOf[Foo].typeSymbol.asClass.knownDirectSubclasses.last.asType.toType

  val res = getModule(tpeOther)
  println(res)
}