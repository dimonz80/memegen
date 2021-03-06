package models

import java.io.File
import javax.inject.{Inject, Singleton}
import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration.DurationInt


/**
 * Сервис для работы REST API
 *
 * @param remoteMemesService
 * @param metadataService
 * @param imageService
 */
@Singleton
class MemesService @Inject()(
                              remoteMemesService: RemoteMemesService,
                              metadataService: MemeMetadataService,
                              imageService: ImageService
                            )(implicit ex: ExecutionContext) {


  /**
   * Список мемов пользователя
   *
   * @param user
   * @return
   */
  def memes(implicit user: User): Seq[MemeMetadata] =
    metadataService.userMemes(user)

  /**
   * Нафти мем по ID для пользователя
   *
   * @param id
   * @param user
   * @return
   */
  def meme(id: Long)(implicit user: User): Option[MemeMetadata] =
    metadataService.find(id).filter(_.user == Option(user.name))

  /**
   * Поиск мемов по регулярному выражению для пользователя
   *
   * @param regExp
   * @param user
   * @return
   */
  def search(regExp: String)(implicit user: User): Seq[MemeMetadata] =
    metadataService.find(regExp)

  /**
   * Получить локально сохраненную картинку мема
   *
   * @param name
   * @return
   */
  def image(name: String): Option[Array[Byte]] = imageService.get(name)


  /**
   * Запросить удаленный сервис создать мем
   *
   * @param request
   * @return
   */
  def generateMeme(request: MemeRequest): Array[Byte] = {
    Await.result(
      remoteMemesService.generateMeme(request).map {
        case data: MemeRequestData => data
        case _ => throw new ApplicationException("Unknown memes service response")
      }.flatMap { response =>
        remoteMemesService.image(response.url)
      }, 60.seconds)

  }

  /**
   * Сохранить метаданные и картинку  с указанием авторства
   *
   * @param localMeme
   * @param imageData
   * @param user
   * @return
   */
  def createMeme(localMeme: MemeMetadata,
                 imageData: Array[Byte])
                (implicit user: User): Long = {
    val newUrl = imageService.put(imageData)
    metadataService.save(localMeme.copy(user = Option(user.name), url = Option(newUrl)))

  }

  /**
   * Сохранить метаданные и картинку  с указанием авторства
   *
   * @param localMeme
   * @param imageData
   * @param user
   * @return
   */
  def createMeme(localMeme: MemeMetadata,
                 imageData: File)
                (implicit user: User): Long = {
    val newUrl = imageService.put(imageData)
    metadataService.save(localMeme.copy(user = Option(user.name), url = Option(newUrl)))

  }


  /**
   * Обглвить метаданные и опционально картинку
   *
   * @param id
   * @param localMeme
   * @param imageData
   * @param user
   */
  def updateMeme(id: Long,
                 localMeme: MemeMetadata,
                 imageData: Option[Array[Byte]] = None)
                (implicit user: User): Unit = {

    metadataService.find(id).foreach { metaData =>

      val newFileName = imageData.map { data =>
        metaData.url.foreach(imageService.delete)
        imageService.put(data)
      }.orElse(localMeme.url)


      val newMetadata = localMeme.copy(
        url = newFileName,
        user = Option(user.name)
      )

      metadataService.update(id, newMetadata)

    }
  }

  /**
   * Удалить мем по ID для пользователя
   *
   * @param id
   * @param user
   */
  def deleteMeme(id: Long)(implicit user: User): Unit = {
    metadataService.find(id).foreach { metaData =>
      metaData.url.foreach { url =>
        imageService.delete(url)
      }
      metadataService.delete(id)
    }
  }

  /**
   * Получить список шаблонов
   *
   * @return
   */
  def templates: Seq[MemeTemplate] = metadataService.templates


}