package models

import java.io.{File, FileInputStream, InputStream}


/**
 * Интерфес для сревиса хранения картинок
 */
trait ImageService {
  /**
   * Получить картинку по имени
   *
   * @param name
   * @return
   */
  def get(name: String): Option[Array[Byte]]

  /**
   * Сохранить картинку и получить имя
   *
   * @param data бинарное представление картинки
   * @return имя катинки
   */
  def put(data: Array[Byte]): String

  def put(file: File): String = {
    val fis = new FileInputStream(file)
    val name = put(fis)
    fis.close()
    name
  }

  /**
   * Cохранить картинку и получить имя
   *
   * @param in поток с данными картинки
   * @return имя картинки
   */
  def put(in: InputStream): String

  /**
   * Удалить картинку по имени
   *
   * @param name имя картинки
   */
  def delete(name: String): Unit

}