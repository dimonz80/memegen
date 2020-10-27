package models

import javax.inject.Inject
import play.api.db.DBApi
import anorm.{SQL, SqlStringInterpolation}

class UserServiceDB @Inject()(dbAPI: DBApi) extends UserService {

  val db = dbAPI.database("default")

  val parser = anorm.Macro.namedParser[User]

  override def find(userName: String): Option[User] = db.withConnection { implicit conn =>
    SQL"""select * from "User" where name = $userName""".as(parser singleOpt)
  }

  override def findByKey(key: String): Option[User] = db.withConnection { implicit conn =>
    SQL"""select * from "User" where apiKey = $key""".as(parser singleOpt)
  }
}
