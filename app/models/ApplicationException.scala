package models

/**
 * Исключение для бизне-логики без сбора стек-трейса, чтобы побыстрее
 *
 * @param message
 */
class ApplicationException(message: String) extends RuntimeException(message) {
  override def fillInStackTrace(): Throwable = this
}

class UnauthorizedAPIAccessException(message: String) extends ApplicationException(message)