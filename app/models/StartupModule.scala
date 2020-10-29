package models


import com.google.inject.AbstractModule


/**
 * Модуль, выполняемый при старте сервера
 * Служит для указания нужных зависимостей
 */
class StartupModule extends AbstractModule {

  override def configure(): Unit = {
    // Указать реализации сервисов
    binder().bind(classOf[RemoteMemesService]).to(classOf[RemoteMemesServiceImpl])
    binder().bind(classOf[ImageService]).to(classOf[ImageServiceDropBox])
    binder().bind(classOf[MemeMetadataService]).to(classOf[MemeMetadataServiceDB])
    binder().bind(classOf[UserService]).to(classOf[UserServiceDB])

    // Инициировать задачу обновления шаблонов
    binder().bind(classOf[UpdateTemplatesTask]).asEagerSingleton()

  }
}
