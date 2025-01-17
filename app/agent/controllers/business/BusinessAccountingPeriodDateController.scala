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
import agent.forms._
import agent.models.enums._
import agent.services.CacheUtil._
import agent.services.KeystoreService
import core.config.BaseControllerConfig
import core.config.featureswitch.{AgentTaxYear, FeatureSwitching}
import core.models.{No, Yes}
import core.services.{AccountingPeriodService, AuthService}
import core.utils.Implicits._
import incometax.business.models.AccountingPeriodModel
import incometax.subscription.models.IncomeSourceType
import incometax.util.{AccountingPeriodUtil, CurrentDateProvider}
import javax.inject.{Inject, Singleton}
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, Request}
import play.twirl.api.Html
import uk.gov.hmrc.http.InternalServerException

import scala.concurrent.Future

@Singleton
class BusinessAccountingPeriodDateController @Inject()(val baseConfig: BaseControllerConfig,
                                                       val messagesApi: MessagesApi,
                                                       val keystoreService: KeystoreService,
                                                       val authService: AuthService,
                                                       val accountingPeriodService: AccountingPeriodService,
                                                       val currentDateProvider: CurrentDateProvider
                                                      ) extends AuthenticatedController with FeatureSwitching {

  def view(form: Form[AccountingPeriodModel],
           backUrl: String, isEditMode: Boolean,
           viewType: AccountingPeriodViewType
          )(implicit request: Request[_]): Html =
    agent.views.html.business.accounting_period_date(
      form,
      agent.controllers.business.routes.BusinessAccountingPeriodDateController.submit(editMode = isEditMode),
      isEditMode,
      backUrl,
      viewType,
      AccountingPeriodUtil.getCurrentTaxYear.taxEndYear
    )

  def show(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      for {
        accountingPeriod <- keystoreService.fetchAccountingPeriodDate()
        backUrl <- backUrl(isEditMode)
        viewType <- whichView
      } yield
        Ok(view(
          AccountingPeriodDateForm.accountingPeriodDateForm.fill(accountingPeriod),
          backUrl = backUrl,
          isEditMode = isEditMode,
          viewType = viewType
        ))
  }

  def submit(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user => {
      whichView.flatMap {
        viewType =>
          AccountingPeriodDateForm.accountingPeriodDateForm.bindFromRequest().fold(
            formWithErrors => backUrl(isEditMode).map(backUrl => BadRequest(view(
              form = formWithErrors,
              backUrl = backUrl,
              isEditMode = isEditMode,
              viewType = viewType
            ))),
            accountingPeriod =>
              if (accountingPeriodService.checkEligibleAccountingPeriod(accountingPeriod.startDate.toLocalDate, accountingPeriod.endDate.toLocalDate)) {
                for {
                  cache <- keystoreService.fetchAll() map (_.get)
                  optOldAccountingPeriodDates = cache.getAccountingPeriodDate()
                  _ = cache.getIncomeSource() map (source => IncomeSourceType(source.source))
                  _ <- keystoreService.saveAccountingPeriodDate(accountingPeriod)
                  taxYearChanged = optOldAccountingPeriodDates match {
                    case Some(oldAccountingPeriod) => (oldAccountingPeriod.taxEndYear != accountingPeriod.taxEndYear)
                    case None => true
                  }
                  _ <- if (taxYearChanged) keystoreService.saveTerms(terms = false)
                  else Future.successful(Unit)
                } yield {
                  if (isEditMode) {
                    if (taxYearChanged) {
                      Redirect(agent.controllers.routes.TermsController.show(editMode = true))
                    } else {
                      Redirect(agent.controllers.routes.CheckYourAnswersController.show())
                    }
                  } else {
                    Redirect(agent.controllers.business.routes.BusinessAccountingMethodController.show())
                  }
                }
              } else {
                Redirect(agent.controllers.eligibility.routes.NotEligibleForIncomeTaxController.show())
              }
          )
      }
    }
  }

  def whichView(implicit request: Request[_]): Future[AccountingPeriodViewType] = {

    keystoreService.fetchAccountingPeriodPrior().flatMap {
      case Some(currentPeriodPrior) =>
        currentPeriodPrior.currentPeriodIsPrior match {
          case Yes =>
            NextAccountingPeriodView
          case No =>
            CurrentAccountingPeriodView
        }
      case _ => new InternalServerException(s"Internal Server Error - No Accounting Period Prior answer retrieved from keystore")
    }
  }

  def backUrl(isEditMode: Boolean)(implicit request: Request[_]): Future[String] = {

    if (isEditMode) {
      agent.controllers.routes.CheckYourAnswersController.show().url
    } else {
      if (isEnabled(AgentTaxYear)) {
        agent.controllers.business.routes.MatchTaxYearController.show().url
      }
      else {
        keystoreService.fetchAccountingPeriodPrior() flatMap {
          case Some(currentPeriodPrior) => currentPeriodPrior.currentPeriodIsPrior match {
            case Yes =>
              agent.controllers.business.routes.RegisterNextAccountingPeriodController.show().url
            case No =>
              agent.controllers.business.routes.BusinessAccountingPeriodPriorController.show().url
          }
          case _ => new InternalServerException(s"Internal Server Error - No Accounting Period Prior answer retrieved from keystore")
        }
      }
    }
  }
}

