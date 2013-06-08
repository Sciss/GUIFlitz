package de.sciss.guiflitz

import reflect.runtime.{universe => ru, currentMirror => cm}
import ru.{TypeTag, Type, typeOf, Symbol, newTermName}
import collection.breakOut
import collection.immutable.{IndexedSeq => Vec}
import scala.util.control.NonFatal

object MetaTest extends App {
  sealed trait Foo
  case class Bar(name: String = "Schoko", age: Int = 33) extends Foo
  case object Baz extends Foo

  //  parse[Bar]()
  //  parse[Foo]()
  //  parse[Baz.type]()

  val info = apply[Foo]
  println(info)

  def parse[A: TypeTag]() {
    val tpe = typeOf[A]
    println(s"Sealed sub types of $tpe - isModuleClass? ${tpe.typeSymbol.isModuleClass}")
    val subs = sealedDescendants[A]
    println(subs)
    subs.foreach { syms =>
      syms.foreach { sym =>
        println(s"Symbol $sym - isModuleClass? ${sym.isModuleClass}")
        if (!sym.isModuleClass) {
          val (im, ts, mApply) = getApplyMethod(sym)
          val as    = mApply.paramss.flatten
          val args  = as.zipWithIndex.map { case (p, i) =>
            try {
              val name    = p.name
              val mDef    = ts.member(newTermName(s"apply$$default$$${i+1}")).asMethod
              val default = im.reflectMethod(mDef)()
              // println(s"$p - isParameter? ${p.isParameter}")
              val ptp     = p.typeSignature
              val atpe    = ptp match {
                case t if t <:< typeOf[Int]     => Shape.Int
                case t if t <:< typeOf[Double]  => Shape.Double
                case t if t <:< typeOf[String]  => Shape.String
                case t if t <:< typeOf[Boolean] => Shape.Boolean
                case t                          => Shape.Other(t)
              }

              Arg(name.decoded, atpe, default)

            } catch {
              case NonFatal(e) =>
                println(s"For type $sym, parameter $p at index $i has no default value")
                throw e
            }
          }

          println(s"Apply args: $args")
        }
      }
    }
  }

  private def getApplyMethod(sym: Symbol): (ru.InstanceMirror, Type, ru.MethodSymbol) = {
    val clazz  = sym.asClass
    val mod    = clazz.companionSymbol.asModule
    val im     = cm.reflect(cm.reflectModule(mod).instance)
    val ts     = im.symbol.typeSignature
    val mApply = ts.member(ru.newTermName("apply")).asMethod
    (im, ts, mApply)
  }

  // cf. stackoverflow no. 12078366
  def sealedDescendants[A: TypeTag]: Option[Set[Symbol]] = {
    val symbol = typeOf[A].typeSymbol.asClass
    if (symbol.isSealed)
      Some(symbol.knownDirectSubclasses)
    else None
  }

  object Shape {
    case object Int extends Shape {
      val tpe = typeOf[Int]
    }
    case object Double extends Shape {
      val tpe = typeOf[Double]
    }
    case object String extends Shape {
      val tpe = typeOf[String]
    }
    case object Boolean extends Shape {
      val tpe = typeOf[Boolean]
    }
    case class Vector (tpe: Type, elem: Shape)     extends Shape
    case class Product(tpe: Type, args: Vec[Arg])  extends Shape
    // case class Module (tpe: Type)                  extends Shape
    case class Variant(tpe: Type, sub: Vec[Shape]) extends Shape
    case class Other  (tpe: Type)                  extends Shape
  }
  sealed trait Shape {
    def tpe: Type
  }

  case class Arg(name: String, shape: Shape, default: Any)

  trait Cell[A] {
    def apply(): A
    def update(value: A): Unit
  }

  def apply[A: TypeTag]: Shape = {
    shapeFromType(typeOf[A])
  }

  private def shapeFromType(tpe: Type): Shape = {
    tpe match {
      case t if t <:< typeOf[Int]       => Shape.Int
      case t if t <:< typeOf[Double]    => Shape.Double
      case t if t <:< typeOf[String]    => Shape.String
      case t if t <:< typeOf[Boolean]   => Shape.Boolean
      case t if t <:< typeOf[Vec[Any]]  =>
        // cf. stackoverflow nr. 12842729
        val ta  = t.asInstanceOf[ru.TypeRefApi].args.head
        val sa  = shapeFromType(ta)
        Shape.Vector(tpe, sa)

      case _ =>
        val clazz = tpe.typeSymbol.asClass
        if (clazz.isCaseClass && !clazz.isModuleClass) {
          val (im, ts, mApply) = getApplyMethod(clazz)
          val as    = mApply.paramss.flatten
          val args: Vec[Arg] = as.zipWithIndex.map { case (p, i) =>
            try {
              val name    = p.name
              val mDef    = ts.member(newTermName(s"apply$$default$$${i+1}")).asMethod
              val default = im.reflectMethod(mDef)()
              // println(s"$p - isParameter? ${p.isParameter}")
              val ptp     = p.typeSignature
              val as      = shapeFromType(ptp)
              Arg(name.decoded, as, default)

            } catch {
              case NonFatal(e) =>
                println(s"For type $clazz, parameter $p at index $i has no default value")
                throw e
            }
          } (breakOut)
          Shape.Product(tpe, args)

        } else if (clazz.isSealed) {
          val sub: Vec[Shape] = clazz.knownDirectSubclasses.map(sym => shapeFromType(sym.asType.toType))(breakOut)
          Shape.Variant(tpe, sub)

        } else {
          Shape.Other(tpe)
        }
    }
  }
}