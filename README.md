# GUIFlitz

## statement

GUIFlitz provides automatic graphical user interface representations based on case classes for rapid prototyping in the Scala programming language. It is released under the [GNU Lesser General Public License](https://raw.github.com/Sciss/GUIFlitz/master/LICENSE) v2.1+ and comes with absolutely no warranties. To contact the author, send an email to `contact at sciss.de`.

## requirements / installation

This project currently compiles against Scala 2.11, 2.10 using sbt 0.13.

To use the library in your project:

    "de.sciss" %% "guiflitz" % v

The current version `v` is `"0.5.0"`

## example

An example application is provided by means of `sbt test:run`. The source class is `Demo`.

## supported types

- primitives: `Int`, `Double`, `Boolean`, `String`, `Unit`
- sealed traits with known direct subclasses, given that each subclass is supported
- case classes, given that each constructor parameter's type is supported
- singleton objects
- `immutable.IndexedSeq[A]`, given that type `A` is supported
- `Option[A]`, given that type `A` is supported

Support for custom types can be added via the `addViewFactory` method of the `AutoView`'s configuration. For details, see the `CustomViewApp` example in the `test` directory.

## limitations, known issues

- does not work yet with type parameters, e.g. `case class Foo(opt: Either[Int, String])` fails because of `Either` taking type parameters.
- titled border doesn't respect `small` config (should adjust font)
