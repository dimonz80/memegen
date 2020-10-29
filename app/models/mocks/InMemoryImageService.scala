package models.mocks

import java.io.{ByteArrayOutputStream, InputStream}

import javax.inject.{Inject, Singleton}
import models.ImageService
import play.api.Configuration

import scala.collection.mutable

/**
 * Заглушка для хранения картинок в ОЗУ
 *
 * @param conf
 */
@Singleton
class InMemoryImageService @Inject()(conf: Configuration) extends ImageService {

  val storage: mutable.Map[String, Array[Byte]] = scala.collection.mutable.Map[String, Array[Byte]]()

  def urlPrefix: String = conf.get[String]("image.urlPrefix")

  /**
   * Получить картинку по имени
   *
   * @param name
   * @return
   */
  override def get(name: String): Option[Array[Byte]] = storage.get(name)

  /**
   * Сохранить картинку и получить имя
   *
   * @param data бинарное представлени картинки
   * @return имя катинки
   */
  override def put(data: Array[Byte]): String = synchronized {
    val newName = urlPrefix + "/" + java.util.UUID.randomUUID().toString
    storage.put(newName, data)
    newName
  }


  /**
   * Удалить картинку по имени
   *
   * @param name имя картинки
   */
  override def delete(name: String): Unit = synchronized(storage.remove(name))


  /**
   * Cохранить картинку и получить имя
   *
   * @param in поток с данными картинки
   * @return имя картинки
   */
  override def put(in: InputStream): String = {
    val buf = new Array[Byte](1024)
    val resultBuf = new ByteArrayOutputStream()
    Iterator.continually(in.read(buf)).takeWhile(_ > 0).foreach { size =>
      resultBuf.write(buf, 0, size)
    }
    put(resultBuf.toByteArray)

  }
}
