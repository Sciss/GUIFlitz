/*
 *  Shape.scala
 *  (GUIFlitz)
 *
 *  Copyright (c) 2013 Hanns Holger Rutz. All rights reserved.
 *
 *  This software is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License
 *  as published by the Free Software Foundation; either
 *  version 2, june 1991 of the License, or (at your option) any later version.
 *
 *  This software is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public
 *  License (gpl.txt) along with this software; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
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
  case class Vector (tpe: Type, elem: Shape)     extends Shape {
    def instantiate() = Vec.empty
  }
  case class Product(tpe: Type, args: Vec[Arg])  extends Shape {
    def instantiate(): Any = {
      val clazz           = tpe.typeSymbol.asClass
      val (im, _, mApply) = getApplyMethod(clazz)
      val vargs = args.map { arg => arg.default.getOrElse(arg.shape.instantiate()) }
      im.reflectMethod(mApply)(vargs: _*)
    }
  }
  // case class Module (tpe: Type)                  extends Shape

  case class Variant(tpe: Type, sub: Vec[Shape]) extends Shape {
    def find(obj: Any): Option[Shape] = {
      val objType = cm.classSymbol(obj.getClass).toType
      require  (objType <:< this.tpe)
      sub find (objType <:< _   .tpe)
    }

    def instantiate() = sub.head.instantiate()
  }
  case class Other  (tpe: Type)                  extends Shape {
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
    shapeFromType(typeOf[A])
  }

  private[guiflitz] def getApplyMethod(sym: Symbol): (ru.InstanceMirror, Type, ru.MethodSymbol) = {
    val clazz  = sym.asClass
    val mod    = clazz.companionSymbol.asModule
    val im     = cm.reflect(cm.reflectModule(mod).instance)
    val ts     = im.symbol.typeSignature
    val mApply = ts.member(ru.newTermName("apply")).asMethod
    (im, ts, mApply)
  }

  private def shapeFromType(tpe: Type): Shape = {
    tpe match {
      case t if t <:< typeOf[Int]         => Shape.Int
      case t if t <:< typeOf[Double]      => Shape.Double
      case t if t <:< typeOf[String]      => Shape.String
      case t if t <:< typeOf[Boolean]     => Shape.Boolean
      //      case t if t <:< typeOf[Option[Any]] =>
      //        val ta  = t.asInstanceOf[ru.TypeRefApi].args.head
      //        val sa  = shapeFromType(ta)
      //        Shape.Option(tpe, sa)

      case t if t <:< typeOf[Vec[Any]]    =>
        // cf. stackoverflow nr. 12842729
        val ta  = t.asInstanceOf[ru.TypeRefApi].args.head
        val sa  = shapeFromType(ta)
        Shape.Vector(tpe, sa)

      case _ =>
        val tsym  = tpe.typeSymbol
        println(s"Type symbol $tsym; isClass? ${tsym.isClass}")
        val clazz = tsym.asClass
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
              val as      = shapeFromType(ptp)
              Arg(name.decoded, as, default)

            //            } catch {
            //              case NonFatal(e) =>
            //                println(s"For type $clazz, parameter $p at index $i has no default value")
            //                throw e
            //            }
          } (breakOut)
          Shape.Product(tpe, args)

        } else if (clazz.isSealed) {
          val syms            = clazz.knownDirectSubclasses.toIndexedSeq.sortBy(_.name.toString)
          clazz.companionSymbol.typeSignature // !!! work around stackoverflow no. 17012294
          val sub: Vec[Shape] = syms.map(sym => shapeFromType(sym.asType.toType))
          Shape.Variant(tpe, sub)

        } else {
          Shape.Other(tpe)
        }
    }
  }

  case class Arg(name: String, shape: Shape, default: Option[Any])
}
sealed trait Shape {
  def tpe: Type
  /** Tries to instantiate this shape. Throws a runtime exception if not possible (e.g. default args missing) */
  def instantiate(): Any
}