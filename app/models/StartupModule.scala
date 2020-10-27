package models


import com.google.inject.AbstractModule
import models.mocks.{ImMemoryUserService, InMemoryImageService, InMemoryMemeMetadataService, MockRemoteMemesService}


/**
 * Модуль, выполняемый при старте сервера
 * Сулжит для указания нужных зависимостей
 */
class StartupModule extends AbstractModule {

  override def configure(): Unit = {
    // Указать реализации сервисов

    binder().bind(classOf[RemoteMemesService]).to(classOf[RemoteMemesServiceImpl])
    binder().bind(classOf[ImageService]).to(classOf[ImageServiceDropBox])
    binder().bind(classOf[MemeMetadataService]).to(classOf[MemeMetadataServiceDB])
    binder().bind(classOf[UserService]).to(classOf[UserServiceDB])

    //binder().bind(classOf[ImageService]).to(classOf[InMemoryImageService])
    //binder().bind(classOf[ImageService]).to(classOf[ImageServiceFS])
    //binder().bind(classOf[MemeMetadataService]).to(classOf[InMemoryMemeMetadataService])
    //binder().bind(classOf[RemoteMemesService]).to(classOf[MockRemoteMemesService])


    // Инициировать задачу обновления шаблонов
    binder().bind(classOf[UpdateTemplatesTask]).asEagerSingleton()

  }
}
