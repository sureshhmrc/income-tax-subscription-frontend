/*
 * Copyright 2017 HM Revenue & Customs
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

import javax.inject.{Inject, Singleton}

import agent.audit.Logging
import agent.auth.{AuthenticatedController, IncomeTaxAgentUser}
import agent.services.{ClientRelationshipService, KeystoreService, SubscriptionOrchestrationService}
import core.config.BaseControllerConfig
import core.services.AuthService
import incometax.subscription.models.SubscriptionSuccess
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, Request, Result}
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}

import scala.concurrent.Future

@Singleton
class CheckYourAnswersController @Inject()(val baseConfig: BaseControllerConfig,
                                           val messagesApi: MessagesApi,
                                           val keystoreService: KeystoreService,
                                           val subscriptionService: SubscriptionOrchestrationService,
                                           val clientRelationshipService: ClientRelationshipService,
                                           val authService: AuthService,
                                           logging: Logging
                                          ) extends AuthenticatedController {

  import agent.services.CacheUtil._

  private def journeySafeGuard(processFunc: => IncomeTaxAgentUser => Request[AnyContent] => CacheMap => Future[Result])
                              (noCacheMapErrMessage: String) =
    Authenticated.async { implicit request =>
      implicit user =>
        keystoreService.fetchAll().flatMap {
          case Some(cache) =>
            (user.clientNino, cache.getTerms()) match {
              case (None, _) => Future.successful(Redirect(agent.controllers.matching.routes.ConfirmClientController.show()))
              case (_, Some(true)) => processFunc(user)(request)(cache)
              case (_, Some(false)) => Future.successful(Redirect(agent.controllers.routes.TermsController.showTerms(editMode = true)))
              case _ => Future.successful(Redirect(agent.controllers.routes.TermsController.showTerms()))
            }
          case _ => error(noCacheMapErrMessage)
        }
    }

  lazy val backUrl: String = agent.controllers.routes.TermsController.showTerms().url

  val show: Action[AnyContent] = journeySafeGuard { implicit user =>
    implicit request =>
      cache =>
        Future.successful(
          Ok(agent.views.html.check_your_answers(
            cache.getSummary,
            agent.controllers.routes.CheckYourAnswersController.submit(),
            backUrl = backUrl
          ))
        )
  }(noCacheMapErrMessage = "User attempted to view 'Check Your Answers' without any keystore cached data")

  val submit: Action[AnyContent] = journeySafeGuard { implicit user =>
    implicit request =>
      cache =>
        // Will fail if there is no NINO
        val nino = user.clientNino.get

        // Will fail if there is no ARN in session
        val arn = user.arn.get
        val headerCarrier = implicitly[HeaderCarrier].withExtraHeaders(ITSASessionKeys.RequestURI -> request.uri)

        for {
          mtditid <- subscriptionService.createSubscription(arn, nino, cache.getSummary())(headerCarrier)
            .collect { case Right(SubscriptionSuccess(id)) => id }
            .recoverWith { case _ => error("Successful response not received from submission") }
          cacheMap <- keystoreService.saveSubscriptionId(mtditid)
            .recoverWith { case _ => error("Failed to save to keystore") }
        } yield Redirect(agent.controllers.routes.ConfirmationController.showConfirmation()).addingToSession(ITSASessionKeys.MTDITID -> mtditid)
  }(noCacheMapErrMessage = "User attempted to submit 'Check Your Answers' without any keystore cached data")

  def error(message: String): Future[Nothing] = {
    logging.warn(message)
    Future.failed(new InternalServerException(message))
  }

}
