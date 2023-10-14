/*
 * Copyright 2019 Michel Davit
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fr.davit.pekko.http.scaladsl.marshallers.thrift

import org.apache.pekko.http.scaladsl.marshalling.{Marshaller, ToEntityMarshaller}
import org.apache.pekko.http.scaladsl.model.*
import org.apache.pekko.http.scaladsl.unmarshalling.{FromEntityUnmarshaller, Unmarshaller}
import org.apache.pekko.util.ByteString
import org.apache.thrift.TBase
import org.apache.thrift.protocol.{TBinaryProtocol, TCompactProtocol, TJSONProtocol, TProtocolFactory}
import org.apache.thrift.transport.{TByteBuffer, TIOStreamTransport}

import scala.reflect.ClassTag
import scala.util.chaining.*

object ThriftProtocol {
  val `application/vnd.apache.thrift.binary`: ContentType  =
    MediaType.applicationBinary("vnd.apache.thrift.binary", MediaType.NotCompressible)
  val `application/vnd.apache.thrift.compact`: ContentType =
    MediaType.applicationBinary("vnd.apache.thrift.compact", MediaType.NotCompressible)
  val `application/vnd.apache.thrift.json`: ContentType    =
    MediaType.applicationWithFixedCharset("vnd.apache.thrift.json", HttpCharsets.`UTF-8`)

  private[thrift] implicit class ThriftProtocolOps(val factory: TProtocolFactory) extends AnyVal {
    private def newInstance[T <: TBase[?, ?]: ClassTag](): T   =
      implicitly[ClassTag[T]].runtimeClass.getDeclaredConstructor().newInstance().asInstanceOf[T]
    def serialize[T <: TBase[?, ?]](thrift: T): ByteString     = {
      val builder = ByteString.newBuilder
      thrift.write(factory.getProtocol(new TIOStreamTransport(builder.asOutputStream)))
      builder.result()
    }
    def read[T <: TBase[?, ?]: ClassTag](bytes: ByteString): T =
      newInstance[T]().tap(_.read(factory.getProtocol(new TByteBuffer(bytes.asByteBuffer))))
  }
}

trait ThriftAbstractSupport {
  implicit def thriftUnmarshaller[T <: TBase[?, ?]: ClassTag]: FromEntityUnmarshaller[T]
  implicit def thriftMarshaller[T <: TBase[?, ?]]: ToEntityMarshaller[T]
}

//----------------------------------------------------------------------------------------------------------------------
// Binary
//----------------------------------------------------------------------------------------------------------------------
trait ThriftBinarySupport extends ThriftAbstractSupport {
  import ThriftProtocol.*
  private val protocolFactory: TProtocolFactory                                                   = new TBinaryProtocol.Factory()
  implicit override def thriftUnmarshaller[T <: TBase[?, ?]: ClassTag]: FromEntityUnmarshaller[T] =
    Unmarshaller.byteStringUnmarshaller
      .forContentTypes(`application/vnd.apache.thrift.binary`)
      .map(protocolFactory.read[T])

  implicit override def thriftMarshaller[T <: TBase[?, ?]]: ToEntityMarshaller[T] =
    Marshaller
      .byteStringMarshaller(`application/vnd.apache.thrift.binary`)
      .compose(protocolFactory.serialize)
}

object ThriftBinarySupport extends ThriftBinarySupport

//----------------------------------------------------------------------------------------------------------------------
// Compact
//----------------------------------------------------------------------------------------------------------------------
trait ThriftCompactSupport extends ThriftAbstractSupport {
  import ThriftProtocol.*
  private val protocolFactory: TProtocolFactory = new TCompactProtocol.Factory()

  implicit override def thriftUnmarshaller[T <: TBase[?, ?]: ClassTag]: FromEntityUnmarshaller[T] =
    Unmarshaller.byteStringUnmarshaller
      .forContentTypes(`application/vnd.apache.thrift.compact`)
      .map(protocolFactory.read[T])

  implicit override def thriftMarshaller[T <: TBase[?, ?]]: ToEntityMarshaller[T] =
    Marshaller
      .byteStringMarshaller(`application/vnd.apache.thrift.compact`)
      .compose(protocolFactory.serialize)

}

object ThriftCompactSupport extends ThriftCompactSupport

//----------------------------------------------------------------------------------------------------------------------
// JSON
//----------------------------------------------------------------------------------------------------------------------
trait ThriftJsonSupport extends ThriftAbstractSupport {
  import ThriftProtocol.*
  private val protocolFactory: TProtocolFactory = new TJSONProtocol.Factory()

  implicit override def thriftUnmarshaller[T <: TBase[?, ?]: ClassTag]: FromEntityUnmarshaller[T] =
    Unmarshaller.byteStringUnmarshaller
      .forContentTypes(ContentTypes.`application/json`, `application/vnd.apache.thrift.json`)
      .map(protocolFactory.read[T])

  implicit override def thriftMarshaller[T <: TBase[?, ?]]: ToEntityMarshaller[T] =
    Marshaller.oneOf(
      Marshaller.byteStringMarshaller(ContentTypes.`application/json`).compose(protocolFactory.serialize),
      Marshaller.byteStringMarshaller(`application/vnd.apache.thrift.json`).compose(protocolFactory.serialize)
    )
}

object ThriftJsonSupport extends ThriftJsonSupport

//----------------------------------------------------------------------------------------------------------------------
// Generic
//----------------------------------------------------------------------------------------------------------------------
trait ThriftSupport extends ThriftAbstractSupport {

  private val thriftSupports = Seq(ThriftJsonSupport, ThriftBinarySupport, ThriftCompactSupport)

  implicit override def thriftUnmarshaller[T <: TBase[?, ?]: ClassTag]: FromEntityUnmarshaller[T] = {
    Unmarshaller.firstOf(thriftSupports.map(_.thriftUnmarshaller[T])*)
  }

  implicit override def thriftMarshaller[T <: TBase[?, ?]]: ToEntityMarshaller[T] = {
    Marshaller.oneOf(thriftSupports.map(_.thriftMarshaller[T])*)
  }
}

object ThriftSupport extends ThriftSupport
