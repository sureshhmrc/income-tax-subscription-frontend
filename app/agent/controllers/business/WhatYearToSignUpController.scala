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

package agent.controllers.business

import agent.auth.AuthenticatedController
import agent.forms.AccountingYearForm
import agent.models.AccountingYearModel
import agent.services.KeystoreService
import core.config.featureswitch.FeatureSwitching
import core.config.{AppConfig, BaseControllerConfig}
import core.services.{AccountingPeriodService, AuthService}
import javax.inject.Inject
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, Request}
import play.twirl.api.Html

import scala.concurrent.Future

class WhatYearToSignUpController @Inject()(val baseConfig: BaseControllerConfig,
                                           val messagesApi: MessagesApi,
                                           val keystoreService: KeystoreService,
                                           val authService: AuthService,
                                           val appConfig: AppConfig,
                                           val accountingPeriodService: AccountingPeriodService
                                          ) extends AuthenticatedController with FeatureSwitching {

  def backUrl(isEditMode: Boolean): String =
    if (isEditMode)
      agent.controllers.routes.CheckYourAnswersController.show().url
    else
      agent.controllers.business.routes.MatchTaxYearController.show().url

  def view(accountingYearForm: Form[AccountingYearModel], isEditMode: Boolean)(implicit request: Request[_]): Html = {
    agent.views.html.business.what_year_to_sign_up(
      accountingYearForm = accountingYearForm,
      postAction = agent.controllers.business.routes.WhatYearToSignUpController.submit(editMode = isEditMode),
      backUrl = backUrl(isEditMode),
      endYearOfCurrentTaxPeriod = accountingPeriodService.currentTaxYear,
      isEditMode = isEditMode
    )
  }

  def show(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      keystoreService.fetchWhatYearToSignUp() map { accountingYear =>
        Ok(view(accountingYearForm = AccountingYearForm.accountingYearForm.fill(accountingYear),
          isEditMode = isEditMode))
      }
  }

  def submit(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      AccountingYearForm.accountingYearForm.bindFromRequest.fold(
        formWithErrors =>
          Future.successful(BadRequest(view(accountingYearForm = formWithErrors, isEditMode = isEditMode))),
        accountingYear => {
          Future.successful(keystoreService.saveWhatYearToSignUp(accountingYear)) map { _ =>
            if (isEditMode) {
              Redirect(agent.controllers.routes.CheckYourAnswersController.show())
            }
            else {
              Redirect(agent.controllers.business.routes.BusinessAccountingMethodController.show())
            }
          }
        }
      )
  }
}