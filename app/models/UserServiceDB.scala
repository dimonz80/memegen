package models

import javax.inject.Inject
import play.api.db.{DBApi, Database}
import anorm.{RowParser, SqlStringInterpolation}

/**
 * Реализация сервиса хранинея пользоватлей в БД
 *
 * @param dbAPI
 */
class UserServiceDB @Inject()(dbAPI: DBApi) extends UserService {

  lazy val db: Database = dbAPI.database("default")

  val parser: RowParser[User] = anorm.Macro.namedParser[User]

  /**
   * Получить пользователя по имени
   *
   * @param userName
   * @return
   */
  override def find(userName: String): Option[User] = db.withConnection { implicit conn =>
    SQL"""select * from "User" where name = $userName""".as(parser singleOpt)
  }

  /**
   * Найти пользователя по ключу API
   *
   * @param key
   * @return
   */
  override def findByKey(key: String): Option[User] = db.withConnection { implicit conn =>
    SQL"""select * from "User" where apiKey = $key""".as(parser singleOpt)
  }
}

