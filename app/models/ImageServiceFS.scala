package models

import java.io.{File, FileInputStream, FileOutputStream, InputStream}

import javax.inject.{Inject, Singleton}
import play.api.Configuration

/**
 * Храним картинка в ФС
 */
@Singleton
class ImageServiceFS @Inject()(conf: Configuration) extends ImageService {

  val filesDir = "./files/"

  val urlPrefix: String = conf.get[String]("image.urlPrefix")

  override def get(name: String): Option[Array[Byte]] = {
    val f = new File(filesDir + name.replace(urlPrefix,""))
    if (f.exists) {
      val buf = new Array[Byte](f.length().toInt)
      val is = new FileInputStream(f)
      is.read(buf)
      is.close()
      Option(buf)
    } else {
      None
    }
  }

  override def put(data: Array[Byte]): String = {
    val fileName = java.util.UUID.randomUUID().toString
    val f = new File(filesDir + fileName)
    val of = new FileOutputStream(f)
    of.write(data)
    of.close()
    urlPrefix + "/" + fileName
  }

  override def put(in: InputStream): String = {
    val fileName = java.util.UUID.randomUUID().toString
    val fos = new FileOutputStream(filesDir + fileName)
    val buf = new Array[Byte](1024)
    Iterator.continually(in.read(buf)).takeWhile(_ > 0).foreach(size => fos.write(buf, 0, size))
    fos.close()
    urlPrefix + "/" + fileName
  }

  override def delete(name: String): Unit = {
    new File(filesDir + name.replace(urlPrefix,"")).delete()
  }
}
