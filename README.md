# Генератор мемов
 
Используется   [Play framework 2.8](https://www.playframework.com/)  и scala 2.13
 
## Структкра проекта


* /app/controllers - контроллеры
* /app/models - бизнес-логика
* /app/views - html-шаблоны
* /conf/evolution/default/\*.sql - скрипты миргации БД
* /conf/application.conf - конфигурационный файл
* /conf/routes - привязка методов контроллеров в URL
 

## Реализованное API 
 
  HTTP method   |   URL pattern             | Description
----------------|---------------------------|-----------------------------
GET             | /api/templates            | Get memes templates
GET             | /api/memes                | Get all user's meme
GET             | /api/meme/$id<[^/]+>      | Get meme by id
POST            | /api/meme                 | delete Meme by id
POST            | /api/generate             | Generate new meme
GET             | /api/search               | Search meme with query
DELETE          | /api/delete/$id<[^/]+>    | Delete Meme by id
GET             | /api/image/$name<[^/]+>   | Get image




## Особенности реализации

Для аутентификации пользователей используется ключ API, который может передаваться 
либо в заголовке ```API-Key``` либо в URL в виде параметра запроса ```API-Key```,
например  ```https://memegenerator-test.herokuapp.com/api/memes?API-Key=111```

В случае, если ключ неверен или отсутствует, будет возвращен отчет об ошибке 

```json
{
  "error" : "Unautorized",
  "details" : {
    "description" : "Needs API key"
  }
}
```
Подобный ответ будет так-же при друших видах ошибок, например, при ошбке валидации JSON:

```json
{
	"error" : "Validation error",
	"details" : {
		".boxes[1].text" : "Attempt to decode value on failed cursor"
	}
}
``` 

В общем случае сообщения об ошибках представлены case class ErrorMessage

```scala
/**
 * Возвращаемое сообщение ошибке
 *
 * @param error общее описание ошибки
 * @param details расшифровка конкретных ошибок
 */
@JsonCodec
case class ErrorMessage(error: String, details: Map[String, String] = Map())
```


Для абстрагирования от конкретных реализаций выделено 4 интерфейса

* ```trait ImageService``` - для работы с файлами
* ```trait UserService``` - для работы с пользователями
* ```trait MemeMetadataService``` - для работы с метаданными сохраненных мемов
* ```trait RemoteMemeService``` - для работы с API удаленного сервиса

Для этих интерфесов сделано несколько реализаций. 

* ```class ImageServiceDropBox extends ImageService``` - реализация хранения фалов в DropBox через DropBox API
* ```class ImageServiceFS extends ImageService``` - реализация хранения фалов в локальной файловой системе
* ```class MemeMetadataServiceDB extends MemeMetadataService``` - реализация хранения метаданных в БД
* ```class RemoteMemesServiceImpl extends RemoteMemeService``` - реализация реальной работы с удаленным API генерации мемов
* ```class UserServiceDB extends UserService``` - реализация хранения пользователей в БД

В пакете ```models.mocks``` - реализации этих интефейсов для тестов.

Конкретные реализации указываются в классе ```StartupModule``` при поможи DI контейнета (используется Google Guice).

Например
```scala
binder().bind(classOf[ImageService]).to(classOf[ImageServiceDropBox])
```


## Тесты
Тесты находятся в файле ```test/APITest.scala```

Для реализации пункта задания "В качестве дополнительного
задания: написать тесты так, чтобы они использовались для генерации
документации по методам" тесты сдаланы в виде списка объектов с
опиcанием HTTP метода, URL, описанием и методом, который вызваестя при прогоне теста

```scala
/**
 * Представление теста для генерации документации
 *
 * @param method      HTTP метод
 * @param path        URL
 * @param description описание для документации
 * @param body        колбэк с тестом
 */
case class APITest(method: String, path: String, description: String, body: APITest => Unit) {
  def toDoc: String = s"$method $path - $description"
}
```
Прогон тестов осуществляется итерацией по списку
```scala
    val apiTest : List[APITest]

    apiTests.foreach { test =>
      test.body(test)
    
  }
```

Генерация документации делается тоже проходом по списку, только без вызова тестового метода

```scala
 apiTests.foreach { test => info(test.toDoc) }
 ```

## Генерация документации

Документацию можно генерировать, основываясь на информации о роутинге, 
которую предоставляет Play Framework.

Смысл в том, чтобы анотировать методы контроллера, которые обрабатывают вызовы API,
затем получить все маршруты в виде информации об HTTP методе, URL и функции контроллера,
и отфильтровать аннотиваронные. В аннотацию можно добавить инфрмацию для формирования документации. 
 
Для пометки методов контроллера сделана аннотация ```APIDescription```, 
в качестве параметра которой можно задать описание 
 
 Используестся примерно так
 
 ```scala
  @APIDescription("Search meme with query")
  def search(query: String) = apiAction { implicit request =>
    apiAction.withUser { implicit user =>
      Ok(memesService.search(query).asJson)
    }
  }
 ```
 
Такая генерация документации реализована в ```models.RoutingDocumentation```.
Сгенерированная документаия опебликована по адреса ```/apiDoc```


## Обновление списка шаблонов по расписанию
 
Реализовано тривиально через Akka Scheduler. 
Обновляет шаблоны при запуске приложения и потом каждые 24 часа
 См. ```models.UpdateTemplatesTask```

По хорошему нужен Scheduler с возможностью задавать расписание более продвинутым способом, 
например Quartz.


## Deployment

Развернут на Heroku  

На скорую руку сооружен GUI чтобы поиграться с API - https://memegenerator-test.herokuapp.com/?apiKey=111

Сгенерированная документация по API доступна тут https://memegenerator-test.herokuapp.com/apiDoc

## Недостатки текущей реализации

* Работа с картинками через Array[Byte]
* Для нормальной работы по расписанию нужны более продвинутые инструменты
* Получилось немного громоздко, "интерпрайзно"
* В тестах не отражены негативные сценарии


