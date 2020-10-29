package models

import akka.actor.{Actor, Cancellable, Props}
import javax.inject.{Inject, Singleton}
import play.api.{Application, Logger}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.util.{Failure, Success}

/**
 * Просто запускатель обновлений шаблонов каждые 24 часа
 * TODO - переделать на чем-нибудь cron-подобном
 *
 * @param app
 * @param remoteMemesService
 * @param metadataService
 * @param ex
 */

@Singleton
class UpdateTemplatesTask @Inject()(app: Application,
                                    remoteMemesService: RemoteMemesService,
                                    metadataService: MemeMetadataService,
                                    implicit val ex: ExecutionContext) {

  val logger: Logger = Logger("application")

  def start(): Cancellable = {
    app.actorSystem.scheduler.scheduleWithFixedDelay(1 seconds, 24 hours)(new Runnable {
      override def run(): Unit = {
        remoteMemesService.templates.map { templates =>
          metadataService.refreshTemplates(templates)
          templates.size
        }.onComplete {
          case Success(size) => logger.info(s"${size} templates loaded")
          case Failure(err) => logger.error(s"Error while loading templates: ${err.getMessage}\n${err.getStackTrace.mkString("\n")}")
        }
      }
    })
  }

  start()

}
