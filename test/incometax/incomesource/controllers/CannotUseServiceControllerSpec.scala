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

package incometax.incomesource.controllers

import assets.MessageLookup.{CannotUseService => messages}
import core.controllers.ControllerBaseSpec
import org.jsoup.Jsoup
import play.api.http.Status
import play.api.mvc.{Action, AnyContent}
import play.api.test.Helpers._

class CannotUseServiceControllerSpec extends ControllerBaseSpec {

  override val controllerName: String = "CannotUseServiceController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map()

  object TestCannotUseServiceController extends CannotUseServiceController(
    MockBaseControllerConfig,
    messagesApi,
    mockAuthService
  )

  "Calling the show action of the Cannot Use Service Controller" when {

    def call = TestCannotUseServiceController.show(subscriptionRequest)

    "return ok (200)" in {
      val result = call
      val document = Jsoup.parse(contentAsString(result))
      status(result) must be(Status.OK)
      contentType(result) must be(Some("text/html"))
      charset(result) must be(Some("utf-8"))
      document.title mustBe messages.title
    }
  }
}


