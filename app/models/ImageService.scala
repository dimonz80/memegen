package models

import java.io.{File, FileInputStream, InputStream}


/**
 * Интерфес для сревиса хранения картинок
 */
trait ImageService {

  def get(name: String): Option[Array[Byte]]

  def put(data: Array[Byte]): String

  def put(file: File): String = {
    val fis = new FileInputStream(file)
    val name = put(fis)
    fis.close()
    name
  }

  def put(in: InputStream): String

  def delete(name: String): Unit

}