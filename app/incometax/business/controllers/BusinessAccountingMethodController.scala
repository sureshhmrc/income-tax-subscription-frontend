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

import core.auth.SignUpController
import core.config.{AppConfig, BaseControllerConfig}
import core.models.{No, Yes}
import core.services.CacheUtil.CacheMapUtil
import core.services.{AuthService, KeystoreService}
import incometax.business.forms.AccountingMethodForm
import incometax.business.models.{AccountingMethodModel, MatchTaxYearModel}
import incometax.incomesource.models.RentUkPropertyModel
import incometax.incomesource.services.CurrentTimeService
import incometax.subscription.models.Business
import javax.inject.{Inject, Singleton}
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, Request}
import play.twirl.api.Html
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

@Singleton
class BusinessAccountingMethodController @Inject()(val baseConfig: BaseControllerConfig,
                                                   val messagesApi: MessagesApi,
                                                   val keystoreService: KeystoreService,
                                                   val authService: AuthService,
                                                   val currentTimeService: CurrentTimeService
                                                  ) extends SignUpController {


  val appConfig: AppConfig = baseConfig.applicationConfig

  def view(accountingMethodForm: Form[AccountingMethodModel], isEditMode: Boolean)(implicit request: Request[_]): Future[Html] = {
    for {
      back <- backUrl(isEditMode)
    } yield
      incometax.business.views.html.accounting_method(
        accountingMethodForm = accountingMethodForm,
        postAction = incometax.business.controllers.routes.BusinessAccountingMethodController.submit(editMode = isEditMode),
        isEditMode,
        backUrl = back
      )
  }

  def show(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      keystoreService.fetchAccountingMethod() flatMap { accountingMethod =>
        view(accountingMethodForm = AccountingMethodForm.accountingMethodForm.fill(accountingMethod), isEditMode = isEditMode).map(view => Ok(view))
      }
  }

  def submit(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      AccountingMethodForm.accountingMethodForm.bindFromRequest.fold(
        formWithErrors => view(accountingMethodForm = formWithErrors, isEditMode = isEditMode).map(view => BadRequest(view)),
        accountingMethod => {
          keystoreService.saveAccountingMethod(accountingMethod) flatMap { _ =>
            if (isEditMode) {
              Future.successful(Redirect(incometax.subscription.controllers.routes.CheckYourAnswersController.show()))
            } else if (appConfig.eligibilityPagesEnabled) {
              keystoreService.fetchRentUkProperty() map {
                case Some(RentUkPropertyModel(Yes, _)) if appConfig.propertyCashOrAccrualsEnabled =>
                  Redirect(incometax.business.controllers.routes.PropertyAccountingMethodController.show())
                case _ =>
                  Redirect(incometax.subscription.controllers.routes.CheckYourAnswersController.show())
              }
            } else {
              Future.successful(Redirect(incometax.subscription.controllers.routes.TermsController.show()))
            }
          }
        }
      )
  }

  def backUrl(isEditMode: Boolean)(implicit hc: HeaderCarrier): Future[String] =
    if (isEditMode)
      Future.successful(incometax.subscription.controllers.routes.CheckYourAnswersController.show().url)
    else {
      keystoreService.fetchAll() map { cacheMap =>
        (cacheMap.getIncomeSourceType(), cacheMap.getMatchTaxYear()) match {
          case (_, Some(MatchTaxYearModel(No))) =>
          incometax.business.controllers.routes.BusinessAccountingPeriodDateController.show().url
          case (Some(Business), _) if appConfig.whatTaxYearToSignUpEnabled =>
            incometax.business.controllers.routes.WhatYearToSignUpController.show().url
          case (_, Some(MatchTaxYearModel(Yes))) =>
            incometax.business.controllers.routes.MatchTaxYearController.show().url
        }
      }
    }


}
