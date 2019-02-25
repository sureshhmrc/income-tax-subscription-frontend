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

import org.jsoup.Jsoup
import play.api.http.Status
import play.api.mvc.{Action, AnyContent}
import play.api.test.Helpers.{contentAsString, _}

class NotEnrolledAgentServicesControllerSpec extends AgentControllerBaseSpec {

  // Required for trait but no authorisation tests are required
  override val controllerName: String = "NotEnrolledAgentServices"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map()

  object TestNotEnrolledAgentServicesController extends NotEnrolledAgentServicesController(
    MockBaseControllerConfig,
    stubMCC
  )

  "Calling the 'show' action of the NotEnrolledAgentServicesController" should {

    lazy val result = TestNotEnrolledAgentServicesController.show(subscriptionRequest)
    lazy val document = Jsoup.parse(contentAsString(result))

    "return 200" in {
      status(result) must be(Status.OK)
    }
  }
}
