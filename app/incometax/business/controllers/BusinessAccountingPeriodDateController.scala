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
import core.services.{AccountingPeriodService, AuthService, KeystoreService}
import incometax.business.forms.AccountingPeriodDateForm
import incometax.business.models.AccountingPeriodModel
import incometax.business.models.enums._
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, Request}
import play.twirl.api.Html
import core.services.CacheUtil._
import incometax.incomesource.services.CurrentTimeService
import incometax.subscription.models.{Both, IncomeSourceType}

import scala.concurrent.Future

@Singleton
class BusinessAccountingPeriodDateController @Inject()(val baseConfig: BaseControllerConfig,
                                                       val messagesApi: MessagesApi,
                                                       val keystoreService: KeystoreService,
                                                       val authService: AuthService,
                                                       val currentTimeService: CurrentTimeService,
                                                       val accountingPeriodService: AccountingPeriodService
                                                      ) extends SignUpController {

  def view(form: Form[AccountingPeriodModel], backUrl: String, isEditMode: Boolean, editMatch: Boolean)(implicit request: Request[_]): Html =
    incometax.business.views.html.accounting_period_date(
      accountingPeriodForm = form,
      postAction = incometax.business.controllers.routes.BusinessAccountingPeriodDateController.submit(editMode = isEditMode, editMatch = editMatch),
      viewType = whichView,
      isEditMode = isEditMode,
      backUrl = backUrl
    )

  def show(isEditMode: Boolean, editMatch: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      for {
        accountingPeriod <- keystoreService.fetchAccountingPeriodDate()
      } yield
        Ok(view(
          AccountingPeriodDateForm.accountingPeriodDateForm.fill(accountingPeriod),
          backUrl = backUrl(isEditMode, editMatch),
          isEditMode = isEditMode,
          editMatch = editMatch
        ))
  }

  def submit(isEditMode: Boolean, editMatch: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      AccountingPeriodDateForm.accountingPeriodDateForm.bindFromRequest().fold(
        formWithErrors => Future.successful(BadRequest(view(
          form = formWithErrors,
          backUrl = backUrl(isEditMode, editMatch),
          isEditMode = isEditMode,
          editMatch = editMatch
        ))),
        accountingPeriod => {
          if(accountingPeriodService.checkEligibleAccountingPeriod(
            accountingPeriod.startDate.toLocalDate,
            accountingPeriod.endDate.toLocalDate)) {

            for {
              cache <- keystoreService.fetchAll()
              optOldAccountingPeriodDates = cache.getAccountingPeriodDate()
              _ <- keystoreService.saveAccountingPeriodDate(accountingPeriod)
              enteredTaxEndYear = accountingPeriod.taxEndYear
              _ <- optOldAccountingPeriodDates match {
                case Some(oldAccountingPeriodDates) if oldAccountingPeriodDates.taxEndYear != enteredTaxEndYear =>
                  keystoreService.saveTerms(terms = false)
                case _ => Future.successful(Unit)
              }
            } yield
              if (isEditMode) Redirect(incometax.subscription.controllers.routes.CheckYourAnswersController.show())
              else Redirect(incometax.business.controllers.routes.BusinessAccountingMethodController.show())
          } else {
            Future.successful(Redirect(incometax.eligibility.controllers.routes.NotEligibleForIncomeTaxController.show()))
          }
        }
      )
  }

  private def hasBothIncomeSources(incomeSourceType: IncomeSourceType): Boolean =
    incomeSourceType match {
      case Both => true
      case _ => false
    }


  def whichView(implicit request: Request[_]): AccountingPeriodViewType =
    if (request.isInState(Registration)) RegistrationAccountingPeriodView
    else SignUpAccountingPeriodView

  def backUrl(isEditMode: Boolean, editMatch: Boolean)(implicit request: Request[_]): String =
    if (isEditMode) {
      if (editMatch) incometax.business.controllers.routes.MatchTaxYearController.show(editMode = isEditMode).url
      else incometax.subscription.controllers.routes.CheckYourAnswersController.show().url
    }
    else
      incometax.business.controllers.routes.MatchTaxYearController.show(editMode = isEditMode).url

}
