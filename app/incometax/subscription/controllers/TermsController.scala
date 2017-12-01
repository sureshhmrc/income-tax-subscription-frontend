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

package incometax.subscription.controllers

import java.time.LocalDate
import javax.inject.{Inject, Singleton}

import core.auth.SignUpController
import core.config.BaseControllerConfig
import core.services.{AuthService, KeystoreService}
import core.utils.Implicits._
import incometax.business.models.AccountingPeriodModel
import incometax.incomesource.forms.{IncomeSourceForm, OtherIncomeForm}
import incometax.incomesource.models.OtherIncomeModel
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, Request}
import play.twirl.api.Html
import uk.gov.hmrc.http.InternalServerException

import scala.concurrent.Future

@Singleton
class TermsController @Inject()(val baseConfig: BaseControllerConfig,
                                val messagesApi: MessagesApi,
                                val keystoreService: KeystoreService,
                                val authService: AuthService
                               ) extends SignUpController {

  def view(backUrl: String, taxEndYear: Int)(implicit request: Request[_]): Html =
    incometax.subscription.views.html.terms(
      postAction = incometax.subscription.controllers.routes.TermsController.submitTerms(),
      taxEndYear = taxEndYear,
      backUrl
    )

  def showTerms(): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      for {
        taxEndYear <- keystoreService.fetchAccountingPeriodDate()
          .map(dates => TermsController.getTaxEndYear(dates.get))
        backUrl <- backUrl
      } yield Ok(view(backUrl = backUrl, taxEndYear = taxEndYear))
  }

  def submitTerms(isEditMode: Boolean = false): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      keystoreService.saveTerms(terms = true) map (
        _ => Redirect(incometax.subscription.controllers.routes.CheckYourAnswersController.show()))
  }

  def backUrl(implicit request: Request[_]): Future[String] =
    keystoreService.fetchIncomeSource() flatMap {
      case Some(source) => source.source match {
        case IncomeSourceForm.option_business =>
          incometax.business.controllers.routes.BusinessAccountingMethodController.show().url
        case IncomeSourceForm.option_both =>
          incometax.business.controllers.routes.BusinessAccountingMethodController.show().url
        case IncomeSourceForm.option_property =>
          import OtherIncomeForm._
          keystoreService.fetchOtherIncome() flatMap {
            case Some(OtherIncomeModel(`option_yes`)) =>
              incometax.incomesource.controllers.routes.OtherIncomeErrorController.showOtherIncomeError().url
            case Some(OtherIncomeModel(`option_no`)) =>
              incometax.incomesource.controllers.routes.OtherIncomeController.showOtherIncome().url
            case _ => new InternalServerException(s"Internal Server Error - TermsController.backUrl, no other income answer")
          }
        case x => new InternalServerException(s"Internal Server Error - TermsController.backUrl, unexpected income source: '$x'")
      }
      case _ => new InternalServerException(s"Internal Server Error - TermsController.backUrl, no income source retrieve from Keystore")
    }

}

object TermsController {

  private val april = 4
  private val sixth = 6

  def getTaxEndYear(accountingPeriodModel: AccountingPeriodModel): Int = {
    val endDate = accountingPeriodModel.endDate.toLocalDate
    if (endDate.isBefore(LocalDate.of(endDate.getYear, april, sixth))) endDate.getYear
    else endDate.getYear + 1
  }

}