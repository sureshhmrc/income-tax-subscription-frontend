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

package incometax.business.controllers

import javax.inject.{Inject, Singleton}
import core.auth.{Registration, SignUpController}
import core.config.BaseControllerConfig
import core.models.{No, Yes}
import core.services.{AuthService, CacheUtil, KeystoreService}
import incometax.business.forms.MatchTaxYearForm
import incometax.business.models.MatchTaxYearModel
import incometax.incomesource.services.CurrentTimeService
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc._
import play.twirl.api.Html
import core.services.CacheUtil._
import incometax.subscription.models.Business

import scala.concurrent.Future

@Singleton
class MatchTaxYearController @Inject()(val baseConfig: BaseControllerConfig,
                                       val messagesApi: MessagesApi,
                                       val keystoreService: KeystoreService,
                                       val authService: AuthService,
                                       val currentTimeService: CurrentTimeService
                                      ) extends SignUpController {

  def view(matchTaxYearForm: Form[MatchTaxYearModel], isEditMode: Boolean)(implicit request: Request[AnyContent]): Html =
    incometax.business.views.html.match_to_tax_year(
      matchTaxYearForm = matchTaxYearForm,
      postAction = incometax.business.controllers.routes.MatchTaxYearController.submit(editMode = isEditMode),
      isRegistration = request.isInState(Registration),
      backUrl = backUrl(isEditMode = isEditMode),
      isEditMode
    )

  def show(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      keystoreService.fetchMatchTaxYear() map {
        matchTaxYear => Ok(view(matchTaxYearForm = MatchTaxYearForm.matchTaxYearForm.fill(matchTaxYear), isEditMode = isEditMode))
      }
  }

  def submit(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      MatchTaxYearForm.matchTaxYearForm.bindFromRequest.fold(
        formWithErrors => Future.successful(BadRequest(view(matchTaxYearForm = formWithErrors, isEditMode = isEditMode))),
        matchTaxYear => {
          for {
            cacheMap <- keystoreService.fetchAll()
            oldAnswer = cacheMap.getMatchTaxYear()
            changedAnswer = oldAnswer.fold(false)(_ != matchTaxYear)
            _ <- if (changedAnswer) keystoreService.saveTerms(terms = false) else Future.successful(Unit)
            _ <- keystoreService.saveMatchTaxYear(matchTaxYear)
          } yield (isEditMode, matchTaxYear.matchTaxYear, cacheMap.getIncomeSourceType()) match {
            case (false, Yes, Some(Business)) if applicationConfig.whatTaxYearToSignUpEnabled =>
              Redirect(incometax.business.controllers.routes.WhatYearToSignUpController.show())
            case (false, Yes, _) =>
              Redirect(incometax.business.controllers.routes.BusinessAccountingMethodController.show())
            case (false, No, _) =>
              Redirect(incometax.business.controllers.routes.BusinessAccountingPeriodDateController.show())
            case (true, Yes, _) =>
              Redirect(incometax.subscription.controllers.routes.CheckYourAnswersController.show())
            case (true, No, _) =>
              Redirect(incometax.business.controllers.routes.BusinessAccountingPeriodDateController.show(editMode = true, editMatch = true))
          }
        }
      )
  }

  def backUrl(isEditMode: Boolean)(implicit request: Request[AnyContent]): String =
    if (isEditMode)
      incometax.subscription.controllers.routes.CheckYourAnswersController.show().url
    else if (request.isInState(Registration))
      incometax.business.controllers.routes.BusinessStartDateController.show().url
    else
      incometax.business.controllers.routes.BusinessNameController.show().url

}
