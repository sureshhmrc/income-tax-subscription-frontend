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

import java.time.Duration

import assets.MessageLookup.{UserDetailsLockout => messages}
import core.ITSASessionKeys
import core.auth.UserMatching
import core.config.MockConfig
import core.controllers.ControllerBaseSpec
import core.utils.TestConstants._
import core.views.ViewSpecTrait
import org.jsoup.Jsoup
import play.api.Play
import play.api.http.Status
import play.api.i18n.Messages.Implicits.applicationMessagesApi
import play.api.mvc.{Action, AnyContent, Cookie, Request}
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsString, contentType, _}
import uk.gov.hmrc.http.{NotFoundException, SessionKeys}
import uk.gov.hmrc.play.language.LanguageUtils.WelshLangCode
import usermatching.services.mocks.MockUserLockoutService

class UserDetailsLockoutControllerSpec extends ControllerBaseSpec
  with MockUserLockoutService {

  // Required for trait but no authorisation tests are required
  override val controllerName: String = "UserDetailsLockoutController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "show" -> TestUserDetailsLockoutController.show
  )

  def createTestUserDetailsLockoutController(enableMatchingFeature: Boolean) = new UserDetailsLockoutController(
    MockBaseControllerConfig,
    messagesApi,
    mockAuthService,
    mockUserLockoutService
  )

  lazy val TestUserDetailsLockoutController = createTestUserDetailsLockoutController(enableMatchingFeature = true)

  lazy val request = userMatchingRequest.withSession(SessionKeys.userId -> testUserId.value, ITSASessionKeys.JourneyStateKey -> UserMatching.name)


  "Calling the 'show' action of the UserDetailsLockoutController" when {

    "the user is locked out" should {
      lazy val result = TestUserDetailsLockoutController.show(request)
      lazy val document = Jsoup.parse(contentAsString(result))

      "return 200" in {
        setupMockLockedOut(testUserId.value)
        status(result) must be(Status.OK)

        contentType(result) must be(Some("text/html"))
        charset(result) must be(Some("utf-8"))

        document.title mustBe messages.title
      }
    }

    "the user is not locked out" should {
      s"redirect to ${usermatching.controllers.routes.UserDetailsController.show().url}" in {
        setupMockNotLockedOut(testUserId.value)

        lazy val result = TestUserDetailsLockoutController.show(request)

        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe usermatching.controllers.routes.UserDetailsController.show().url
      }
    }

  }

  "durationText" when {
    "the language is English" should {
      implicit lazy val r: Request[_] = request

      "convert time using correct singular units" in {
        val testDuration = List(Duration.ofHours(1), Duration.ofMinutes(1), Duration.ofSeconds(1)).reduce(_.plus(_))
        TestUserDetailsLockoutController.durationText(testDuration) mustBe "1 hour 1 minute 1 second"
      }

      "convert time using correct plural units" in {
        val testDuration = List(Duration.ofHours(2), Duration.ofMinutes(2), Duration.ofSeconds(2)).reduce(_.plus(_))
        TestUserDetailsLockoutController.durationText(testDuration) mustBe "2 hours 2 minutes 2 seconds"
      }

      "convert different combinations of hour minute seconds correctly" in {
        val testDuration1 = List(Duration.ofHours(2), Duration.ofSeconds(2)).reduce(_.plus(_))
        TestUserDetailsLockoutController.durationText(testDuration1) mustBe "2 hours 2 seconds"
        val testDuration2 = List(Duration.ofMinutes(2), Duration.ofSeconds(2)).reduce(_.plus(_))
        TestUserDetailsLockoutController.durationText(testDuration2) mustBe "2 minutes 2 seconds"
        val testDuration3 = List(Duration.ofMinutes(2)).reduce(_.plus(_))
        TestUserDetailsLockoutController.durationText(testDuration3) mustBe "2 minutes"
      }
    }
    "the language is Welsh" should {
      implicit lazy val r: Request[_] = request.withCookies(Cookie(Play.langCookieName(applicationMessagesApi), WelshLangCode))
      "convert time using correct singular units" in {
        val testDuration = List(Duration.ofHours(1), Duration.ofMinutes(1), Duration.ofSeconds(1)).reduce(_.plus(_))
        TestUserDetailsLockoutController.durationText(testDuration) mustBe "1 awr 1 munud 1 eiliad"
      }

      "convert time using correct plural units" in {
        val testDuration = List(Duration.ofHours(2), Duration.ofMinutes(2), Duration.ofSeconds(2)).reduce(_.plus(_))
        TestUserDetailsLockoutController.durationText(testDuration) mustBe "2 oriau 2 munudau 2 eiliadau"
      }

      "convert different combinations of hour minute seconds correctly" in {
        val testDuration1 = List(Duration.ofHours(2), Duration.ofSeconds(2)).reduce(_.plus(_))
        TestUserDetailsLockoutController.durationText(testDuration1) mustBe "2 oriau 2 eiliadau"
        val testDuration2 = List(Duration.ofMinutes(2), Duration.ofSeconds(2)).reduce(_.plus(_))
        TestUserDetailsLockoutController.durationText(testDuration2) mustBe "2 munudau 2 eiliadau"
        val testDuration3 = List(Duration.ofMinutes(2)).reduce(_.plus(_))
        TestUserDetailsLockoutController.durationText(testDuration3) mustBe "2 munudau"
      }
    }
  }


  authorisationTests()

}
