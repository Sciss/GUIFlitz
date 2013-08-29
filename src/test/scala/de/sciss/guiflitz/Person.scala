package de.sciss.guiflitz

import collection.immutable.{IndexedSeq => Vec}

object Gender {
  case object Male   extends Gender
  case object Female extends Gender
  case class  Other(detail: String) extends Gender
}
sealed trait Gender

sealed trait MaybePerson
case object NoPerson extends MaybePerson

object Person {
  val Example = Person(name = "Nam June", age = 80, flux = true, num = 3.1415, gender = Gender.Other("schoko"),
                       spouse =
                          Person("Shigeko", age = 75, flux = true, num = 1.234, gender = Gender.Female,
                            spouse = NoPerson, tags = Vec("Video", "Performance")),
                       tags = Vec("Video")
                      )
}
case class Person(name: String, age: Int, flux: Boolean, num: Double, gender: Gender, spouse: MaybePerson,
                  tags: Vec[String])
  extends MaybePerson

object Test {
  import reflect.runtime.universe._
  import reflect.runtime.{currentMirror => cm}

  def find(tps: List[Type], obj: Any): Option[Type] = {
    val clazz = obj.getClass
    val c2    = cm.classSymbol(clazz).toType
    tps find { tp =>
      // val c   = tp.typeSymbol.asClass
      // val m   = cm.reflectClass(c)
      c2 <:< tp
    }
  }
}