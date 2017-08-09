/*
 * Copyright 2017 HM Revenue & Customs
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

package controllers

import javax.inject.{Inject, Singleton}

import auth.AuthenticatedController
import config.BaseControllerConfig
import play.api.i18n.MessagesApi
import services.AuthService

import scala.concurrent.Future

@Singleton
class AlreadyEnrolledController @Inject()(val baseConfig: BaseControllerConfig,
                                          val messagesApi: MessagesApi,
                                          val authService: AuthService
                                         ) extends AuthenticatedController {
  val enrolled = Authenticated.asyncEnrolled {
    implicit request =>
      implicit val headerCarrier = hc(request)
      user =>
        Future.successful(
          Ok(
            views.html.enrolled.already_enrolled(
              subscriptionId = user.mtdItsaRef.get,
              signOutAction = controllers.routes.SignOutController.signOut()
            )
          )
        )
  }
}
