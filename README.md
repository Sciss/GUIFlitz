# GUIFlitz

## statement

GUIFlitz provides automatic graphical user interface representations based on case classes for rapid prototyping in the Scala programming language. It is released under the [GNU General Public License](https://raw.github.com/Sciss/GUIFlitz/master/LICENSE) v2+ and comes with absolutely no warranties. To contact the author, send an email to `contact at sciss.de`.

## note

This project is in early stage and not yet fully functional.

## requirements / installation

This project currently compiles against Scala 2.10 using sbt 0.12.

To use the library in your project:

    "de.sciss" %% "guiflitz" % v

The current version `v` is `"0.0.+"`

## example

An example application is provided by means of `sbt test:run`. The source class is `Demo`.

## limitations

- currently supported types: `Int`, `Double`, `Boolean`, `String`, case classes and singleton objects
- does not work yet with type parameters, e.g. `case class Foo(opt: Option[Int])` fails because of `Option` taking a type parameter.
- `IndexedSeq` view not yet implemented
- titled border doesn't respect `small` config (should adjust font)