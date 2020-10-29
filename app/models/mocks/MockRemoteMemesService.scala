package models.mocks

import java.io.{File, FileInputStream}

import javax.inject.Singleton
import models.{ApplicationException, ErrorResponse, MemeRequest, MemeRequestData, MemeTemplate, MemesSequenceData, RemoteMemesService, RemoteResponse, SuccessResponse}

import scala.concurrent.{ExecutionContextExecutor, Future}


/**
 * Заглушка для симуляции удаленного сервиса
 */
@Singleton
class MockRemoteMemesService extends RemoteMemesService {
  implicit val ex: ExecutionContextExecutor = scala.concurrent.ExecutionContext.global


  def responseFromFile(fileName: String): RemoteResponse = {
    val data = scala.io.Source.fromFile(fileName).getLines().mkString("\n")

    io.circe.parser.decode[RemoteResponse](data) match {
      case Left(err) => throw err
      case Right(value) => value
    }
  }

  def imageFromFile(fileName: String): Array[Byte] = {

    val f = new File(fileName)
    val fis = new FileInputStream(f)
    val buf = new Array[Byte](f.length().toInt)

    fis.read(buf)
    fis.close()
    buf
  }


  override def templates: Future[Seq[MemeTemplate]] = Future {
    responseFromFile("./memes.json") match {
      case SuccessResponse(MemesSequenceData(templates), _) => templates
      case ErrorResponse(error_message, _) => throw new ApplicationException(error_message)
      case _ => throw new ApplicationException("strange data received")
    }
  }

  override def generateMeme(captionImage: MemeRequest): Future[MemeRequestData] = Future {
    responseFromFile("./response.json") match {
      case SuccessResponse(data@MemeRequestData(_, _), _) => data
      case ErrorResponse(error_message, _) => throw new ApplicationException(error_message)
      case _ => throw new ApplicationException("strange data received")
    }
  }


  override def image(url: String): Future[Array[Byte]] = Future {
    imageFromFile("./image.jpg")
  }
}
