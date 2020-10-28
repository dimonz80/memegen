package models
import java.io.{ByteArrayInputStream, ByteArrayOutputStream, InputStream}

import javax.inject.{Inject, Singleton}
import com.dropbox.core.v2.DbxClientV2
import com.dropbox.core.{DbxAppInfo, DbxRequestConfig, DbxWebAuth}
import play.api.Configuration

/**
 * Реализация хранения файов в DropBox
 * @param conf
 */
@Singleton
class ImageServiceDropBox @Inject()(conf : Configuration) extends ImageService {

  val app_key = "1zjkda0bzj3vdsk"
  val app_secret = "heogqoulh5j2c2k"
  val token = "PrcBN89UVckAAAAAAAAAAepI9RR25NahNgXxDYfQytViJBRcJBIH8lLStgsSDaY6"

  val urlPrefix = conf.get[String]("image.urlPrefix")



  lazy val appInfo = new DbxAppInfo(app_key, app_secret)
  lazy val config = new DbxRequestConfig("azavea/rf-dropbox-test")

  lazy val webAuth = new DbxWebAuth(config, appInfo)
  lazy val authRequest = DbxWebAuth.newRequestBuilder().withNoRedirect().build()

  lazy val client = new DbxClientV2(config, token)

  override def get(name: String): Option[Array[Byte]] = {
    val out = new ByteArrayOutputStream()
    val remoteName = name.replace(urlPrefix,"")
    client.files().downloadBuilder(remoteName).download(out)

    val res = out.toByteArray
    out.close()
    Option(res)
  }

  override def put(data: Array[Byte]): String = {
    val newName = java.util.UUID.randomUUID.toString
    val in = new ByteArrayInputStream(data)
    client.files.uploadBuilder("/" + newName).uploadAndFinish(in)
    in.close()
    urlPrefix + "/" + newName
  }

  override def put(in: InputStream): String = {
    val newName = java.util.UUID.randomUUID.toString
    client.files.uploadBuilder("/" + newName).uploadAndFinish(in)
    in.close()
    urlPrefix + "/" + newName
  }

  override def delete(name: String): Unit = {
    val remoteName = name.replace(urlPrefix,"")
    client.files.deleteV2(remoteName)
  }
}
