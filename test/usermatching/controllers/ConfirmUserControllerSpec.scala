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
import core.auth.{UserMatched, UserMatching}
import core.controllers.ControllerBaseSpec
import core.services.mocks.MockKeystoreService
import core.utils.TestConstants._
import core.utils.{TestConstants, TestModels}
import org.scalatest.OptionValues
import play.api.http.Status
import play.api.mvc._
import play.api.test.Helpers.{await, _}
import uk.gov.hmrc.http.{HttpResponse, InternalServerException, SessionKeys}
import usermatching.services.mocks.{MockUserLockoutService, MockUserMatchingService}

import scala.concurrent.Future

class ConfirmUserControllerSpec extends ControllerBaseSpec
  with MockUserLockoutService with MockUserMatchingService with MockKeystoreService
  with OptionValues {

  override val controllerName: String = "ConfirmUserController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "show" -> TestConfirmUserController.show(),
    "submit" -> TestConfirmUserController.submit()
  )

  object TestConfirmUserController extends ConfirmUserController(
    MockBaseControllerConfig,
    messagesApi,
    mockAuthService,
    mockUserMatchingService,
    mockUserLockoutService
  )

  override def beforeEach(): Unit = {
    super.beforeEach()
  }

  val userDetails = TestModels.testUserDetails
  val token = TestConstants.testToken

  lazy val request = userMatchingRequest.withSession(SessionKeys.userId -> testUserId.value, ITSASessionKeys.JourneyStateKey -> UserMatching.name).buildRequest(userDetails)


  "Calling the show action of the ConfirmUserController with an authorised user" should {

    def call(request: Request[AnyContent]) = TestConfirmUserController.show()(request)

    "when there are no user details stored redirect them to user details" in {
      setupMockNotLockedOut(testUserId.value)

      val r = request.buildRequest(None)
      val result = call(r)

      status(result) must be(Status.SEE_OTHER)

      redirectLocation(result) must contain(usermatching.controllers.routes.UserDetailsController.show().url)

      await(result).verifyStoredUserDetailsIs(None)(r)
    }

    "if there are user details return ok (200)" in {
      setupMockNotLockedOut(testUserId.value)

      val r = request.buildRequest(userDetails)

      val result = call(r)

      status(result) must be(Status.OK)

      await(result).verifyStoredUserDetailsIs(userDetails)(r)
    }
  }

  "Calling the submit action of the confirmUserController with a locked out user" should {
    def callSubmit(): Future[Result] = TestConfirmUserController.submit()(request)

    "return the user details page" in {
      setupMockLockedOut(testUserId.value)

      val result = callSubmit()

      redirectLocation(result) must contain(usermatching.controllers.routes.UserDetailsLockoutController.show().url)
    }
  }

  "Calling the submit action of the confirmUserController with no keystore data" should {
    def callSubmit(request: Request[AnyContent]): Future[Result] = TestConfirmUserController.submit()(request)

    "return the user details page" in {
      setupMockNotLockedOut(testUserId.value)

      val r = request.buildRequest(None)

      val result = callSubmit(r)

      redirectLocation(result) must contain(usermatching.controllers.routes.UserDetailsController.show().url)
    }
  }

  "Calling the submit action of the ConfirmUserController with an authorised user and valid submission" when {

    def callSubmit(request: Request[AnyContent]): Future[Result] = TestConfirmUserController.submit()(request)

    "UserMatchingService returned UnexpectedFailure" should {
      "return a InternalServerException" in {
        mockUserMatchException(userDetails)
        setupMockNotLockedOut(testUserId.value)

        val r = request.buildRequest(userDetails)

        val result = callSubmit(r)

        intercept[InternalServerException](await(result))
      }
    }

    "UserMatchingService returns user with nino and utr" should {
      s"redirect to the home controller with nino and sautr added to session" in {
        mockUserMatchSuccess(userDetails)
        setupMockNotLockedOut(testUserId.value)

        val r = request.buildRequest(userDetails)

        val fresult = callSubmit(r)
        val result = await(fresult)
        status(fresult) mustBe SEE_OTHER
        redirectLocation(fresult) mustBe Some(routes.HomeController.index().url)

        val session = result.session(request)
        session.get(ITSASessionKeys.NINO) must contain(TestConstants.testNino)
        session.get(ITSASessionKeys.UTR) must contain(TestConstants.testUtr)
        session.get(ITSASessionKeys.JourneyStateKey) mustBe Some(UserMatched.name)
        result.verifyStoredUserDetailsIs(None)(r)
      }
    }

    "UserMatchingService returns user with only nino" should {
      s"redirect to the home controller with nino added to session" in {
        mockUserMatchSuccessNoUtr(userDetails)
        setupMockNotLockedOut(testUserId.value)

        val r = request.buildRequest(userDetails)

        val result = callSubmit(r)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.HomeController.index().url)

        val session = await(result).session(request)
        session.get(ITSASessionKeys.NINO) must contain(TestConstants.testNino)
        session.get(ITSASessionKeys.JourneyStateKey) mustBe Some(UserMatched.name)
      }
    }

    "UserMatchingService returns nothing" when {
      "not locked out is returned by the service" should {
        "redirect to the user details page and apply the new counter to session" in {
          mockUserMatchNotFound(userDetails)
          setupMockNotLockedOut(testUserId.value)
          setupIncrementNotLockedOut(testUserId.value, 0)

          val r = request.buildRequest(userDetails)

          val result = callSubmit(r)

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(usermatching.controllers.routes.UserDetailsErrorController.show().url)

          val session = await(result).session(request)
          session.get(ITSASessionKeys.FailedUserMatching) must contain("1")
        }
      }

      "locked out is returned by the service" should {
        "remove the counter from the session, lockout the user then redirect to the locked out page" in {
          val currentFailedMatches = 3
          implicit val requestWithLockout = request.withSession(
            SessionKeys.userId -> testUserId.value,
            ITSASessionKeys.FailedUserMatching -> currentFailedMatches.toString
          )

          mockUserMatchNotFound(userDetails)
          setupMockNotLockedOut(testUserId.value)
          setupIncrementLockedOut(testUserId.value, currentFailedMatches)
          setupMockKeystore(deleteAll = HttpResponse(Status.OK))

          val r = requestWithLockout.buildRequest(userDetails)

          val result = TestConfirmUserController.submit()(r)

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(usermatching.controllers.routes.UserDetailsLockoutController.show().url)

          val session = await(result).session
          session.get(ITSASessionKeys.FailedUserMatching) mustBe empty

          verifyIncrementLockout(testUserId.value, 1)
        }
      }
    }
  }


  authorisationTests()

}
