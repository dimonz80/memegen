package controllers

import java.util.Base64
import akka.util.ByteString
import io.circe.DecodingFailure
import javax.inject._
import play.api.mvc._
import play.api.libs.circe.Circe
import io.circe.syntax._
import io.circe.parser.decode
import models.{APIDescription, MemeRequest, MemesService, RoutingDocumentation, SaveMemeRequest}
import play.api.http.{ContentTypes, HttpEntity}

import scala.concurrent.ExecutionContext
import scala.util.Try

import ErrorMessage._

/**
 * Котроллер для API
 *
 * @param controllerComponents
 * @param memesService
 * @param ec
 */
@Singleton
class MemesController @Inject()(
                                 val controllerComponents: ControllerComponents,
                                 memesService: MemesService,
                                 userAction: WithUserAction,
                                 routingDocumentation: RoutingDocumentation,
                                 implicit val ec: ExecutionContext
                               ) extends BaseController with Circe {


  val apiAction: ActionBuilder[UserRequest, AnyContent] = Action.andThen(userAction)


  def index(apiKey: String): Action[AnyContent] = Action { implicit request =>
    Ok(views.html.bootstrap(apiKey))
  }

  @APIDescription("Get memes templates")
  def templates: Action[AnyContent] = apiAction { implicit request =>
    Ok(memesService.templates.asJson).as(ContentTypes.JSON)
  }

  @APIDescription("Generate new meme")
  def generateMeme: Action[AnyContent] = apiAction { request =>
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


  @APIDescription("Get all user's memes")
  def memes: Action[AnyContent] = apiAction { implicit request =>
    Ok(memesService.memes(request.user).asJson).as(ContentTypes.JSON)
  }


  @APIDescription("Get meme by id")
  def meme(id: Long): Action[AnyContent] = apiAction { implicit request =>
    memesService.meme(id)(request.user).map { meme =>
      Ok(meme.asJson)
    }.getOrElse {
      NotFound(ErrorMessage("Meme not found").asJson)
    }.as(ContentTypes.JSON)

  }


  @APIDescription("Search meme with regexp")
  def search(query: String): Action[AnyContent] = apiAction { implicit request =>
    request.withUser { implicit user =>
      Ok(memesService.search(query).asJson)
    }
  }

  @APIDescription("Get image by name")
  def image(name: String): Action[AnyContent] = Action { request =>
    memesService.image(request.uri).map { data =>
      Result(
        header = ResponseHeader(200, Map.empty),
        body = HttpEntity.Strict(ByteString(data), Option("image/jpeg")))
    }.getOrElse {
      NotFound(ErrorMessage("Image not found").asJson).as(ContentTypes.JSON)
    }
  }

  @APIDescription("Save meme")
  def saveMeme: Action[AnyContent] = apiAction { implicit request =>
    import ErrorMessage._

    request.withUser { implicit user =>
      request.body.asJson.map { json =>
        decode[SaveMemeRequest](json.toString) match {
          case Right(SaveMemeRequest(metadata, imageData)) =>
            metadata.id.map { id =>
              memesService.updateMeme(id, metadata.copy(user = Option(user.name)))
              Ok(id.asJson)
            }.orElse {
              Try(imageData.map(Base64.getDecoder.decode)).toOption.flatten.map { base64Data =>
                val newId = memesService.createMeme(metadata.copy(user = Option(user.name)), base64Data)
                Ok(newId.asJson)
              }
            }.getOrElse {
              BadRequest(ErrorMessage("Request must contains image in base64 encoding").asJson)
            }

          case Left(err: DecodingFailure) => BadRequest(err.asErrorMessage)
          case Left(err) => BadRequest(err.asErrorMessage)
        }
      }.getOrElse {
        BadRequest(ErrorMessage("JSON required").asJson)
      }
    }

  }


  @APIDescription("Delete meme by id")
  def deleteMeme(id: Long): Action[AnyContent] = apiAction { implicit request =>
    request.withUser { implicit user =>
      memesService.deleteMeme(id)
      Ok
    }
  }


  def apiDoc: Action[AnyContent] = Action { implicit request =>
    Ok(views.html.apiDoc(routingDocumentation.doc))
  }
}


