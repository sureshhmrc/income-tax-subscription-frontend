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

package identityverification.controllers

import javax.inject.{Inject, Singleton}
import core.audit.Logging
import core.auth.StatelessController
import core.config.BaseControllerConfig
import core.services.AuthService
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}

import scala.concurrent.Future


@Singleton
class IdentityVerificationController @Inject()(override val baseConfig: BaseControllerConfig,
                                               mcc: MessagesControllerComponents,
                                               val authService: AuthService,
                                               logging: Logging
                                              ) extends StatelessController(mcc) {

  def identityVerificationUrl(implicit request: Request[AnyContent]): String =
    applicationConfig.identityVerificationURL +
      IdentityVerificationController.identityVerificationUrl(
        applicationConfig.contactFormServiceIdentifier,
        applicationConfig.baseUrl
      )

  def gotoIV: Action[AnyContent] = Authenticated.asyncUnrestricted {
    implicit user =>
      implicit request =>
        Future.successful(Redirect(identityVerificationUrl))
  }

  def ivFailed: Action[AnyContent] = Action { implicit request =>
    Ok(identityverification.views.html.iv_failed(identityverification.controllers.routes.IdentityVerificationController.gotoIV().url))
  }
}

object IdentityVerificationController {

  def completionUri(baseUrl: String): String = baseUrl + usermatching.controllers.routes.HomeController.index().url

  def failureUri(baseUrl: String): String = baseUrl + identityverification.controllers.routes.IdentityVerificationController.ivFailed().url

  def identityVerificationUrl(origin: String, baseUrl: String)(implicit request: Request[AnyContent]): String =
    s"/mdtp/uplift?origin=$origin&confidenceLevel=200&completionURL=${completionUri(baseUrl)}&failureURL=${failureUri(baseUrl)}"

}
