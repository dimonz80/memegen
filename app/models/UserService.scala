package models

/**
 * Модель пользователя
 *
 * @param name
 * @param apiKey
 */
case class User(name: String, apiKey: String)

/**
 * Интерфейс для сревиса пользователей
 */
trait UserService {

  /**
   * Получить пользователя по имени
   *
   * @param userName
   * @return
   */

  def find(userName: String): Option[User]

  /**
   * Найти пользователя по ключу API
   *
   * @param key
   * @return
   */
  def findByKey(key: String): Option[User]


}
