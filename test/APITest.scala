package ru.dimonz80.tests

import org.scalatest.BeforeAndAfterAll
import org.scalatestplus.play.PlaySpec
import io.circe.parser._
import io.circe.syntax._
import models.{ImageService, MemeBox, MemeMetadata, MemeMetadataService, MemeRequest, MemeTemplate, MemesService, RemoteMemesService, SaveMemeRequest, UserService}
import models.mocks.{ImMemoryUserService, InMemoryImageService, InMemoryMemeMetadataService, MockRemoteMemesService}
import play.api.http.ContentTypes
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers._
import play.api.test._
import play.api.inject.bind
import java.io.File
import java.util.Base64


/**
 * Представление теста для генерации документации
 *
 * @param method      HTTP метод
 * @param path        URL
 * @param description описание для документации
 * @param body        - колбэк с тестом
 */
case class APITest(method: String, path: String, description: String, body: APITest => Unit) {
  def toDoc: String = s"$method $path - $description"
}


class APITestSuite extends PlaySpec with BeforeAndAfterAll {

  lazy val app = new GuiceApplicationBuilder()
    .overrides(bind[RemoteMemesService].to[MockRemoteMemesService])
    .overrides(bind[ImageService].to[InMemoryImageService])
    .overrides(bind[MemeMetadataService].to[InMemoryMemeMetadataService])
    .overrides(bind[UserService].to[ImMemoryUserService])
    //.overrides(bind[DBApi].to[DBAPiMock])
    .build()


  val userService = app.injector.instanceOf[UserService]
  val imageService = app.injector.instanceOf[ImageService]
  val memesService = app.injector.instanceOf[MemesService]


  val user = userService.find("User1").getOrElse(throw new RuntimeException("can't find mock user"))

  val testMetadata = MemeMetadata(None, None, None, "Meme name", Option("Comment"))
  val testImage = new File("./image.jpg")

  val memeId = memesService.createMeme(
    testMetadata,
    testImage)(user)

  val savedMeme = memesService.meme(memeId)(user).get


  val (meta, url, imageData, base64ImageData) = (for {
    meta <- memesService.meme(memeId)(user)
    url <- meta.url
    imageData <- imageService.get(url)
  } yield {
    (meta, url, imageData, Base64.getEncoder.encodeToString(imageData))
  }).getOrElse(fail("Can't get test data"))

  val saveRequest = SaveMemeRequest(meta, Option(base64ImageData))
  val userApiKeyHeader = "API-Key" -> user.apiKey
  val jsonContentTypeHeader = "Content-Type" -> ContentTypes.JSON

  val memeRequest = MemeRequest(
    template_id = "111",
    font = Option("arial"),
    max_font_size = Option(50),
    boxes = Seq(MemeBox(text = "Some text"))
  )

  val apiTests = List(
    APITest("GET", "/api/templates", "get cached templates", { d =>
      s"${d.method} ${d.path} - ${d.description}" in {
        val Some(result) = route(app, FakeRequest(d.method, d.path).withHeaders(userApiKeyHeader))

        val content = Helpers.contentAsString(result)

        status(result) mustEqual OK

        contentType(result) mustEqual Option("application/json")

        decode[Seq[MemeTemplate]](content) match {
          case Left(err) => fail(err)
          case Right(_) => succeed
        }
      }
    }),

    APITest("GET", "/api/memes", "get user's memes", { d =>
      s"${d.method} ${d.path} - ${d.description}" in {
        val Some(result) = route(app, FakeRequest(d.method, d.path).withHeaders(userApiKeyHeader))
        val content = Helpers.contentAsString(result)

        status(result) mustEqual OK

        contentType(result) mustEqual Option("application/json")

        decode[Seq[MemeMetadata]](content) match {
          case Left(err) => fail(err)
          case Right(_) => succeed
        }
      }
    }),

    APITest("GET", s"/api/meme/:id", "get user's meme with id", (d: APITest) => {
      s"${d.method} ${d.path} - ${d.description}" in {
        val Some(result) = route(app, FakeRequest(d.method, d.path.replace(":id", memeId.toString)).withHeaders(userApiKeyHeader))
        val content = Helpers.contentAsString(result)

        status(result) mustEqual OK

        contentType(result) mustEqual Option("application/json")

        decode[MemeMetadata](content) match {
          case Left(err) => fail(err)
          case Right(memeMetadata) => if (memeMetadata.id == Option(memeId)) succeed else fail(s"requires $memeId")
        }
      }
    }),

    APITest("POST", s"/api/meme", "save meme and get id", ((d: APITest) => {
      s"${d.method} ${d.path} - ${d.description} (create new)" in {
        val Some(result) = route(app, FakeRequest(d.method, d.path)
          .withHeaders(
            userApiKeyHeader,
            jsonContentTypeHeader)
          .withBody(
            saveRequest.copy(
              metadata = saveRequest.metadata.copy(id = None)
            ).asJson.toString
          )
        )

        val content = Helpers.contentAsString(result)

        status(result) mustEqual OK

        contentType(result) mustEqual Option("application/json")

        decode[Long](content) match {
          case Left(err) => fail(err)
          case Right(newId) => if (newId != memeId) succeed else fail("same id")
        }
      }

      s"${d.method} ${d.path} - ${d.description} (save existed)" in {
        val Some(result) = route(app, FakeRequest(d.method, d.path)
          .withHeaders(
            userApiKeyHeader,
            jsonContentTypeHeader)
          .withBody(
            saveRequest.asJson.toString
          )
        )

        val content = Helpers.contentAsString(result)

        status(result) mustEqual OK

        contentType(result) mustEqual Option("application/json")

        decode[Long](content) match {
          case Left(err) => fail(err)
          case Right(newId) => if (newId == memeId) succeed else fail("different id")
        }
      }
    })),


    APITest("POST", s"/api/generate", "update meme with existed id", ((d: APITest) => {
      s"${d.method} ${d.path} - ${d.description}" in {
        val Some(result) = route(app, FakeRequest(d.method, d.path)
          .withHeaders(
            userApiKeyHeader,
            jsonContentTypeHeader)
          .withBody(
            memeRequest.asJson.toString
          ))
        val content = Helpers.contentAsString(result)

        status(result) mustEqual OK

        decode[String](content) match {
          case Left(err) => fail(err)
          case Right(base64Str) => {
            Base64.getDecoder.decode(base64Str)
            succeed
          }
        }
      }
    })),

    APITest("GET", s"/api/search?query=.*", "search local memes with regexp", (d: APITest) => {
      s"${d.method} ${d.path} - ${d.description}" in {
        val Some(result) = route(app, FakeRequest(d.method, d.path).withHeaders(userApiKeyHeader))
        val content = Helpers.contentAsString(result)

        status(result) mustEqual OK

        contentType(result) mustEqual Option("application/json")

        decode[Seq[MemeMetadata]](content) match {
          case Left(err) => fail(err)
          case Right(memeMetadata) => succeed

        }
      }
    }),

    APITest("GET", s"/api/image/:name", "get meme image", (d: APITest) => {

      val fullImagePath = savedMeme.url.get

      s"${d.method} ${d.path} - ${d.description}" in {
        val Some(result) = route(
          app,
          FakeRequest(
            d.method,
            fullImagePath
          ).withHeaders(userApiKeyHeader))

        status(result) mustEqual OK

        contentType(result) mustEqual Option("image/jpeg")

      }
    }),

    APITest("DELETE", s"/api/delete/:id", "delete meme with id", { d =>

      s"${d.method} ${d.path} - ${d.description}" in {
        val Some(result) = route(
          app,
          FakeRequest(
            d.method,
            d.path.replace(":id", s"$memeId")
          ).withHeaders(userApiKeyHeader)
        )

        status(result) mustEqual OK

        memesService.meme(memeId)(user) mustBe None

      }
    }),


  )


  // Прогнать все тесты API
  "API" should {
    apiTests.foreach { test =>
      test.body(test)
    }
  }

  // TODO
  "API security" should {
    "GET /api/templates  - response UNAUTHORIZED without header with API key" in {
      val Some(result) = route(app, FakeRequest(GET, "/api/templates")) //.withHeaders(userApiKeyHeader))
      status(result) mustEqual UNAUTHORIZED
    }
  }

  // TODO
  "API negative scenarios" should {
    "TODO" in {
      succeed
    }
  }


  info("---------------- [API DOCS GENERATING] ------------------")
  apiTests.foreach { test => info(test.toDoc) }

  info("--------------[END OF API DOCS GENERATING] --------------")


}