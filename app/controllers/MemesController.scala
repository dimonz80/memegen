package controllers

import java.io.{ByteArrayInputStream, FileInputStream}
import java.util.Base64

import akka.util.ByteString
import io.circe.{CursorOp, Decoder, DecodingFailure, HCursor, JsonObject}
import io.circe.generic.JsonCodec
import javax.inject._
import play.api.mvc._
import play.api.libs.circe.Circe
import io.circe.syntax._
import io.circe.parser.decode
import models.{APIDescription, ApplicationException, ImageService, MemeBox, MemeMetadata, MemeRequest, MemeTemplate, MemesService, RoutingDocumentation}
import play.api.http.{ContentTypes, HttpEntity}

import scala.concurrent.ExecutionContext

/**
 * Котроллер для API
 *
 * @param controllerComponents
 * @param memesService
 * @param apiAction
 * @param ec
 */
@Singleton
class MemesController @Inject()(
                                 val controllerComponents: ControllerComponents,
                                 memesService: MemesService,
                                 apiAction: APIAction,
                                 routingDocumentation: RoutingDocumentation,
                                 implicit val ec: ExecutionContext
                               ) extends BaseController with Circe {


  def index(apiKey: String) = Action { implicit request =>
    Ok(views.html.bootstrap(apiKey))
  }

  @APIDescription("Get memes templates")
  def templates = apiAction { implicit request =>
    apiAction.withUser { implicit user =>
      Ok(memesService.templates.asJson).as(ContentTypes.JSON)
    }
  }


  @APIDescription("Generate new meme ")
  def generateMeme = apiAction { request =>
    import ErrorMessage._

    request.body.asJson.map { data =>
      decode[MemeRequest](data.toString) match {
        case Right(memeRequest) =>
          val newMeme = memesService.generateMeme(memeRequest)
          val base64Image = Base64.getEncoder.encodeToString(newMeme)

          Ok(base64Image.asJson).as(ContentTypes.JSON)

        case Left(err: DecodingFailure) => BadRequest(err.asErrorMessage)
        case Left(err) => BadRequest(err.asErrorMessage)
      }
    }.getOrElse {
      BadRequest(ErrorMessage("JSON required").asJson).as(ContentTypes.JSON)
    }
  }


  @APIDescription("Get all user's meme")
  def memes = apiAction { implicit request =>
    apiAction.withUser { implicit user =>
      Ok(memesService.memes.asJson).as(ContentTypes.JSON)
    }
  }


  @APIDescription("Get meme by id")
  def meme(id: Long) = apiAction { implicit request =>
    apiAction.withUser { implicit user =>
      memesService.meme(id).map { meme =>
        Ok(meme.asJson)
      }.getOrElse {
        NotFound(ErrorMessage("Meme not found").asJson)
      }.as(ContentTypes.JSON)
    }
  }


  @APIDescription("Search meme with query")
  def search(query: String) = apiAction { implicit request =>
    apiAction.withUser { implicit user =>
      Ok(memesService.search(query).asJson)
    }
  }

  @APIDescription("Get image")
  def image(name: String) = Action { request =>

    memesService.image(request.uri).map { data =>
      Result(
        header = ResponseHeader(200, Map.empty),
        body = HttpEntity.Strict(ByteString(data), Option("image/jpeg")))
    }.getOrElse {
      NotFound(ErrorMessage("Image not found").asJson).as(ContentTypes.JSON)
    }
  }

  @APIDescription("delete Meme by id")
  def saveMeme = apiAction { implicit request =>
    import ErrorMessage._

    apiAction.withUser { implicit user =>
      request.body.asJson.map { json =>
        decode[SaveMemeRequest](json.toString) match {
          case Right(SaveMemeRequest(metadata, imageData)) =>
            val id = metadata.id.map { id =>
              memesService.updateMeme(id, metadata.copy(user = Option(user.name)))
              id
            }.getOrElse {

              val decodedImageData = try {
                imageData.map(Base64.getDecoder.decode)
              } catch {
                case e: Exception => throw new ApplicationException("Can't decode image data")
              }

              decodedImageData.map { data =>
                memesService.createMeme(metadata.copy(user = Option(user.name)), data)
              }.getOrElse {
                throw new ApplicationException("For new meme require file")
              }
            }

            Ok(id.asJson)

          case Left(err: DecodingFailure) => BadRequest(err.asErrorMessage)
          case Left(err) => BadRequest(err.asErrorMessage)
        }
      }.getOrElse {
        BadRequest(ErrorMessage("JSON required").asJson)
      }
    }

  }


  @APIDescription("delete Meme by id")
  def deleteMeme(id: Long) = apiAction { implicit request =>
    apiAction.withUser { implicit user =>
      memesService.deleteMeme(id)
      Ok
    }
  }


  def apiDoc = Action { implicit request =>
    Ok(views.html.apiDoc(routingDocumentation.doc))
  }
}

/**
 * Возвращаемое сообщение ошибке
 *
 * @param error общее описание ошибки
 * @param details расшифровка конкретных ошибок
 */
@JsonCodec
case class ErrorMessage(error: String, details: Map[String, String] = Map())

object ErrorMessage {

  implicit class DecodingFailureWrapper(df: DecodingFailure) {
    def asErrorMessage = ErrorMessage("Validation error", Map(CursorOp.opsToPath(df.history) -> df.message)).asJson
  }

  implicit class ErrorWrapper(e: io.circe.Error) {
    def asErrorMessage = ErrorMessage(e.getMessage).asJson
  }

}

/**
 * Запрос на создание/изменение мема
 *
 * @param metadata
 * @param base64Image
 */
@JsonCodec
case class SaveMemeRequest(metadata: MemeMetadata, base64Image: Option[String])