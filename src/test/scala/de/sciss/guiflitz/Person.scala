package de.sciss.guiflitz

// object Gender {
  case object Male   extends Gender
  case object Female extends Gender
  case object Other  extends Gender
// }
sealed trait Gender

object Person {
  val Example = Person(name = "Nam June", age = 80, flux = true, num = 3.1415, gender = /* Gender.*/ Male)
}
case class Person(name: String, age: Int, flux: Boolean, num: Double, gender: Gender)

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