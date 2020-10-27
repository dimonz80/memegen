package models.mocks

import java.io.{ByteArrayOutputStream, File, InputStream}

import javax.inject.{Inject, Singleton}
import models.ImageService
import play.api.Configuration

/**
 * Заглушка для хранения картинок в ОЗУ
 *
 * @param conf
 */
@Singleton
class InMemoryImageService @Inject()(conf: Configuration) extends ImageService {

  val storage = scala.collection.mutable.Map[String, Array[Byte]]()

  def urlPrefix = conf.get[String]("image.urlPrefix")

  override def get(name: String) = {
    storage.get(name)
  }

  override def put(data: Array[Byte]) = synchronized {
    val newName = urlPrefix + "/" + java.util.UUID.randomUUID().toString
    storage.put(newName, data)
    newName
  }


  override def delete(name: String): Unit = synchronized(storage.remove(name))

  override def put(in: InputStream): String = {
    val buf = new Array[Byte](1024)
    val resultBuf = new ByteArrayOutputStream()
    Iterator.continually(in.read(buf)).takeWhile(_ > 0).foreach { size =>
      resultBuf.write(buf, 0, size)
    }
    put(resultBuf.toByteArray)

  }
}

