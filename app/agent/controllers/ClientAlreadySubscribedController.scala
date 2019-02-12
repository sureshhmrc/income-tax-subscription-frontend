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

import agent.auth.UserMatchingController
import core.config.BaseControllerConfig
import core.services.AuthService
import javax.inject.{Inject, Singleton}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}

import scala.concurrent.Future

@Singleton
class ClientAlreadySubscribedController @Inject()(val baseConfig: BaseControllerConfig,
                                                  mcc: MessagesControllerComponents,
                                                  val authService: AuthService
                                                 ) extends UserMatchingController(mcc) {

  val show: Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      Future.successful(Ok(agent.views.html.client_already_subscribed(
        postAction = agent.controllers.routes.ClientAlreadySubscribedController.submit()
      )))
  }

  val submit: Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      Future.successful(Redirect(agent.controllers.matching.routes.ClientDetailsController.show()))
  }

}
