/*
 * Copyright 2019 HM Revenue & Customs
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

package agent.controllers

import java.net.URLEncoder
import javax.inject.{Inject, Singleton}

import agent.views.html.feedback.feedback_thankyou
import core.config.AppConfig
import play.api.Logger
import play.api.http.{Status => HttpStatus}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, Request, RequestHeader}
import play.twirl.api.Html
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.crypto.PlainText
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.bootstrap.controller.{FrontendController, UnauthorisedAction}
import uk.gov.hmrc.play.bootstrap.filters.frontend.crypto.SessionCookieCrypto
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.play.partials._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class FeedbackController @Inject()(implicit val applicationConfig: AppConfig,
                                   protected val authConnector: AuthConnector,
                                   val sessionCookieCrypto: SessionCookieCrypto,
                                   val http: HttpClient,
                                   val messagesApi: MessagesApi
                                  ) extends FrontendController with PartialRetriever with I18nSupport {
  private val TICKET_ID = "ticketId"

  override val httpGet = http

  implicit val cachedStaticHtmlPartialRetriever: CachedStaticHtmlPartialRetriever = new CachedStaticHtmlPartialRetriever {
    override val httpGet: HttpGet = http
  }

  implicit val formPartialRetriever: FormPartialRetriever = new FormPartialRetriever {
    override def httpGet: HttpGet = http

    override def crypto: (String) => String = cookie => sessionCookieCrypto.crypto.encrypt(PlainText(cookie)).value
  }

  def contactFormReferer(implicit request: Request[AnyContent]): String = request.headers.get(REFERER).getOrElse("")

  def localSubmitUrl(implicit request: Request[AnyContent]): String = routes.FeedbackController.submit().url

  protected def loadPartial(url: String)(implicit request: RequestHeader): HtmlPartial= ???

  private def feedbackFormPartialUrl(implicit request: Request[AnyContent]) =
    s"${applicationConfig.contactFrontendPartialBaseUrl}/contact/beta-feedback/form/?submitUrl=${urlEncode(localSubmitUrl)}" +
      s"&service=${urlEncode(applicationConfig.contactFormServiceIdentifier)}&referer=${urlEncode(contactFormReferer)}"

  private def feedbackHmrcSubmitPartialUrl(implicit request: Request[AnyContent]) =
    s"${applicationConfig.contactFrontendPartialBaseUrl}/contact/beta-feedback/form?resubmitUrl=${urlEncode(localSubmitUrl)}"

  private def feedbackThankYouPartialUrl(ticketId: String)(implicit request: Request[AnyContent]) =
    s"${applicationConfig.contactFrontendPartialBaseUrl}/contact/beta-feedback/form/confirmation?ticketId=${urlEncode(ticketId)}"

  def show: Action[AnyContent] = UnauthorisedAction {
    implicit request =>
      (request.session.get(REFERER), request.headers.get(REFERER)) match {
        case (None, Some(ref)) => Ok(agent.views.html.feedback.feedback(feedbackFormPartialUrl, None)).withSession(request.session + (REFERER -> ref))
        case _ => Ok(agent.views.html.feedback.feedback(feedbackFormPartialUrl, None))
      }
  }

  def submit: Action[AnyContent] = UnauthorisedAction.async {
    implicit request =>
      request.body.asFormUrlEncoded.map { formData =>
        http.POSTForm[HttpResponse](feedbackHmrcSubmitPartialUrl, formData)(rds = readPartialsForm, hc = partialsReadyHeaderCarrier, implicitly[ExecutionContext]).map {
          resp =>
            resp.status match {
              case HttpStatus.OK => Redirect(routes.FeedbackController.thankyou()).withSession(request.session + (TICKET_ID -> resp.body))
              case HttpStatus.BAD_REQUEST => BadRequest(agent.views.html.feedback.feedback(feedbackFormPartialUrl, Some(Html(resp.body))))
              case status => Logger.error(s"Unexpected status code from feedback form: $status")
                throw new InternalServerException("feedback controller, submit call failed")
            }
        }
      }.getOrElse {
        Logger.error("Trying to submit an empty feedback form")
        Future.failed(new InternalServerException("feedback controller, tried to submit empty feedback form"))
      }
  }

  def thankyou: Action[AnyContent] = UnauthorisedAction {
    implicit request =>
      val ticketId = request.session.get(TICKET_ID).getOrElse("N/A")
      val referer = request.session.get(REFERER).getOrElse("/")
      Ok(feedback_thankyou(feedbackThankYouPartialUrl(ticketId), referer)).withSession(request.session - REFERER)
  }

  private def urlEncode(value: String) = URLEncoder.encode(value, "UTF-8")

  private def partialsReadyHeaderCarrier(implicit request: Request[_]): HeaderCarrier = {
    val hc1 = PlaHeaderCarrierForPartialsConverter.headerCarrierEncryptingSessionCookieFromRequest(request)
    PlaHeaderCarrierForPartialsConverter.headerCarrierForPartialsToHeaderCarrier(hc1)
  }

  object PlaHeaderCarrierForPartialsConverter extends HeaderCarrierForPartialsConverter {
    override val crypto = encryptCookieString _

    def encryptCookieString(cookie: String): String = {
      sessionCookieCrypto.crypto.encrypt(PlainText(cookie)).value
    }
  }

  implicit val readPartialsForm: HttpReads[HttpResponse] = new HttpReads[HttpResponse] {
    def read(method: String, url: String, response: HttpResponse): HttpResponse = response
  }
}
