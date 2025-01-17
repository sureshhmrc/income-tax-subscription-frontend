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

package incometax.subscription.controllers

import core.ITSASessionKeys
import core.audit.Logging
import core.auth.{IncomeTaxSAUser, Registration, SignUpController}
import core.config.{AppConfig, BaseControllerConfig}
import core.models.{No, Yes}
import core.services.{AuthService, KeystoreService}
import incometax.business.models.MatchTaxYearModel
import incometax.subscription.models._
import incometax.subscription.services.SubscriptionOrchestrationService
import javax.inject.{Inject, Singleton}
import play.api.i18n.MessagesApi
import play.api.mvc.{AnyContent, Request, Result}
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}

import scala.concurrent.Future

@Singleton
class CheckYourAnswersController @Inject()(val baseConfig: BaseControllerConfig,
                                           val messagesApi: MessagesApi,
                                           val keystoreService: KeystoreService,
                                           val subscriptionService: SubscriptionOrchestrationService,
                                           val authService: AuthService,
                                           val appConfig: AppConfig,
                                           logging: Logging
                                          ) extends SignUpController {

  import core.services.CacheUtil._


  def backUrl(incomeSource: IncomeSourceType): String = {
    if (appConfig.eligibilityPagesEnabled) {
      if(appConfig.propertyCashOrAccrualsEnabled) {
        incomeSource match {
          case Property | Both =>
            incometax.business.controllers.routes.PropertyAccountingMethodController.show().url
          case Business =>
            incometax.business.controllers.routes.BusinessAccountingMethodController.show().url
        }
      } else {
        incomeSource match {
          case Business | Both =>
            incometax.business.controllers.routes.BusinessAccountingMethodController.show().url
          case Property =>
            incometax.incomesource.controllers.routes.AreYouSelfEmployedController.show().url
        }
      }
    } else {
      incometax.subscription.controllers.routes.TermsController.show().url
    }
  }

  val show = journeySafeGuard { implicit user =>
    implicit request =>
      cache =>
        Future.successful(
          Ok(incometax.subscription.views.html.check_your_answers(
            cache.getSummary(),
            isRegistration = request.isInState(Registration),
            incometax.subscription.controllers.routes.CheckYourAnswersController.submit(),
            backUrl = backUrl(cache.getIncomeSourceType().get)
          ))
        )
  }(noCacheMapErrMessage = "User attempted to view 'Check Your Answers' without any keystore cached data")

  val submit = journeySafeGuard { implicit user =>
    implicit request =>
      cache =>
        val nino = user.nino.get
        val headerCarrier = implicitly[HeaderCarrier].withExtraHeaders(ITSASessionKeys.RequestURI -> request.uri)

        subscriptionService.createSubscription(nino, cache.getSummary())(headerCarrier).flatMap {
          case Right(SubscriptionSuccess(id)) =>
            keystoreService.saveSubscriptionId(id).map(_ => Redirect(incometax.subscription.controllers.routes.ConfirmationController.show()))
          case Left(failure) =>
            error("Successful response not received from submission: \n" + failure.toString)
        }
  }(noCacheMapErrMessage = "User attempted to submit 'Check Your Answers' without any keystore cached data")

  private def journeySafeGuard(processFunc: IncomeTaxSAUser => Request[AnyContent] => CacheMap => Future[Result])
                              (noCacheMapErrMessage: String) =
    Authenticated.async { implicit request =>
      implicit user =>
        keystoreService.fetchAll().flatMap { cache =>
          if (appConfig.eligibilityPagesEnabled) {
            val isProperty = cache.getIncomeSourceType().contains(Property)
            if (isProperty)
              processFunc(user)(request)(cache)
            else
              (cache.getMatchTaxYear(), cache.getEnteredAccountingPeriodDate()) match {
                case (Some(MatchTaxYearModel(Yes)), _) | (Some(MatchTaxYearModel(No)), Some(_)) =>
                  processFunc(user)(request)(cache)
                case (Some(MatchTaxYearModel(No)), _) =>
                  Future.successful(Redirect(incometax.business.controllers.routes.BusinessAccountingPeriodDateController.show(editMode = true, editMatch = true)))
              }
          } else {
            cache.getTerms match {
              case Some(true) =>
                val isProperty = cache.getIncomeSourceType().contains(Property)
                if (isProperty)
                  processFunc(user)(request)(cache)
                else
                  (cache.getMatchTaxYear(), cache.getEnteredAccountingPeriodDate()) match {
                    case (Some(MatchTaxYearModel(Yes)), _) | (Some(MatchTaxYearModel(No)), Some(_)) =>
                      processFunc(user)(request)(cache)
                    case (Some(MatchTaxYearModel(No)), _) =>
                      Future.successful(Redirect(incometax.business.controllers.routes.BusinessAccountingPeriodDateController.show(editMode = true, editMatch = true)))
                  }
              case Some(false) => Future.successful(Redirect(incometax.subscription.controllers.routes.TermsController.show(editMode = true)))
              case _ => Future.successful(Redirect(incometax.subscription.controllers.routes.TermsController.show()))
            }
          }
        }
    }

  def error(message: String): Future[Nothing] = {
    logging.warn(message)
    Future.failed(new InternalServerException(message))
  }
}
