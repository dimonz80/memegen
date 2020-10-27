package models

import java.net.{URLEncoder}

import javax.inject.Inject
import play.api.http.ContentTypes
import play.api.libs.ws.WSClient
import io.circe.parser.decode

import scala.concurrent.{ExecutionContext, Future}


/**
 * Реализация сервиса запросов к удаленному сервису генерации мемов
 *
 * @param ws служба для HTTP запросов
 * @param ex ExecutionContext нужен для WSClient
 */
class RemoteMemesServiceImpl @Inject()(ws: WSClient)(implicit ex: ExecutionContext) extends RemoteMemesService {

  val templatesUrl = "https://api.imgflip.com/get_memes"
  val captionUrl = "https://api.imgflip.com/caption_image"
  val userName = "memgentest"
  val password = "Q1w2e3r4"


  def fillForm(request: MemeRequest, user: String, password: String): String = {

    //Хэлпер для формирования полей формы
    def field[T](name: String, value: T) = {
      value match {
        case o: Option[_] => o.map(v => name -> v.toString)
        case x => Option(name -> x.toString)
      }
    }

    (List(
      field("username", user),
      field("password", password),
      field("template_id", request.template_id),
      field("font", request.font),
      field("max_font_size", request.max_font_size)
    ) ++ request.boxes.zipWithIndex.flatMap { case (box, idx) => // порядок боксов похоже перепутан
      List(
        field(s"boxes[$idx][text]", box.text),
        field(s"boxes[$idx][x]", box.x),
        field(s"boxes[$idx][y]", box.y),
        field(s"boxes[$idx][width]", box.width),
        field(s"boxes[$idx][height]", box.height),
        field(s"boxes[$idx][outline_color]", box.outline_color)
      )
    }).flatten.map { case (k, v) =>
      s"${URLEncoder.encode(k, "UTF-8")}=${URLEncoder.encode(v, "UTF-8")}"
    }.mkString("&")

  }


  override def generateMeme(request: MemeRequest): Future[MemeRequestData] = {
    ws.url(captionUrl)
      .withHttpHeaders("Content-Type" -> s"${ContentTypes.FORM};charset=utf-8")
      .post(fillForm(request, userName, password))
      .map { response =>
        decode[RemoteResponse](response.body) match {
          case Right(SuccessResponse(data@MemeRequestData(_, _), _)) => data
          case Right(ErrorResponse(err, _)) => throw new ApplicationException(err)
          case Right(_) => throw new ApplicationException("Unexpected remote service response")
          case Left(err) => throw err
        }
      }
  }

  override def templates: Future[Seq[MemeTemplate]] = {
    ws.url(templatesUrl).get.map { response =>
      decode[RemoteResponse](response.body) match {
        case Right(SuccessResponse(MemesSequenceData(templates), _)) => templates
        case Right(_) => throw new ApplicationException("Unexpected response")
        case Left(err) => throw err
      }
    }
  }

  override def image(url: String): Future[Array[Byte]] = {
    ws.url(url).get.map(response => response.bodyAsBytes.toArray)
  }
}
