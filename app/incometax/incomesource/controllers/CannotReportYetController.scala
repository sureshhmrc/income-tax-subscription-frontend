/*
 * Copyright 2018 HM Revenue & Customs
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

package incometax.incomesource.controllers

import javax.inject.Inject

import core.audit.Logging
import core.auth.TaxYearDeferralController
import core.config.BaseControllerConfig
import core.services.CacheUtil._
import core.services.{AuthService, KeystoreService}
import incometax.business.forms.MatchTaxYearForm
import incometax.incomesource.forms.IncomeSourceForm
import incometax.incomesource.models.NewIncomeSourceModel
import incometax.subscription.models.{Both, Business, Property}
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.http.InternalServerException

import scala.concurrent.Future

class CannotReportYetController @Inject()(val baseConfig: BaseControllerConfig,
                                          val messagesApi: MessagesApi,
                                          val keystoreService: KeystoreService,
                                          val logging: Logging,
                                          val authService: AuthService
                                         ) extends TaxYearDeferralController {

  def show(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      if (applicationConfig.newIncomeSourceFlowEnabled) {
        for {
          cache <- keystoreService.fetchAll()
          newIncomeSource = cache.getNewIncomeSource().get
          matchTaxYear = cache.getMatchTaxYear().map(_.matchTaxYear == MatchTaxYearForm.option_yes)
        } yield
          Ok(incometax.incomesource.views.html.cannot_report_yet(
            postAction = routes.CannotReportYetController.submit(editMode = isEditMode),
            backUrl(newIncomeSource, matchTaxYear, isEditMode)
          ))
      }
      else {
        for {
          cache <- keystoreService.fetchAll()
          incomeSource = cache.getIncomeSource().get.source
          matchTaxYear = cache.getMatchTaxYear().map(_.matchTaxYear == MatchTaxYearForm.option_yes)
        } yield
          Ok(incometax.incomesource.views.html.cannot_report_yet(
            postAction = routes.CannotReportYetController.submit(editMode = isEditMode),
            backUrl(incomeSource, matchTaxYear, isEditMode)
          ))
      }
  }

  def submit(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      if (isEditMode) Future.successful(Redirect(incometax.subscription.controllers.routes.CheckYourAnswersController.show()))
      else if (applicationConfig.newIncomeSourceFlowEnabled) {
        for {
          cache <- keystoreService.fetchAll()
          newIncomeSource = cache.getNewIncomeSource().get.getIncomeSourceType
        } yield newIncomeSource match {
          case Right(Business | Both) =>
            Redirect(incometax.business.controllers.routes.BusinessAccountingMethodController.show())
          case Right(Property) =>
            Redirect(incometax.incomesource.controllers.routes.OtherIncomeController.show())
        }
      } else {
        keystoreService.fetchIncomeSource() map {
          case Some(incomeSource) => incomeSource.source match {
            case IncomeSourceForm.option_business | IncomeSourceForm.option_both =>
              Redirect(incometax.business.controllers.routes.BusinessAccountingMethodController.show())
            case IncomeSourceForm.option_property =>
              Redirect(incometax.incomesource.controllers.routes.OtherIncomeController.show())
          }
          case _ =>
            logging.info("Tried to submit 'other income error' when no data found in Keystore for 'income source'")
            throw new InternalServerException("Other Income Error Controller, call to submit 'other income error' when no 'income source'")
        }
      }
  }

  def backUrl(incomeSource: String, matchTaxYear: Option[Boolean], isEditMode: Boolean): String =
    (incomeSource, matchTaxYear) match {
      case (IncomeSourceForm.option_property, _) =>
        incometax.incomesource.controllers.routes.IncomeSourceController.show().url
      case (IncomeSourceForm.option_business | IncomeSourceForm.option_both, Some(true)) =>
        incometax.business.controllers.routes.MatchTaxYearController.show(editMode = isEditMode).url
      case (IncomeSourceForm.option_business | IncomeSourceForm.option_both, Some(false)) =>
        incometax.business.controllers.routes.BusinessAccountingPeriodDateController.show(editMode = isEditMode).url
    }

  def backUrl(newIncomeSource: NewIncomeSourceModel, matchTaxYear: Option[Boolean], isEditMode: Boolean): String =
    (newIncomeSource.getIncomeSourceType, matchTaxYear) match {
      case (Right(Property), _) =>
        incometax.incomesource.controllers.routes.WorkForYourselfController.show().url
      case (Right(Business | Both), Some(true)) =>
        incometax.business.controllers.routes.MatchTaxYearController.show(editMode = isEditMode).url
      case (Right(Business | Both), Some(false)) =>
        incometax.business.controllers.routes.BusinessAccountingPeriodDateController.show(editMode = isEditMode).url
    }
}