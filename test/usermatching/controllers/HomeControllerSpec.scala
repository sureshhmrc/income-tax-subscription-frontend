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

import assets.MessageLookup.FrontPage
import core.ITSASessionKeys
import core.audit.Logging
import core.auth.Registration
import core.config.MockConfig
import core.controllers.ControllerBaseSpec
import core.services.mocks.MockKeystoreService
import core.utils.TestConstants
import core.utils.TestConstants._
import incometax.subscription.services.mocks.MockSubscriptionService
import incometax.unauthorisedagent.services.mocks.MockSubscriptionStoreRetrievalService
import org.jsoup.Jsoup
import org.mockito.Mockito.reset
import play.api.http.Status
import play.api.mvc.{Action, AnyContent}
import play.api.test.Helpers.{await, contentAsString, _}
import uk.gov.hmrc.http.InternalServerException
import usermatching.services.mocks.MockCitizenDetailsService


class HomeControllerSpec extends ControllerBaseSpec
  with MockSubscriptionService
  with MockKeystoreService
  with MockCitizenDetailsService
  with MockSubscriptionStoreRetrievalService {

  override val controllerName: String = "HomeControllerSpec"

  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "index" -> TestHomeController(showStartPage = false).index()
  )

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockAuthService)
    mockNinoRetrieval()
  }

  def TestHomeController(showStartPage: Boolean = true, registrationFeature: Boolean = false) = new HomeController(
    mockBaseControllerConfig(new MockConfig {
      override val showGuidance: Boolean = showStartPage
      override val enableRegistration: Boolean = registrationFeature
    }),
    messagesApi,
    mockSubscriptionService,
    MockKeystoreService,
    mockAuthService,
    mockCitizenDetailsService,
    mockSubscriptionStoreRetrievalService,
    app.injector.instanceOf[Logging]
  )

  val testNino = TestConstants.testNino
  val testUtr = TestConstants.testUtr

  "Calling the home action of the Home controller with an authorised user" when {

    "the start page (showStartPage) is enabled" should {

      lazy val result = TestHomeController(showStartPage = true).home()(subscriptionRequest)

      "Return status OK (200)" in {
        status(result) must be(Status.OK)
      }

      "Should have the page title" in {
        Jsoup.parse(contentAsString(result)).title mustBe FrontPage.title
      }
    }

    "the start page (showStartPage) is disabled" should {
      lazy val result = TestHomeController(showStartPage = false).home()(subscriptionRequest)

      "Return status SEE_OTHER (303) redirect" in {
        status(result) must be(Status.SEE_OTHER)
      }

      "Redirect to the 'Index' page" in {
        redirectLocation(result).get mustBe usermatching.controllers.routes.HomeController.index().url
      }
    }
  }

  "index" when {
    "the user has a nino" when {
      "the user already has an MTDIT subscription on ETMP" should {
        "redirect to the claim subscription page" in {
          setupMockGetSubscriptionFound(testNino)
          setupMockKeystoreSaveFunctions()

          val result = TestHomeController().index(fakeRequest)

          status(result) must be(Status.SEE_OTHER)
          redirectLocation(result).get mustBe incometax.subscription.controllers.routes.ClaimSubscriptionController.claim().url

          verifyKeystore(saveSubscriptionId = 1)
        }
      }
      "the user does not already have an MTDIT subscription on ETMP" when {
        "the user's unauthorised agent has already started the subscription journey" should {
          "redirect to the confirm agent subscription page" in {
            setupMockGetSubscriptionNotFound(testNino)
            mockRetrieveSubscriptionData(testNino)(successfulStoredSubscriptionFound)

            val result = TestHomeController().index(fakeRequest)

            status(result) must be(Status.SEE_OTHER)
            redirectLocation(result).get mustBe incometax.unauthorisedagent.controllers.routes.AuthoriseAgentController.show().url
            session(result).get(ITSASessionKeys.AgentReferenceNumber) must contain(testArn)
          }
        }

        "the user does not have a current unauthorised subscription journey" when {
          "the user has a UTR" should {
            "redirect to the sign up journey" in {
              mockNinoAndUtrRetrieval()
              setupMockGetSubscriptionNotFound(testNino)
              mockRetrieveSubscriptionData(testNino)(successfulSubscriptionNotFound)

              val result = await(TestHomeController().index(fakeRequest))

              status(result) must be(Status.SEE_OTHER)

              redirectLocation(result).get mustBe digitalcontact.controllers.routes.PreferencesController.checkPreferences().url
            }
          }

          "the user does not have a utr" when {
            "the user has a matching utr in CID against their NINO" in {
              mockNinoRetrieval()
              setupMockGetSubscriptionNotFound(testNino)
              mockRetrieveSubscriptionData(testNino)(successfulSubscriptionNotFound)
              mockLookupUserWithUtr(testNino)(testUtr)

              val result = await(TestHomeController().index(fakeRequest))

              status(result) mustBe SEE_OTHER
              redirectLocation(result).get mustBe digitalcontact.controllers.routes.PreferencesController.checkPreferences().url

              session(result).get(ITSASessionKeys.UTR) mustBe Some(testUtr)
            }

            "the user does not have a matching utr in CID" when {
              "the registration feature flag is on" should {
                "redirect to the registration journey" in {
                  mockNinoRetrieval()
                  setupMockGetSubscriptionNotFound(testNino)
                  mockRetrieveSubscriptionData(testNino)(successfulSubscriptionNotFound)
                  mockLookupUserWithoutUtr(testNino)

                  val result = TestHomeController(registrationFeature = true).index()(registrationRequest)

                  status(result) mustBe SEE_OTHER
                  redirectLocation(result).get mustBe digitalcontact.controllers.routes.PreferencesController.checkPreferences().url
                  await(result).session(registrationRequest).get(ITSASessionKeys.JourneyStateKey) must contain(Registration.name)
                }
              }
              "the registration feature flag is off" should {
                "redirect to the no SA page" in {
                  mockNinoRetrieval()
                  setupMockGetSubscriptionNotFound(testNino)
                  mockRetrieveSubscriptionData(testNino)(successfulSubscriptionNotFound)
                  mockLookupUserWithoutUtr(testNino)

                  val result = TestHomeController(registrationFeature = false).index()(registrationRequest)

                  status(result) mustBe SEE_OTHER
                  redirectLocation(result).get mustBe usermatching.controllers.routes.NoSAController.show().url

                  await(result).session(registrationRequest).get(ITSASessionKeys.UTR) mustBe None
                }
              }
            }
          }
        }
      }
      "the call to check the user's subscription status fails" should {
        "return an error page" in {
          setupMockGetSubscriptionFailure(testNino)

          intercept[InternalServerException](await(TestHomeController().index(fakeRequest)))
        }
      }
    }
    "the user does not have a nino but has an IR-SA enrolment" should {
      "use SAUTR to get NINO from Citizen details and redirect to rent property page" in {
        mockUtrRetrieval()
        mockLookupNinoSuccess(testUtr)
        val redirectControllerUrl = incometax.incomesource.controllers.routes.RentUkPropertyController.show().url

        val result = await(TestHomeController().index(fakeRequest))
        session(result).get(ITSASessionKeys.NINO) must contain(testNino)
        status(result) mustBe Status.SEE_OTHER
        redirectLocation(result).get mustBe redirectControllerUrl

      }
    }
    "the user does not have a nino" should {
      "redirect to the NINO resolver" in {
        mockIndividualWithNoEnrolments()

        val result = TestHomeController().index(fakeRequest)

        status(result) mustBe Status.SEE_OTHER
        redirectLocation(result).get mustBe usermatching.controllers.routes.UserDetailsController.show().url
      }
    }
  }

  authorisationTests()

}
