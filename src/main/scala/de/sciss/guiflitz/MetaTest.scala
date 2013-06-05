package de.sciss.guiflitz

import reflect.runtime.{universe => ru, currentMirror => cm}
import ru.{TypeTag, Type, typeOf, Symbol, newTermName}
import collection.immutable.{IndexedSeq => Vec}
import scala.util.control.NonFatal

object MetaTest extends App {
  sealed trait Foo
  case class Bar(name: String = "Schoko", age: Int = 33) extends Foo
  case object Baz extends Foo

  parse[Bar]()
  parse[Foo]()
  parse[Baz.type]()

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
                case t if t <:< typeOf[Int]     => ArgType.Int
                case t if t <:< typeOf[Double]  => ArgType.Double
                case t if t <:< typeOf[String]  => ArgType.String
                case t if t <:< typeOf[Boolean] => ArgType.Boolean
                case t                          => ArgType.Unknown(t)
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

  object ArgType {
    case object Int                     extends ArgType
    case object Double                  extends ArgType
    case object String                  extends ArgType
    case object Boolean                 extends ArgType
    case class Vector(elem: ArgType)    extends ArgType
    case class Variant(tps: Vec[Type])  extends ArgType
    case class Unknown(tp: Type)        extends ArgType
  }
  sealed trait ArgType

  case class Arg(name: String, tp: ArgType, default: Any)
}