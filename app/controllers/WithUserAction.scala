package controllers

import javax.inject.{Inject, Singleton}
import models.{User, UserService}
import play.api.http.ContentTypes
import play.api.mvc.{ActionRefiner, Request, Result, Results, WrappedRequest}
import io.circe.syntax._

import scala.concurrent.{ExecutionContext, Future}


/**
 * Класс для добавления пользователя в запрос
 * См. https://www.playframework.com/documentation/2.8.x/ScalaActionsComposition
 *
 * @param user
 * @param request
 * @tparam A
 */
@Singleton
class UserRequest[A](var user: User, request: Request[A]) extends WrappedRequest[A](request) {
  def withUser(f: User => Result): Result = f(user)
}


/**
 * Action с UserRequest'ом вместо просто Request'а
 * См. https://www.playframework.com/documentation/2.87.x/ScalaActionsComposition
 *
 * @param ec
 */
@Singleton
class WithUserAction @Inject()(userService: UserService)(implicit ec: ExecutionContext) extends ActionRefiner[Request, UserRequest] {
  def executionContext: ExecutionContext = ec

  val apiKeyHeader = "API-Key"

  override protected def refine[A](request: Request[A]): Future[Either[Result, UserRequest[A]]] = Future.successful {
    val headerKey = request.headers.get(apiKeyHeader)
    val queryKey = request.queryString.get(apiKeyHeader).flatMap(_.headOption)

    (for {
      apiKey <- headerKey.orElse(queryKey)
      user <- userService.findByKey(apiKey)
    } yield {
      new UserRequest(user, request)
    }).toRight {
      Results.Unauthorized(ErrorMessage("Unauthorized").asJson.toString).as(ContentTypes.JSON)
    }
  }
}
