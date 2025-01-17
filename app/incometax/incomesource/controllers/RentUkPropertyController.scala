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

package incometax.incomesource.controllers

import core.auth.SignUpController
import core.config.{AppConfig, BaseControllerConfig}
import core.models.{No, Yes}
import core.services.{AuthService, KeystoreService}
import incometax.incomesource.forms.RentUkPropertyForm._
import incometax.incomesource.models.RentUkPropertyModel
import incometax.incomesource.services.CurrentTimeService
import javax.inject.{Inject, Singleton}
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, Request, Result}
import play.twirl.api.Html

import scala.concurrent.Future

@Singleton
class RentUkPropertyController @Inject()(val baseConfig: BaseControllerConfig,
                                         val messagesApi: MessagesApi,
                                         val keystoreService: KeystoreService,
                                         val authService: AuthService,
                                         val appConfig: AppConfig,
                                         val currentTimeService: CurrentTimeService
                                        ) extends SignUpController {

  def show(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      keystoreService.fetchRentUkProperty() map {
        case Some(rentUkPropertyModel) => Ok(view(rentUkPropertyForm =
          rentUkPropertyForm.fill(rentUkPropertyModel), isEditMode = isEditMode))
        case None => Ok(view(rentUkPropertyForm = rentUkPropertyForm, isEditMode = isEditMode))
      }
  }

  def view(rentUkPropertyForm: Form[RentUkPropertyModel], isEditMode: Boolean)(implicit request: Request[_]): Html =
    incometax.incomesource.views.html.rent_uk_property(
      rentUkPropertyForm = rentUkPropertyForm,
      postAction = incometax.incomesource.controllers.routes.RentUkPropertyController.submit(editMode = isEditMode),
      isEditMode = isEditMode,
      backUrl = backUrl
    )

  def submit(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      rentUkPropertyForm.bindFromRequest.fold(
        formWithErrors =>
          Future.successful(BadRequest(view(
            rentUkPropertyForm = formWithErrors,
            isEditMode = isEditMode
          ))),
        data => {
          lazy val linearJourney: Future[Result] =
            keystoreService.saveRentUkProperty(data) flatMap { _ =>
              (data.rentUkProperty, data.onlySourceOfSelfEmployedIncome) match {
                case (No, _) =>
                  Future.successful(Redirect(incometax.incomesource.controllers.routes.AreYouSelfEmployedController.show()))
                case (Yes, Some(No)) =>
                  Future.successful(Redirect(incometax.incomesource.controllers.routes.AreYouSelfEmployedController.show()))
                case (Yes, Some(Yes)) if appConfig.eligibilityPagesEnabled =>
                  if(appConfig.propertyCashOrAccrualsEnabled) {
                    Future.successful(Redirect(incometax.business.controllers.routes.PropertyAccountingMethodController.show()))
                  } else {
                    Future.successful(Redirect(incometax.subscription.controllers.routes.CheckYourAnswersController.show()))
                  }
                case _ =>
                  Future.successful(Redirect(incometax.incomesource.controllers.routes.OtherIncomeController.show()))
              }
            }

          if (!isEditMode)
            linearJourney
          else
            (for {
              rentUkPropertyModel <- keystoreService.fetchRentUkProperty()
            } yield {
              // if what was persisted is the same as the new value then go straight back to summary
              if (rentUkPropertyModel.fold(false)(i => i.rentUkProperty.equals(data.rentUkProperty)
                && i.onlySourceOfSelfEmployedIncome.equals(data.onlySourceOfSelfEmployedIncome)))
                Future.successful(Redirect(incometax.subscription.controllers.routes.CheckYourAnswersController.submit()))
              else // otherwise go back to the linear journey
                linearJourney
            }).flatMap(x => x)
        }
      )
  }

  lazy val backUrl: String =
    incometax.subscription.controllers.routes.CheckYourAnswersController.show().url
}
