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
import incometax.business.forms.AccountingMethodPropertyForm
import incometax.business.models.AccountingMethodPropertyModel
import incometax.incomesource.models.{AreYouSelfEmployedModel, RentUkPropertyModel}
import incometax.incomesource.services.CurrentTimeService
import javax.inject.{Inject, Singleton}
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, Request}
import play.twirl.api.Html
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

@Singleton
class PropertyAccountingMethodController @Inject()(val baseConfig: BaseControllerConfig,
                                                           val messagesApi: MessagesApi,
                                                           val keystoreService: KeystoreService,
                                                           val authService: AuthService,
                                                           val appConfig: AppConfig,
                                                           val currentTimeService: CurrentTimeService
                                                  ) extends SignUpController {

  def view(accountingMethodPropertyForm: Form[AccountingMethodPropertyModel], isEditMode: Boolean)(implicit request: Request[_]): Future[Html] = {
    for {
      back <- backUrl(isEditMode)
    } yield
      incometax.business.views.html.property_accounting_method(
        accountingMethodForm = accountingMethodPropertyForm,
        postAction = incometax.business.controllers.routes.PropertyAccountingMethodController.submit(editMode = isEditMode),
        isEditMode,
        backUrl = back
      )
  }

  def show(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      keystoreService.fetchAccountingMethodProperty() flatMap { accountingMethodProperty
      => view(accountingMethodPropertyForm = AccountingMethodPropertyForm.accountingMethodPropertyForm
        .fill(accountingMethodProperty), isEditMode = isEditMode).map(view => Ok(view))
        }
      }

  def submit(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      AccountingMethodPropertyForm.accountingMethodPropertyForm.bindFromRequest.fold(
        formWithErrors =>
          view(accountingMethodPropertyForm = formWithErrors, isEditMode = isEditMode).map(view => BadRequest(view)),
        accountingMethodProperty => {
          keystoreService.saveAccountingMethodProperty(accountingMethodProperty) map { _ =>
            Redirect(incometax.subscription.controllers.routes.CheckYourAnswersController.show())
          }
        }
      )
  }

  def backUrl(isEditMode: Boolean)(implicit hc: HeaderCarrier): Future[String] =
    if (isEditMode)
      Future.successful(incometax.subscription.controllers.routes.CheckYourAnswersController.show().url)
    else {
      keystoreService.fetchAll() map {
        cacheMap =>
          (cacheMap.getRentUkProperty(), cacheMap.getAreYouSelfEmployed())

          match {
            case (Some(RentUkPropertyModel(Yes, Some(Yes))), _) =>
              incometax.incomesource.controllers.routes.RentUkPropertyController.show().url
            case (_, Some(AreYouSelfEmployedModel(Yes))) =>
              incometax.business.controllers.routes.BusinessAccountingMethodController.show().url
            case (_, Some(AreYouSelfEmployedModel(No))) =>
              incometax.incomesource.controllers.routes.AreYouSelfEmployedController.show().url
            case _ =>
              incometax.incomesource.controllers.routes.RentUkPropertyController.show().url

          }
      }
    }

}
