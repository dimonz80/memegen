package models

import io.circe.generic.JsonCodec


/**
 * Метаданные локального мема
 *
 * @param id
 * @param url
 * @param name
 * @param comment
 */
@JsonCodec
case class MemeMetadata(
                         id: Option[Long],
                         url: Option[String],
                         user: Option[String],
                         name: String,
                         comment: Option[String]
                       )

/**
 * Запрос на создание/изменение мема
 *
 * @param metadata метаданные картинки
 * @param base64Image картинка в Base64
 */
@JsonCodec
case class SaveMemeRequest(metadata: MemeMetadata, base64Image: Option[String])

/**
 * Интерфейс для хранения метаданных
 */
trait MemeMetadataService {


  /**
   * Найти локально сохраненный мем по ID
   *
   * @param id
   * @return
   */
  def find(id: Long): Option[MemeMetadata]


  /**
   * Найти локально сохраненные мемы по условию
   * *
   *
   * @param regExp
   * @return
   */
  def find(regExp: String): Seq[MemeMetadata]


  /**
   * Сохранить метаданные локального мема
   *
   * @param meme
   * @return - ID
   */
  def save(meme: MemeMetadata): Long


  /**
   * Обновить метаданные мема по ID
   *
   * @param id
   * @param localMeme
   */
  def update(id: Long, localMeme: MemeMetadata): Unit


  /**
   * Мемы пользователя
   *
   * @param user
   * @return
   */
  def userMemes(user: User): Seq[MemeMetadata]

  def delete(id: Long): Unit

  /**
   * Получть шаблоны для мемов
   *
   * @return
   */
  def templates: Seq[MemeTemplate]

  /**
   * Найти шаблон по ID
   *
   * @param id
   * @return
   */
  def template(id: String): Option[MemeTemplate]

  /**
   * Обновить шадлоны для мемов
   *
   * @param templates
   */
  def refreshTemplates(templates: Seq[MemeTemplate]): Unit

}


