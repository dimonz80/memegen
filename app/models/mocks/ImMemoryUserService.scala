package models.mocks

import javax.inject.Singleton
import models.{User, UserService}


/**
 * Заглушка для сревиса пользователей
 */
@Singleton
class ImMemoryUserService extends UserService {

  val store = Map(
    "User1" -> User("User1", "111"),
    "User2" -> User("User2", "222"),
    "User3" -> User("User3", "333"),
  )

  /**
   * Получить пользователя по имени
   *
   * @param userName
   * @return
   */
  override def find(userName: String): Option[User] = store.get(userName)

  /**
   * Найти пользователя по ключу API
   *
   * @param key
   * @return
   */
  override def findByKey(key: String): Option[User] = store.find { case (_, user) =>
    user.apiKey == key
  }.map(_._2)
}

