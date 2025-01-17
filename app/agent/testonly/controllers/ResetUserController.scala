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

package agent.testonly.controllers

import javax.inject.{Inject, Singleton}

import core.config.BaseControllerConfig
import agent.controllers.ITSASessionKeys
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent}
import core.services.AuthService
import agent.auth.StatelessController

import scala.concurrent.Future

@Singleton
class ResetUserController @Inject()(val baseConfig: BaseControllerConfig,
                                    val messagesApi: MessagesApi,
                                    val authService: AuthService
                                   ) extends StatelessController {

  val resetUser: Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      Future.successful(
        Ok("User reset successfully")
          .removingFromSession(ITSASessionKeys.MTDITID)
      )
  }
}
