/*
 *  AutoView.scala
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

import de.sciss.guiflitz.impl.{AutoViewImpl => Impl}

import scala.language.implicitConversions
import scala.reflect.runtime.{universe => ru}
import ru.{Type, TypeTag}
import scala.swing.Component

object AutoView {
  /** This field is for debugging purposes only. */
  var showLog = false

  sealed trait ConfigLike {
    /** Whether to use small sized components (`true`) or not. Default is `false`. */
    def small: Boolean
    /** Whether to wrap the component in a scroll pane (`true`) or not. Default is `true`. */
    def scroll: Boolean

    private[guiflitz] def factoryMap: Map[Type, ViewFactory[_]]

    def getViewFactory[A](implicit typeTag: TypeTag[A]): Option[ViewFactory[A]] =
      factoryMap.get(typeTag.tpe).asInstanceOf[Option[ViewFactory[A]]]
  }
  object Config {
    def apply(): ConfigBuilder = new ConfigBuilder
    implicit def build(b: ConfigBuilder): Config = b.build
  }
  case class Config private[AutoView](small: Boolean, scroll: Boolean,
                                      private[guiflitz] val factoryMap: Map[Type, ViewFactory[_]])
    extends ConfigLike {
  }
  final class ConfigBuilder private[AutoView]() extends ConfigLike {
    var small   = false
    var scroll  = true

    private[guiflitz] var factoryMap = Map.empty[Type, ViewFactory[_]]

    def addViewFactory[A](factory: ViewFactory[A])(implicit typeTag: TypeTag[A]): Unit =
      factoryMap += typeTag.tpe -> factory

    def build: Config = Config(small = small, scroll = scroll, factoryMap = factoryMap)

    def read(c: Config): Unit = {
      small       = c.small
      scroll      = c.scroll
      factoryMap  = c.factoryMap
    }
  }

  /** Creates a new automatic view for a given type and initial value of that type.
    *
    * @param init     the initial value
    * @param config   the view configuration
    * @tparam A       the type of value to display
    * @return         the view proxy with access to the current value and Swing component
    */
  def apply[A: TypeTag](init: A, config: Config = Config()): AutoView[A] = Impl(init, config)
}
trait AutoView[A] {
  /** The configuration which was used to create the view. */
  def config: AutoView.Config

  /** The current value of the view. */
  def value: A

  /** A mutable cell holding the viewed value.
    * __Important:__ This is not thread safe at the moment. Update the cell only on the Swing event dispatch thread!
    *
    * A call to `cell()` is equivalent to calling `value`
    */
  def cell: Cell[A]

  /** The swing component automatically populated. */
  def component: Component
}