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

import agent.assets.MessageLookup.{NoClientRelationship => messages}
import agent.services.ClientRelationshipService
import agent.services.mocks.MockKeystoreService
import org.jsoup.Jsoup
import play.api.mvc.{Action, AnyContent}
import play.api.test.Helpers._

class NoClientRelationshipControllerSpec
  extends AgentControllerBaseSpec
    with MockKeystoreService {

  override lazy val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "show" -> TestNoClientRelationshipController.show,
    "submit" -> TestNoClientRelationshipController.submit
  )
  override val controllerName: String = "NoClientRelationshipController"
  val mockClientRelationshipService = mock[ClientRelationshipService]

  object TestNoClientRelationshipController extends NoClientRelationshipController(
    MockBaseControllerConfig,
    messagesApi,
    mockClientRelationshipService,
    MockKeystoreService,
    mockAuthService
  )


  "show" should {
    "return an OK with the no client relationship page" in {
      val res = TestNoClientRelationshipController.show(userMatchingRequest)

      status(res) mustBe OK

      lazy val document = Jsoup.parse(contentAsString(res))

      document.title mustBe messages.heading

      document.select("form").attr("action") mustBe agent.controllers.routes.NoClientRelationshipController.submit().url
    }
  }

  "submit" should {
    s"redirect to '${agent.controllers.matching.routes.ClientDetailsController.show().url}'" in {
      val res = TestNoClientRelationshipController.submit(userMatchingRequest)

      status(res) mustBe SEE_OTHER
      redirectLocation(res) must contain(agent.controllers.matching.routes.ClientDetailsController.show().url)
    }
  }

  authorisationTests()
}
