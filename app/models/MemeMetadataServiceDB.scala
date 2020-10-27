package models

import java.sql.Connection

import javax.inject.{Inject, Singleton}
import play.api.db.DBApi
import anorm._
import anorm.SqlParser._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

@Singleton
class MemeMetadataServiceDB @Inject()(dbApi: DBApi,
                                      imageService: ImageService,
                                      remoteMemesService: RemoteMemesService,
                                      implicit val ex: ExecutionContext
                                     ) extends MemeMetadataService {

  lazy val db = dbApi.database("default")

  val parser = Macro.namedParser[MemeMetadata]

  /**
   * Найти локально сохраненный мем по ID
   *
   * @param id
   * @return
   */
  override def find(id: Long): Option[MemeMetadata] = db.withConnection { implicit conn =>
    SQL"select * from MemeMetadata where id= $id".as(parser singleOpt)
  }

  /**
   * Найти локально сохраненные мемы по условию
   * *
   *
   * @param filter
   * @return
   */
  override def find(filter: String): Seq[MemeMetadata] = db.withConnection { implicit conn =>
    SQL"select * from MemeMetadata where name ~= $filter or comment ~= filter".as(parser.*)
  }

  /**
   * Сохранить сетаданные локального мема
   *
   * @param meme
   * @return - ID
   */
  override def save(meme: MemeMetadata): Long = db.withConnection { implicit conn =>
    SQL"""
         insert into MemeMetadata (url,"user",name,comment) values (${meme.url},${meme.user},${meme.name},${meme.comment})
       """.executeInsert(scalar[Long] single)

  }

  /**
   * Обновить метаданные мема по ID
   *
   * @param id
   * @param localMeme
   */
  override def update(id: Long, localMeme: MemeMetadata): Unit = db.withConnection { implicit conn =>
    SQL"""update MemeMetadata set
         (url,"user",name,comment) = (${localMeme.url},${localMeme.user},${localMeme.name},${localMeme.comment})
         where id = $id
    """.executeUpdate()

  }

  /**
   * Мемы пользователя
   *
   * @param user
   * @return
   */
  override def userMemes(user: User): Seq[MemeMetadata] = db.withConnection { implicit conn =>
    SQL"""
        select * from MemeMetadata where "user" = ${user.name}
       """.as(parser.*)

  }

  override def delete(id: Long): Unit = db.withConnection { implicit conn =>
    SQL"delete from MemeMetadata where id = $id".executeUpdate()
  }


  val templateParser = Macro.namedParser[MemeTemplate]

  /**
   * Получть шаблоны для мемов
   *
   * @return
   */
  override def templates: Seq[MemeTemplate] = db.withConnection { implicit conn =>
    SQL"select * from MemeTemplate".as(templateParser.*)
  }

  /**
   * Найти шаблон по ID
   *
   * @param id
   * @return
   */
  override def template(id: String): Option[MemeTemplate] = db.withConnection { implicit conn =>
    SQL"select * from MemeTemplate where id = $id".as(templateParser.singleOpt)
  }

  def deleteTemplate(id: String) = db.withConnection { implicit conn =>
    SQL"delete from MemeTemplate where id = $id".executeUpdate()
  }

  /**
   * Обновить шадлоны для мемов
   *
   * @param templates
   */
  override def refreshTemplates(templates: Seq[MemeTemplate]): Unit = {

    this.templates.foreach { t =>
      imageService.delete(t.url)
      deleteTemplate(t.id)
    }

    Future.sequence(templates.zipWithIndex.map { case (t, idx) =>
      remoteMemesService.image(t.url).map { templateImg =>
        val newUrl = imageService.put(templateImg)


        db.withConnection { implicit conn =>
          SQL"""
              insert into MemeTemplate
              (id, name, url,width,height,box_count) values
              (${t.id},${t.name},${newUrl},${t.width},${t.height},${t.box_count})

           """.executeUpdate()
        }
        t.copy(url = newUrl)
      }
    }).onComplete {
      case Success(newTemplates) =>


      case Failure(exception) =>
      // TODO log errors
    }


  }
}
