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

import agent.audit.Logging
import agent.auth.UnauthorisedAgentController
import agent.services.KeystoreService
import cats.implicits._
import core.config.BaseControllerConfig
import core.services.AuthService
import javax.inject.{Inject, Singleton}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}

@Singleton
class UnauthorisedAgentConfirmationController @Inject()(val baseConfig: BaseControllerConfig,
                                                        mcc: MessagesControllerComponents,
                                                        val keystoreService: KeystoreService,
                                                        val authService: AuthService,
                                                        val logging: Logging
                                                       ) extends UnauthorisedAgentController(mcc) {

  override val unauthorisedDefaultPredicate =
    agent.auth.AuthPredicates.unauthorisedUserMatchingPredicates |+| agent.auth.AuthPredicates.hasSubmitted

  def show: Action[AnyContent] = Authenticated { implicit request =>
    implicit user =>
      Ok(agent.views.html.unauthorised_agent_confirmation(
        postAction = agent.controllers.routes.AddAnotherClientController.addAnother()
      ))
  }
}
