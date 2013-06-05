package de.sciss.guiflitz

import reflect.runtime.{universe => ru}
import util.control.NonFatal
import ru.{TypeTag, typeOf}

object Meta {
  implicit def apply[A: TypeTag]: Meta[A] = if (isSingleton[A]) MetaCaseObject[A]() else MetaCaseClass[A]()

  def isSingleton[A: TypeTag]: Boolean = typeOf[A] <:< typeOf[Singleton]
}
sealed trait Meta[+A] {
  // implicit protected def tt: TypeTag[A]

  protected def tpe: ru.Type

  lazy val name: String = {
    // val t = typeOf[A]
    val s = tpe.toString
    val i = s.lastIndexOf('.') + 1
    s.substring(i)
  }

  def instance(): A
  def instance(args: List[Any]): A
  def defaults: List[Any]

  override def toString = name
}

object MetaCaseClass {
  import reflect.runtime.{currentMirror => cm}

  private def collectDefaults[A](implicit tt: TypeTag[A]): List[Any] = {
    val (im, ts, mApply) = getApplyMethod[A]
    val syms   = mApply.paramss.flatten
    val args   = syms.zipWithIndex.map { case (p, i) =>
      try {
        val mDef = ts.member(ru.newTermName(s"apply$$default$$${i+1}")).asMethod
        im.reflectMethod(mDef)()
      } catch {
        case NonFatal(e) =>
          println(s"For type ${typeOf[A]}, parameter $p at index $i has no default value")
          throw e
      }
    }
    args
  }

  private def getApplyMethod[A: TypeTag]: (ru.InstanceMirror, ru.Type, ru.MethodSymbol) = {
    val clazz  = typeOf[A].typeSymbol.asClass
    val mod    = clazz.companionSymbol.asModule
    val im     = cm.reflect(cm.reflectModule(mod).instance)
    val ts     = im.symbol.typeSignature
    val mApply = ts.member(ru.newTermName("apply")).asMethod
    (im, ts, mApply)
  }

  def apply[A: TypeTag](): MetaCaseClass[A] = {
    val defaults  = collectDefaults[A]
    new MetaCaseClass[A](defaults)
  }
}
case class MetaCaseClass[A: TypeTag](defaults: List[Any]) extends Meta[A] {
  def instance(): A = instance(defaults)

  protected def tpe: ru.Type = typeOf[A]

  def instance(args: List[Any]): A = {
    val (im, _, mApply) = MetaCaseClass.getApplyMethod[A]
    im.reflectMethod(mApply)(args: _*).asInstanceOf[A]
  }
}
//object MetaCaseObject {
//  def apply[A: TypeTag]: MetaCaseObject[A] = new MetaCaseObject[A]
//}
case class MetaCaseObject[A: TypeTag]() extends Meta[A] {
  protected def tpe: ru.Type = typeOf[A]

  def defaults = Nil

  def instance(): A = {
    import reflect.runtime.{currentMirror => cm}
    cm.runtimeClass(typeOf[A].typeSymbol.asClass).asInstanceOf[A]
  }

  def instance(args: List[Any]): A = {
    require(args.isEmpty)
    instance()
  }
}

trait HasMeta[A] {
  def meta: Meta[A]
}