package controllers

import io.circe.{CursorOp, DecodingFailure, Json}
import io.circe.generic.JsonCodec
import io.circe.syntax._

/**
 * Возвращаемое сообщение ошибке
 *
 * @param error   общее описание ошибки
 * @param details расшифровка конкретных ошибок
 */
@JsonCodec
case class ErrorMessage(error: String, details: Map[String, String] = Map())

object ErrorMessage {

  implicit class DecodingFailureWrapper(df: DecodingFailure) {
    def asErrorMessage: Json = ErrorMessage("Validation error", Map(CursorOp.opsToPath(df.history) -> df.message)).asJson
  }

  implicit class ErrorWrapper(e: io.circe.Error) {
    def asErrorMessage: Json = ErrorMessage(e.getMessage).asJson
  }

}
