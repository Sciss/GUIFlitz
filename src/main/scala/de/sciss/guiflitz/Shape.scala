/*
 *  Shape.scala
 *  (GUIFlitz)
 *
 *  Copyright (c) 2013-2015 Hanns Holger Rutz. All rights reserved.
 *
 *  This software is published under the GNU Lesser General Public License v2.1+
 *
 *  For further information, please contact Hanns Holger Rutz at
 *  contact@sciss.de
 */

package de.sciss.guiflitz

import reflect.runtime.{universe => ru, currentMirror => cm}
import ru.{TypeTag, Type, typeOf, Symbol, newTermName}
import collection.breakOut
import collection.immutable.{IndexedSeq => Vec}

object Shape {
  case object Int extends Shape {
    val tpe = typeOf[Int]
    def instantiate() = 0
  }
  case object Double extends Shape {
    val tpe = typeOf[Double]
    def instantiate() = 0.0
  }
  case object String extends Shape {
    val tpe = typeOf[String]
    def instantiate() = "string"
  }
  case object Boolean extends Shape {
    val tpe = typeOf[Boolean]
    def instantiate() = false
  }
  case object Unit extends Shape {
    val tpe = typeOf[Unit]
    def instantiate() = ()
  }
  case class Vector(tpe: Type, elem: Shape) extends Shape {
    def instantiate() = Vec.empty
  }
  case class Option(tpe: Type, elem: Type) extends Shape {
    def instantiate() = None
  }
  case class Product(tpe: Type, args: Vec[Arg])  extends Shape {
    def instantiate(): Any = {
      val clazz           = tpe.typeSymbol.asClass
      val (im, _, mApply) = getApplyMethod(clazz)
      val vArgs = args.map { arg => arg.default.getOrElse(arg.shape.instantiate()) }
      im.reflectMethod(mApply)(vArgs: _*)
    }
  }
  // case class Module (tpe: Type)                  extends Shape

  /** A variant is a shape from a sealed trait resolving to a fixed number of implementing case classes.
    *
    * @param tpe  the trait type
    * @param sub  the sub types. This must be `Vec[Type]` and cannot be `Vec[Shape]` because it is the circuit
    *             breaker point for self referential structures.
    */
  case class Variant(tpe: Type, sub: Vec[Type]) extends Shape {
    def find(obj: Any): scala.Option[Type] = {
      val objType = cm.classSymbol(obj.getClass).toType
      require  (objType <:< this.tpe)
      sub find (objType <:< _       )
    }

    def instantiate() = Shape.fromType(sub.head).instantiate()
  }
  case class Other(tpe: Type) extends Shape {
    def instantiate(): Any = {
      val clazz = tpe.typeSymbol.asClass
      if (clazz.isModuleClass) {
        // cf. https://gist.github.com/xeno-by/4985929
        val mc  = clazz.companionSymbol
        val mod = mc.asModule
        val mm  = cm.reflectModule(mod)
        mm.instance
      } else {
        throw new IllegalStateException(s"Cannot instantiate $tpe")
      }
    }
  }

  def apply[A: TypeTag]: Shape = {
    fromType(typeOf[A])
  }

  private[guiflitz] def getApplyMethod(sym: Symbol): (ru.InstanceMirror, Type, ru.MethodSymbol) = {
    val clazz  = sym.asClass
    val mod    = clazz.companionSymbol.asModule
    val im     = cm.reflect(cm.reflectModule(mod).instance)
    val ts     = im.symbol.typeSignature
    val mApply = ts.member(ru.newTermName("apply")).asMethod
    (im, ts, mApply)
  }

  // cf. stack-overflow nr. 12842729
  @inline private def firstTypeParameter(t: Type): Type =t.asInstanceOf[ru.TypeRefApi].args.head

  def fromType(tpe: Type): Shape = {
    tpe match {
      case t if t <:< typeOf[Int    ] => Shape.Int
      case t if t <:< typeOf[Double ] => Shape.Double
      case t if t <:< typeOf[String ] => Shape.String
      case t if t <:< typeOf[Boolean] => Shape.Boolean
      case t if t <:< typeOf[Unit   ] => Shape.Unit
      //      case t if t <:< typeOf[Option[Any]] =>
      //        val ta  = t.asInstanceOf[ru.TypeRefApi].args.head
      //        val sa  = fromType(ta)
      //        Shape.Option(tpe, sa)

      case t if t <:< typeOf[Vec[Any]] =>
        val ta  = firstTypeParameter(t)
        val sa  = fromType(ta)
        Shape.Vector(tpe, sa)

      case t if t <:< typeOf[scala.Option[Any]] =>
        val ta  = firstTypeParameter(t)
        // val sa  = fromType(ta)
        Shape.Option(tpe, ta) // sa)

      case _ => // try to resolve as a Product (case class) or singleton

        val tSym  = tpe.typeSymbol
        // println(s"Type symbol $tSym; isClass? ${tSym.isClass}")
        val clazz = tSym.asClass
        if (clazz.isCaseClass && !clazz.isModuleClass) {
          val (im, ts, mApply) = getApplyMethod(clazz)
          val as    = mApply.paramss.flatten
          val args: Vec[Arg] = as.zipWithIndex.map { case (p, i) =>
            // try {
              val name    = p.name
              val mDef_?  = ts.member(newTermName(s"apply$$default$$${i+1}"))
              val default = if (mDef_?.isMethod) {
                val mDef    = mDef_?.asMethod
                Some(im.reflectMethod(mDef)())
              } else {
                None
              }
              val ptp     = p.typeSignature
              // val ptp     = p.typeSignatureIn(tpe)
              // println(s"$p - isParameter? ${p.isParameter}; signature $ptp")
              val as      = fromType(ptp)
              Arg(name.decoded, as, default)

            //            } catch {
            //              case NonFatal(e) =>
            //                println(s"For type $clazz, parameter $p at index $i has no default value")
            //                throw e
            //            }
          } (breakOut)
          Shape.Product(tpe, args)

        } else if (clazz.isSealed) {
          val symbols = clazz.knownDirectSubclasses.toIndexedSeq.sortBy(_.name.toString)
          clazz.companionSymbol.typeSignature // !!! work around stack-overflow no. 17012294
          // val sub: Vec[Shape] = symbols.map(sym => fromType(sym.asType.toType))
          val sub: Vec[Type] = symbols.map(_.asType.toType)
          Shape.Variant(tpe, sub)

        } else {
          Shape.Other(tpe)
        }
    }
  }

  case class Arg(name: String, shape: Shape, default: scala.Option[Any])
}
sealed trait Shape {
  def tpe: Type
  /** Tries to instantiate this shape. Throws a runtime exception if not possible (e.g. default args missing) */
  def instantiate(): Any
}