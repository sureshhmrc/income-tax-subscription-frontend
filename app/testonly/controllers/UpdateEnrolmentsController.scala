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

package testonly.controllers

import core.auth.BaseFrontendController
import core.config.AppConfig
import core.services.AuthService
import javax.inject.Inject
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import testonly.connectors.EnrolmentStoreStubConnector
import testonly.forms.UpdateEnrolmentsForm
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.auth.core.retrieve.{Credentials, Retrievals}
import uk.gov.hmrc.auth.otac.Authorised
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import scala.concurrent.Future


class UpdateEnrolmentsController @Inject()(implicit val applicationConfig: AppConfig,
                                           mcc: MessagesControllerComponents,
                                           enrolmentStoreStubConnector: EnrolmentStoreStubConnector,
                                           authService: AuthService
                                          ) extends FrontendController(mcc) with I18nSupport {

  import authService._

  def show: Action[AnyContent] = Action.async(implicit req =>
    authorised().retrieve(Retrievals.credentials) {
      case Credentials(credId, _) =>
        Future.successful(Ok(testonly.views.html.update_enrolments(
          UpdateEnrolmentsForm.updateEnrolmentsForm.fill(credId),
          testonly.controllers.routes.UpdateEnrolmentsController.submit()
        )))
    }
  )


  def submit: Action[AnyContent] = Action.async(implicit req =>
    UpdateEnrolmentsForm.updateEnrolmentsForm.bindFromRequest.fold(
      formWithErrors =>
        Future.successful(BadRequest(testonly.views.html.update_enrolments(
          formWithErrors,
          testonly.controllers.routes.UpdateEnrolmentsController.submit()
        ))),
      credentialId => for {
        _ <- enrolmentStoreStubConnector.updateEnrolments(credentialId)
      } yield Ok("Enrolment updated")
    )

  )
}
