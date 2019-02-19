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

import javax.inject.{Inject, Singleton}
import agent.audit.Logging
import agent.auth.AuthenticatedController
import agent.forms.{IncomeSourceForm, OtherIncomeForm}
import agent.services.KeystoreService
import core.config.BaseControllerConfig
import core.models.{No, Yes, YesNo}
import core.services.AuthService
import core.utils.Implicits._
import incometax.incomesource.services.CurrentTimeService
import incometax.subscription.models.IncomeSourceType
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc._
import play.twirl.api.Html
import uk.gov.hmrc.http.InternalServerException

import scala.concurrent.Future

@Singleton
class OtherIncomeController @Inject()(val baseConfig: BaseControllerConfig,
                                      mcc: MessagesControllerComponents,
                                      val keystoreService: KeystoreService,
                                      val authService: AuthService,
                                      val logging: Logging,
                                      currentTimeService: CurrentTimeService
                                     ) extends AuthenticatedController(mcc) {

  def view(otherIncomeForm: Form[YesNo],
           incomeSource: String,
           isEditMode: Boolean,
           backUrl: String
          )(implicit request: Request[_]): Html =
    agent.views.html.other_income(
      otherIncomeForm = otherIncomeForm,
      incomeSource = incomeSource,
      postAction = agent.controllers.routes.OtherIncomeController.submit(editMode = isEditMode),
      isEditMode = isEditMode,
      backUrl = backUrl
    )

  def show(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      for {
        optIncomeSource <- keystoreService.fetchIncomeSource()
        choice <- if (optIncomeSource.isDefined) keystoreService.fetchOtherIncome() else Future.successful(None)
      } yield (optIncomeSource, choice) match {
        case (Some(incomeSource), _) =>
          Ok(view(
            OtherIncomeForm.otherIncomeForm.fill(choice),
            incomeSource.source,
            isEditMode,
            backUrl(
              isEditMode,
              incomeSource
            )
          ))
        case _ =>
          Redirect(agent.controllers.routes.IncomeSourceController.show())
      }
  }

  def defaultRedirections(optIncomeSource: Option[IncomeSourceType], otherIncome: YesNo)(implicit request: Request[_]): Future[Result] =
    otherIncome match {
      case Yes =>
        Redirect(agent.controllers.routes.OtherIncomeErrorController.show())
      case No =>
        optIncomeSource match {
          case Some(incomeSource) => incomeSource.source match {
            case IncomeSourceForm.option_business =>
              Redirect(agent.controllers.business.routes.BusinessAccountingPeriodPriorController.show())
            case IncomeSourceForm.option_property =>
              Redirect(agent.controllers.routes.TermsController.show())
            case IncomeSourceForm.option_both =>
              Redirect(agent.controllers.business.routes.BusinessAccountingPeriodPriorController.show())
          }
          case _ =>
            logging.info("Tried to submit other income when no data found in Keystore for income source")
            new InternalServerException("Other Income, tried to submit with no income source")
        }
    }

  def submit(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      keystoreService.fetchIncomeSource().flatMap {
        case optIncomeSource@Some(incomeSource) =>
          OtherIncomeForm.otherIncomeForm.bindFromRequest.fold(
            formWithErrors =>
              Future.successful(
                BadRequest(
                  view(
                    otherIncomeForm = formWithErrors,
                    incomeSource = incomeSource.source,
                    isEditMode = isEditMode,
                    backUrl = backUrl(
                      isEditMode,
                      incomeSource
                    )
                  )
                )
              ),
            choice =>
              keystoreService.fetchOtherIncome().flatMap {
                previousOtherIncome =>
                  keystoreService.saveOtherIncome(choice).flatMap { _ =>
                    // if it's in update mode and the previous answer is the same as current then return to check your answers page
                    if (isEditMode && previousOtherIncome.fold(false)(old => old.equals(choice)))
                      Future.successful(Redirect(agent.controllers.routes.CheckYourAnswersController.show()))
                    else defaultRedirections(optIncomeSource, choice)
                  }
              }
          )
        case _ =>
          Future.successful(Redirect(agent.controllers.routes.IncomeSourceController.show()))
      }
  }

  def backUrl(isEditMode: Boolean, incomeSource: IncomeSourceType): String =
    if (isEditMode) {
      agent.controllers.routes.CheckYourAnswersController.show().url
    } else {
      agent.controllers.routes.IncomeSourceController.show().url
    }

}
