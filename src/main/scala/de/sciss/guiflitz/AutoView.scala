/*
 *  AutoView.scala
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

import impl.{AutoViewImpl => Impl}
import scala.swing.{Component, Panel}
import reflect.runtime.{universe => ru}
import ru.TypeTag
import language.implicitConversions

object AutoView {
  /** This field is for debugging purposes only. */
  var showLog = false

  sealed trait ConfigLike {
    /** Whether to use small sized components (`true`) or not. Default is `false`. */
    def small: Boolean
    /** Whether to wrap the component in a scroll pane (`true`) or not. Default is `true`. */
    def scroll: Boolean
  }
  object Config {
    def apply(): ConfigBuilder = new ConfigBuilder
    implicit def build(b: ConfigBuilder): Config = b.build
  }
  case class Config private[AutoView](small: Boolean, scroll: Boolean) extends ConfigLike
  final class ConfigBuilder private[AutoView]() extends ConfigLike {
    var small   = false
    var scroll  = true

    def build: Config = Config(small = small, scroll = scroll)

    def read(c: Config): Unit = {
      small   = c.small
      scroll  = c.scroll
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