# pekko-http-thrift

[![Continuous Integration](https://github.com/RustedBones/pekko-http-thrift/actions/workflows/ci.yml/badge.svg)](https://github.com/RustedBones/pekko-http-thrift/actions/workflows/ci.yml)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/fr.davit/pekko-http-thrift_2.12/badge.svg)](https://maven-badges.herokuapp.com/maven-central/fr.davit/pekko-http-thrift_3)
[![Software License](https://img.shields.io/badge/license-Apache%202-brightgreen.svg?style=flat)](LICENSE)
[![Scala Steward badge](https://img.shields.io/badge/Scala_Steward-helping-blue.svg?style=flat&logo=data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAA4AAAAQCAMAAAARSr4IAAAAVFBMVEUAAACHjojlOy5NWlrKzcYRKjGFjIbp293YycuLa3pYY2LSqql4f3pCUFTgSjNodYRmcXUsPD/NTTbjRS+2jomhgnzNc223cGvZS0HaSD0XLjbaSjElhIr+AAAAAXRSTlMAQObYZgAAAHlJREFUCNdNyosOwyAIhWHAQS1Vt7a77/3fcxxdmv0xwmckutAR1nkm4ggbyEcg/wWmlGLDAA3oL50xi6fk5ffZ3E2E3QfZDCcCN2YtbEWZt+Drc6u6rlqv7Uk0LdKqqr5rk2UCRXOk0vmQKGfc94nOJyQjouF9H/wCc9gECEYfONoAAAAASUVORK5CYII=)](https://scala-steward.org)

pekko-http thrift and json marshalling/unmarshalling for Thrift structs

## Versions

| Version | Release date | pekko Http version | Thrift version | Scala versions |
|---------|--------------|--------------------|----------------|----------------|
| `1.1.0` | 2024-04-09   | `1.0.1`            | `0.20.0`       | `3.3`, `2.13`  |
| `1.0.0` | 2023-11-02   | `1.0.0`            | `0.19.0`       | `3.3`, `2.13`  |

The complete list can be found in the [CHANGELOG](CHANGELOG.md) file.

## Getting pekko-http-thrift

Libraries are published to Maven Central. Add to your `build.sbt`:

```sbt
libraryDependencies += "fr.davit" %% "pekko-http-thrift" % <version> // thrift support
```

## Quick start

For the examples, we are using the following thrift domain model

```thrift
struct Item {
  1: string name
  2: i64 id
}

struct Order {
  1: list<Item> items
}
```

Marshalling/Unmarshalling of the generated classes depends on the `Accept`/`Content-Type` header sent by the client:

- `Content-Type: application/json`: json
- `Content-Type: application/vnd.apache.thrift.json`: json
- `Content-Type: application/vnd.apache.thrift.binary`: binary
- `Content-Type: application/vnd.apache.thrift.compact`: compact

-No `Accept` header or matching several (eg `Accept: application/*`) will take the 1st matching type from the above
list.

### Thrift

The implicit marshallers and unmarshallers for your generated thrift classes are defined in
`ThriftSupport`. Specific (un)marshallers can be imported from `ThriftBinarySupport`, `ThriftCompactSupport`
and `ThriftJsonSupport`.
You simply need to have them in scope.

```scala
import org.apache.pekko.http.scaladsl.server.Directives
import fr.davit.pekko.http.scaladsl.marshallers.thrift.ThriftSupport


class MyThriftService extends Directives with ThriftSupport {

  // format: OFF
  val route =
    get {
      pathSingleSlash {
        complete(Item("thing", 42))
      }
    } ~
      post {
        entity(as[Order]) { order =>
          val itemsCount = order.items.size
          val itemNames = order.items.map(_.name).mkString(", ")
          complete(s"Ordered $itemsCount items: $itemNames")
        }
      }
  // format: ON
}
```

## Limitation

Entity streaming (http chunked transfer) is at the moment not supported by the library.