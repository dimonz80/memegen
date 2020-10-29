package models

/**
 * Исключение для бизнес-логики без сбора стек-трейса, чтобы побыстрее
 *
 * @param message
 */
class ApplicationException(message: String) extends RuntimeException(message) {
  override def fillInStackTrace(): Throwable = this
}

