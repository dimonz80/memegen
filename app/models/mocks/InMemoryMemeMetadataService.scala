package models.mocks

import java.util.regex.PatternSyntaxException

import javax.inject.{Inject, Singleton}
import models.{ApplicationException, ImageService, MemeMetadata, MemeMetadataService, MemeTemplate, RemoteMemesService, User}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

/**
 * Заглушка для хранения метаданных в ОЗУ
 */
@Singleton
class InMemoryMemeMetadataService @Inject()(imageService: ImageService,
                                            remoteMemesService: RemoteMemesService)
                                           (implicit ex: ExecutionContext)
  extends MemeMetadataService {

  private val repository = scala.collection.mutable.Map[Long, MemeMetadata]()

  private val templatesRepository = scala.collection.mutable.Map[String, MemeTemplate]()

  override def find(id: Long): Option[MemeMetadata] = repository.get(id)

  override def find(regExp: String): Seq[MemeMetadata] = repository.filter { case (_, meme) =>
    try {
      meme.name.toUpperCase.matches(regExp.toUpperCase()) ||
        meme.comment.getOrElse("").toUpperCase().matches(regExp.toUpperCase())
    } catch {
      case e: PatternSyntaxException => throw new ApplicationException(e.getMessage)
    }
  }.values.toSeq

  override def save(meme: MemeMetadata): Long = {
    synchronized {
      val nextKey = {
        if (repository.isEmpty) {
          1
        } else {
          repository.keys.max + 1
        }

      }
      repository.put(nextKey, meme.copy(id = Option(nextKey)))
      nextKey
    }
  }

  override def update(id: Long, localMeme: MemeMetadata): Unit =
    repository.get(id).foreach { meme =>
      repository.put(id, localMeme.copy(id = Option(id)))
    }


  override def templates: Seq[MemeTemplate] = {
    templatesRepository.values.toList
  }

  override def refreshTemplates(templatesList: Seq[MemeTemplate]): Unit = {
    Future.sequence(templatesList.zipWithIndex.map { case (t, idx) =>

      remoteMemesService.image(t.url).map { templateImg =>

        val newUrl = imageService.put(templateImg)
        val newTemplate = t.copy(url = newUrl)
        templatesRepository.remove(t.id)
        templatesRepository.put(t.id, newTemplate)

        newTemplate

      }
    }).onComplete {
      case Success(newTemplates) =>
      // TODO log success message
      case Failure(exception) =>
      // TODO log errors
    }
  }

  override def template(id: String): Option[MemeTemplate] = templatesRepository.get(id)


  override def userMemes(user: User): Seq[MemeMetadata] =
    repository.filter { case (_, data) =>
      data.user == Option(user.name)
    }.values.toList

  override def delete(id: Long): Unit = {
    repository.remove(id)
  }
}