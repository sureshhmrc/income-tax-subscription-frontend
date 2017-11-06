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

package core.config

import javax.inject.{Inject, Provider}

import core.views.html.templates.error_template
import play.api.Configuration
import play.api.i18n.MessagesApi
import play.api.mvc.Request
import uk.gov.hmrc.play.bootstrap.http.FrontendErrorHandler

class ErrorHandler @Inject()(val appConfigProvider: Provider[AppConfig],
                             val messagesApi: MessagesApi, val configuration: Configuration) extends FrontendErrorHandler {

  override def standardErrorTemplate(pageTitle: String, heading: String, message: String)(implicit request: Request[_]):
  _root_.play.twirl.api.HtmlFormat.Appendable =
    error_template(pageTitle, heading, message)(implicitly, implicitly, appConfigProvider.get)

}
