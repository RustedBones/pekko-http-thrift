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

import org.apache.pekko.http.scaladsl.model.headers.Accept
import org.apache.pekko.http.scaladsl.model.{ContentType, ContentTypes, HttpEntity}
import org.apache.pekko.http.scaladsl.server.Directives.*
import org.apache.pekko.http.scaladsl.testkit.ScalatestRouteTest
import org.apache.pekko.http.scaladsl.unmarshalling.Unmarshal
import org.apache.pekko.http.scaladsl.unmarshalling.Unmarshaller.UnsupportedContentTypeException
import fr.davit.thrift.TestMessage
import org.apache.thrift.TSerializer
import org.apache.thrift.protocol.{TBinaryProtocol, TCompactProtocol, TJSONProtocol}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class ThriftSupportSpec extends AnyFlatSpec with Matchers with ScalaFutures with ScalatestRouteTest {
  val thrift               = new TestMessage("test", 42)
  val binary: Array[Byte]  = new TSerializer(new TBinaryProtocol.Factory()).serialize(thrift)
  val compact: Array[Byte] = new TSerializer(new TCompactProtocol.Factory()).serialize(thrift)
  val json: Array[Byte]    = new TSerializer(new TJSONProtocol.Factory()).serialize(thrift)

  val dataForContentType: Map[ContentType, Array[Byte]] = Map(
    ContentTypes.`application/json`                        -> json,
    ThriftProtocol.`application/vnd.apache.thrift.json`    -> json,
    ThriftProtocol.`application/vnd.apache.thrift.binary`  -> binary,
    ThriftProtocol.`application/vnd.apache.thrift.compact` -> compact
  )

  def thriftTestSuite(thriftSupport: ThriftAbstractSupport, contentTypes: ContentType*): Unit = {
    import thriftSupport.{thriftMarshaller, thriftUnmarshaller}

    it should "marshall thrift message with default content type" in {
      Get() ~> get(complete(thrift)) ~> check {
        contentType shouldBe contentTypes.head
        responseAs[Array[Byte]] shouldBe dataForContentType(contentTypes.head)
      }
    }

    it should "marshall thrift message with requested content type" in {
      contentTypes.foreach { ct =>
        Get().withHeaders(Accept(ct.mediaType)) ~> get(complete(thrift)) ~> check {
          contentType shouldBe ct
          responseAs[Array[Byte]] shouldBe dataForContentType(ct)
        }
      }
    }

    it should "unmarshall to thrift message with default content type" in {
      contentTypes.foreach { ct =>
        val entity = HttpEntity(ct, dataForContentType(ct))
        Unmarshal(entity).to[TestMessage].futureValue shouldBe thrift
      }
    }

    it should "fail unmarshalling if the content type is not valid" in {
      val entity = HttpEntity(ContentTypes.`text/plain(UTF-8)`, "")
      Unmarshal(entity).to[TestMessage].failed.futureValue shouldBe an[UnsupportedContentTypeException]
    }
  }

  "ThriftBinarySupport" should behave like thriftTestSuite(
    ThriftBinarySupport,
    ThriftProtocol.`application/vnd.apache.thrift.binary`
  )
  "ThriftCompactSupport" should behave like thriftTestSuite(
    ThriftCompactSupport,
    ThriftProtocol.`application/vnd.apache.thrift.compact`
  )
  "ThriftJsonSupport" should behave like thriftTestSuite(
    ThriftJsonSupport,
    ContentTypes.`application/json`,
    ThriftProtocol.`application/vnd.apache.thrift.json`
  )
  "ThriftSupport" should behave like thriftTestSuite(
    ThriftSupport,
    ContentTypes.`application/json`,
    ThriftProtocol.`application/vnd.apache.thrift.json`,
    ThriftProtocol.`application/vnd.apache.thrift.binary`,
    ThriftProtocol.`application/vnd.apache.thrift.compact`
  )
}
