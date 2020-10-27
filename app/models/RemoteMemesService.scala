package models


import io.circe.syntax.EncoderOps
import cats.syntax.functor._
import io.circe.{Decoder, Encoder}
import io.circe.generic.JsonCodec

import scala.concurrent.Future


@JsonCodec
case class MemeTemplate(id: String,
                        name: String,
                        url: String,
                        width: Int,
                        height: Int,
                        box_count: Int)


@JsonCodec
case class MemeBox(text: String,
                   x: Option[Int] = None,
                   y: Option[Int] = None,
                   width: Option[Int] = None,
                   height: Option[Int] = None,
                   color: Option[String] = None,
                   outline_color: Option[String] = None)

/**
 * Кодеки для абстрактного типа Data, которые возвращают результат в зависимости от поддтипа
 */
object Data {
  implicit val dataEncoder: Encoder[Data] = Encoder.instance {
    case p@MemeRequestData(_, _) => p.asJson
    case p@MemesSequenceData(_) => p.asJson
  }

  implicit val dataDecoder: Decoder[Data] = List[Decoder[Data]](
    Decoder[MemeRequestData].widen,
    Decoder[MemesSequenceData].widen,
  ).reduceLeft(_ or _)
}

/**
 * Кодеки для абстрактного типа RemoteResponse, которые возвращают результат в зависимости от поддтипа
 */
object RemoteResponse {
  implicit val responseEncoder: Encoder[RemoteResponse] = Encoder.instance {
    case r@SuccessResponse(_, _) => r.asJson
    case r@ErrorResponse(_, _) => r.asJson
  }

  implicit val responseDecoder: Decoder[RemoteResponse] = List[Decoder[RemoteResponse]](
    Decoder[SuccessResponse].widen,
    Decoder[ErrorResponse].widen
  ).reduceLeft(_ or _)


}


@JsonCodec
case class MemeRequest(template_id: String,
                       font: Option[String] = None,
                       max_font_size: Option[Int] = None,
                       boxes: Seq[MemeBox] = Seq()
                      )

/**
 * Обобщенный тип для представления полезной нагрузки ответа сервиса
 */
sealed trait Data

/**
 * Ответ сервиса генерации мемов
 *
 * @param url
 * @param page_url
 */
@JsonCodec
case class MemeRequestData(url: String, page_url: String) extends Data

/**
 * Ответ сервиса шаблонов мемов
 *
 * @param memes
 */
@JsonCodec
case class MemesSequenceData(memes: Seq[MemeTemplate]) extends Data


/**
 * Обобщенный тип для представления ответа сервиса
 */
sealed trait RemoteResponse

@JsonCodec
case class SuccessResponse(data: Data, success: Boolean = true) extends RemoteResponse

@JsonCodec
case class ErrorResponse(error_message: String, success: Boolean = false) extends RemoteResponse

/**
 * Интерфейс получения данных от удленного сервиса
 */
trait RemoteMemesService {

  def templates: Future[Seq[MemeTemplate]]

  def generateMeme(captionImage: MemeRequest): Future[MemeRequestData]

  def image(url: String): Future[Array[Byte]]
}