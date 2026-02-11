/*
 * Copyright 2026 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package utils

import org.slf4j
import play.api.Logger
import utils.LogContext.{ClassContext, MethodContext}

import scala.language.implicitConversions

trait Logging {
  private val classLoggingContext: ClassContext = ClassContext(this.getClass.getSimpleName.replace("$", ""))
  val logger: LoggerWithContext = LoggerWithContext(Logger(this.getClass), classLoggingContext)
  def infoLogger(mc: MethodContext): String => Unit = (msg: String) => logger.info(mc, msg)
  def warnLogger(mc: MethodContext): String => Unit = (msg: String) => logger.warn(mc, msg)
  def errorLogger(mc: MethodContext): (String, Option[Throwable]) => Unit = (msg: String, exOpt: Option[Throwable]) =>
    exOpt.fold(logger.warn(mc, msg))(ex => logger.error(mc, msg, ex))
}

case class LoggerWithContext(underlying: Logger, cc: ClassContext) {
  private val logger: slf4j.Logger = underlying.logger
  def info(mc: MethodContext, msg: String): Unit = logger.info(s"[$cc][$mc] - $msg")
  def warn(mc: MethodContext, msg: String): Unit = logger.warn(s"[$cc][$mc] - $msg")
  def error(mc: MethodContext, msg: String): Unit = logger.error(s"[$cc][$mc] - $msg")
  def error(mc: MethodContext, msg: String, ex: Throwable): Unit = logger.error(s"[$cc][$mc] - $msg", ex)
}

enum LogContext(str: String) {
  override def toString: String = str
  case ClassContext(str: String) extends LogContext(str)
  case MethodContext(str: String) extends LogContext(str)
}

object LogContext {
  implicit def stringToMethodContext(str: String): MethodContext = MethodContext(str)
}

