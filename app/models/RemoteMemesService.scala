package models


import io.circe.syntax.EncoderOps
import cats.syntax.functor._
import io.circe.{Decoder, Encoder}
import io.circe.generic.JsonCodec

import scala.concurrent.Future


/**
 * Шаблон мема
 *
 * @param id
 * @param name
 * @param url
 * @param width
 * @param height
 * @param box_count
 */
@JsonCodec
case class MemeTemplate(id: String,
                        name: String,
                        url: String,
                        width: Int,
                        height: Int,
                        box_count: Int)


/**
 * Часть запроса на генерацию сервиса с описанием блока текста
 *
 * @param text
 * @param x
 * @param y
 * @param width
 * @param height
 * @param color
 * @param outline_color
 */
@JsonCodec
case class MemeBox(text: String,
                   x: Option[Int] = None,
                   y: Option[Int] = None,
                   width: Option[Int] = None,
                   height: Option[Int] = None,
                   color: Option[String] = None,
                   outline_color: Option[String] = None)


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
 * Обобщенный тип для представления ответа сервиса
 */
sealed trait RemoteResponse

/**
 * Успешный ответ сервиса
 *
 * @param data    полезная нагрузка
 * @param success флаг "успешности" запроса
 */
@JsonCodec
case class SuccessResponse(data: Data, success: Boolean = true) extends RemoteResponse

/**
 * Ответ с ошибкой
 *
 * @param error_message сообзение об ошибке
 * @param success       - флаг "неуспешности" запроса
 */
@JsonCodec
case class ErrorResponse(error_message: String, success: Boolean = false) extends RemoteResponse

/**
 * Интерфейс получения данных от удленного сервиса
 */
trait RemoteMemesService {

  /**
   * Получить список шаблонов
   *
   * @return
   */
  def templates: Future[Seq[MemeTemplate]]

  /**
   * Отправить запрос на генерацию мема и получить объект соссылкой на картинку
   *
   * @param captionImage
   * @return
   */
  def generateMeme(captionImage: MemeRequest): Future[MemeRequestData]

  /**
   * Получить картинку
   *
   * @param url
   * @return
   */
  def image(url: String): Future[Array[Byte]]
}