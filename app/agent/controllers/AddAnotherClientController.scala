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
import agent.auth.{IncomeTaxAgentUser, StatelessController}
import agent.services.KeystoreService
import core.auth.AuthPredicate.AuthPredicate
import core.config.BaseControllerConfig
import core.services.AuthService
import javax.inject.{Inject, Singleton}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}

@Singleton
class AddAnotherClientController @Inject()(override val baseConfig: BaseControllerConfig,
                                           mcc: MessagesControllerComponents,
                                           keystore: KeystoreService,
                                           val authService: AuthService,
                                           logging: Logging
                                          ) extends StatelessController(mcc) {

  override val statelessDefaultPredicate: AuthPredicate[IncomeTaxAgentUser] = agent.auth.AuthPredicates.defaultPredicates

  def addAnother(): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user => {
      for {
        _ <- keystore.deleteAll()
      } yield Redirect(agent.controllers.matching.routes.ClientDetailsController.show().url)
        .removingFromSession(ITSASessionKeys.JourneyStateKey)
        .removingFromSession(ITSASessionKeys.UnauthorisedAgentKey)
        .removingFromSession(ITSASessionKeys.clientData: _*)
    }.recover {
      case e =>
        logging.warn("AddAnotherClientController.addAnother encountered error: " + e)
        throw e
    }
  }

}
