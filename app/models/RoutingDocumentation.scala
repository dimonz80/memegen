package models


import javax.inject._
import io.circe.generic.JsonCodec
import play.api.routing.Router

import scala.util.Try

@JsonCodec
case class RouterDocItem(method: String, path: String, controllerMethod: String, description: String = "")


class RoutingDocumentation @Inject()(routerProv: Provider[Router]) {

  lazy val doc = routerProv.get.documentation.flatMap {
    case (method, path, controllerMethod) =>
      val methodName = controllerMethod.split("\\(").apply(0)
      val actionName = methodName.split("\\.").last
      val arr = methodName.split("\\.")
      val controllerName = arr.slice(0, arr.size - 1).mkString(".")

      val description = Try(for {
        method <- Class.forName(controllerName).getMethods.find(_.getName.equals(actionName))
        description <- method.getDeclaredAnnotations.find(_.isInstanceOf[APIDescription]).map(_.asInstanceOf[APIDescription].value)
      } yield {
        description
      }).toOption.flatten


      description.map(d => RouterDocItem(method, path, methodName, d))


  }


}