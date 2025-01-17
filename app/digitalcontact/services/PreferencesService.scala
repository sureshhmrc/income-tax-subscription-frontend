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

package digitalcontact.services

import javax.inject.{Inject, Singleton}
import digitalcontact.connectors.PreferenceFrontendConnector
import digitalcontact.models.{PaperlessPreferenceError, PaperlessState}
import play.api.i18n.Messages
import play.api.mvc.{AnyContent, Request}

import scala.concurrent.Future

@Singleton
class PreferencesService @Inject()(preferenceFrontendConnector: PreferenceFrontendConnector) {

  @inline def checkPaperless(token: String)(implicit request: Request[AnyContent], messages: Messages): Future[Either[PaperlessPreferenceError.type, PaperlessState]] =
    preferenceFrontendConnector.checkPaperless(token)

  @inline def defaultChoosePaperlessUrl(implicit request: Request[AnyContent], messages: Messages): String = preferenceFrontendConnector.choosePaperlessUrl

}
