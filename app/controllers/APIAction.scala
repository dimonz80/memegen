package controllers

import javax.inject.{Inject, Singleton}
import models.{ApplicationException, UnauthorizedAPIAccessException, User, UserService}
import play.api.http.ContentTypes
import play.api.mvc.{ActionBuilder, AnyContent, BodyParser, BodyParsers, Request, Result, Results}
import io.circe.syntax._
import scala.concurrent.{ExecutionContext, Future}


/**
 * Обработчик запросов API
 *
 * @param bodyParsers
 * @param userService
 * @param ex
 */
@Singleton
class APIAction @Inject()(bodyParsers: BodyParsers.Default,
                          userService: UserService,
                          implicit val ex: ExecutionContext)
  extends ActionBuilder[Request, AnyContent] {


  val apiKeyHeader = "API-Key"

  /**
   * Хелпер для работы с авторизованными пользователями
   *
   * @param f колбек, работающий в контексте пользователя
   * @param request
   * @tparam T
   * @return
   */
  def withUser[T](f: User => T)(implicit request: Request[_]): T = {

    val headerKey = request.headers.get(apiKeyHeader)
    val queryKey = request.queryString.get(apiKeyHeader).flatMap(_.headOption)

    (for {
      apiKey <- headerKey.orElse(queryKey)
      user <- userService.findByKey(apiKey)
    } yield {
      f(user)
    }).getOrElse {
      Results.Unauthorized(
        ErrorMessage("Unauthorized", Map("description" -> "Needs API key")).asJson.toString
      ).as(ContentTypes.JSON)
    }
  }

  override def parser: BodyParser[AnyContent] = bodyParsers

  override def invokeBlock[A](request: Request[A], block: Request[A] => Future[Result]): Future[Result] = {
    try {
      withUser { user =>
        block(request)
      }(request)
    }
    catch {
 
      case e: ApplicationException => Future {
        Results.BadRequest(ErrorMessage(e.getMessage).asJson.toString()).as(ContentTypes.JSON)
      }

      case e: Exception =>
        println(e.getMessage)
        Future(Results.InternalServerError(ErrorMessage("Something goes wrong").asJson.toString).as(ContentTypes.JSON))
    }
  }


  override protected def executionContext: ExecutionContext = ex
}