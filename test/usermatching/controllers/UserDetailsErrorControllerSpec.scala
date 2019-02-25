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

package usermatching.controllers

import core.ITSASessionKeys
import core.auth.UserMatching
import core.controllers.ControllerBaseSpec
import core.utils.TestConstants.testUserId
import play.api.http.Status
import play.api.mvc.{Action, AnyContent}
import play.api.test.Helpers.{contentType, _}
import uk.gov.hmrc.http.SessionKeys

class UserDetailsErrorControllerSpec extends ControllerBaseSpec {

  // Required for trait but no authorisation tests are required
  override val controllerName: String = "UserDetailsErrorController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "show" -> TestUserDetailsErrorController.show,
    "submit" -> TestUserDetailsErrorController.submit
  )

  def createTestUserDetailsErrorController(enableMatchingFeature: Boolean) = new UserDetailsErrorController(
    MockBaseControllerConfig,
    stubMCC,
    mockAuthService
  )

  lazy val TestUserDetailsErrorController = createTestUserDetailsErrorController(enableMatchingFeature = true)

  lazy val request = userMatchingRequest.withSession(SessionKeys.userId -> testUserId.value, ITSASessionKeys.JourneyStateKey -> UserMatching.name)


  "Calling the 'show' action of the UserDetailsErrorController" should {

    lazy val result = TestUserDetailsErrorController.show(request)

    "return 200" in {
      status(result) must be(Status.OK)
    }

    "return HTML" in {
      contentType(result) must be(Some("text/html"))
      charset(result) must be(Some("utf-8"))
    }

  }

  "Calling the 'submit' action of the UserDetailsErrorController" should {

    lazy val result = TestUserDetailsErrorController.submit(request)

    "return 303" in {
      status(result) must be(Status.SEE_OTHER)
    }

    "Redirect to the 'User details' page" in {
      redirectLocation(result).get mustBe usermatching.controllers.routes.UserDetailsController.show().url
    }

  }


  authorisationTests()

}
