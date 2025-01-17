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

import javax.inject.{Inject, Singleton}

import core.config.{AppConfig, BaseControllerConfig}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import scala.concurrent.Future

@Singleton
class NotEnrolledAgentServicesController @Inject()(val baseConfig: BaseControllerConfig,
                                                   val messagesApi: MessagesApi
                                                  ) extends FrontendController with I18nSupport {

  implicit lazy val appConfig: AppConfig = baseConfig.applicationConfig

  val show: Action[AnyContent] = Action.async {
    implicit request =>
      Future.successful(Ok(agent.views.html.not_enrolled_agent_services()))
  }

}
