/*
 * Copyright 2018 HM Revenue & Customs
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

package usermatching

import agent.services.CacheConstants._
import core.ITSASessionKeys
import core.config.featureswitch
import core.config.featureswitch.FeatureSwitching
import helpers.IntegrationTestConstants._
import helpers.servicemocks._
import helpers.{ComponentSpecBase, SessionCookieCrumbler}
import org.jsoup.Jsoup
import play.api.http.Status
import play.api.http.Status._
import play.api.i18n.Messages

class HomeControllerISpec extends ComponentSpecBase {

  "GET /report-quarterly/income-and-expenses/sign-up" when {
    "feature-switch.show-guidance is true" should {
      "return the guidance page" in {
        When("We hit to the guidance page route")
          val res = IncomeTaxSubscriptionFrontend.startPage()

        Then("Return the guidance page")
          res should have(
            httpStatus(SEE_OTHER),
            redirectURI(indexURI)
          )
      }
    }
  }
  "GET /report-quarterly/income-and-expenses/sign-up/index" when {
    "the user both nino and utr enrolments" when {
      "the user has a subscription" should {
        "redirect to the claim subscription page" in {
          Given("I setup the Wiremock stubs")
            AuthStub.stubAuthSuccess()
            CitizenDetailsStub.stubCIDUserWithNinoAndUtr(testNino, testUtr)
            SubscriptionStub.stubGetSubscriptionFound()
            KeystoreStub.stubPutMtditId()

          When("GET /index is called")
            val res = IncomeTaxSubscriptionFrontend.indexPage()

          Then("Should return a SEE OTHER with the claim subscription page")
            res should have(
              httpStatus(SEE_OTHER),
              redirectURI(claimSubscriptionURI)
            )
        }
      }
      "the user does not have a subscription" when {
        "the user is eligible" should {
          "redirect to the preferences page" in {
            Given("I setup the Wiremock stubs")
              AuthStub.stubAuthSuccess()
              CitizenDetailsStub.stubCIDUserWithNinoAndUtr(testNino, testUtr)
              SubscriptionStub.stubGetNoSubscription()
              EligibilityStub.stubEligibilityResponse(testUtr)(true)

            When("GET /index is called")
              val res = IncomeTaxSubscriptionFrontend.indexPage()

            Then("Should return a SEE OTHER and re-direct to the preferences page")
              res should have(
                httpStatus(SEE_OTHER),
                redirectURI(preferencesURI)
              )
          }
        }
        "the user is ineligible" should {
          "redirect to the Not eligible page" in {
            Given("I setup the Wiremock stubs")
            AuthStub.stubAuthSuccess()
            CitizenDetailsStub.stubCIDUserWithNinoAndUtr(testNino, testUtr)
            SubscriptionStub.stubGetNoSubscription()
            EligibilityStub.stubEligibilityResponse(testUtr)(false)

            When("GET /index is called")
            val res = IncomeTaxSubscriptionFrontend.indexPage()

            Then("Should return a SEE OTHER and re-direct to the not eligible page")
            res should have(
              httpStatus(SEE_OTHER),
              redirectURI(notEligibleURI)
            )
          }
        }
      }
      "the subscription call fails" should {
        "return an internal server error" in {
          Given("I setup the Wiremock stubs")
            AuthStub.stubAuthSuccess()
            CitizenDetailsStub.stubCIDUserWithNinoAndUtr(testNino, testUtr)
            SubscriptionStub.stubGetSubscriptionFail()

          When("GET /index is called")
            val res = IncomeTaxSubscriptionFrontend.indexPage()

          Then("Should return an INTERNAL_SERVER_ERROR")
            res should have(
              httpStatus(INTERNAL_SERVER_ERROR)
            )
        }
      }
    }
    "the user only has a nino in enrolment" when {
      "CID returned a record with UTR" when {
        "the user is eligible" should {
          "continue normally" in {
            Given("I setup the Wiremock stubs")
              AuthStub.stubAuthNoUtr()
              SubscriptionStub.stubGetNoSubscription()
              CitizenDetailsStub.stubCIDUserWithNinoAndUtr(testNino, testUtr)
              EligibilityStub.stubEligibilityResponse(testUtr)(true)

            When("GET /index is called")
              val res = IncomeTaxSubscriptionFrontend.indexPage()

            Then("Should return a SEE OTHER and re-direct to the preferences page")
              res should have(
                httpStatus(SEE_OTHER),
                redirectURI(preferencesURI)
              )

              val cookie = SessionCookieCrumbler.getSessionMap(res)
              cookie.keys should contain(ITSASessionKeys.UTR)
              cookie(ITSASessionKeys.UTR) shouldBe testUtr
          }
        }
      }
      "CID returned a record with out a UTR" should {
        "continue normally" in {
          Given("I setup the Wiremock stubs")
            AuthStub.stubAuthNoUtr()
            CitizenDetailsStub.stubCIDUserWithNinoAndUtr(testNino, testUtr)
            SubscriptionStub.stubGetNoSubscription()
            CitizenDetailsStub.stubCIDUserWithNoUtr(testNino)

          When("GET /index is called")
            val res = IncomeTaxSubscriptionFrontend.indexPage()

          Then("Should return a SEE OTHER and re-direct to the no nino page")
            res should have(
              httpStatus(SEE_OTHER),
              redirectURI(noSaURI)
            )

            val cookie = SessionCookieCrumbler.getSessionMap(res)
            cookie.keys should not contain ITSASessionKeys.UTR
        }
      }
      "CID could not find the user" should {
        "display error page" in {
          Given("I setup the Wiremock stubs")
            AuthStub.stubAuthNoUtr()
            SubscriptionStub.stubGetNoSubscription()
            CitizenDetailsStub.stubCIDNotFound(testNino)

          When("GET /index is called")
            val res = IncomeTaxSubscriptionFrontend.indexPage()

          Then("Should return an INTERNAL_SERVER_ERROR")
            res should have(
              httpStatus(INTERNAL_SERVER_ERROR),
              pageTitle("Sorry, we are experiencing technical difficulties - 500")
            )

            val cookie = SessionCookieCrumbler.getSessionMap(res)
            cookie.keys should not contain ITSASessionKeys.UTR
        }
      }
    }

    "the unauthorised agent feature switch is on" when {
      "the user has an unfinished unauthorised agent journey" should {
        "redirect to the confirm agent subscription journey" in {
          enable(featureswitch.UnauthorisedAgentFeature)

          Given("I setup the Wiremock stubs")
            AuthStub.stubAuthNoUtr()
            CitizenDetailsStub.stubCIDUserWithNinoAndUtr(testNino, testUtr)
            SubscriptionStub.stubGetNoSubscription()
            EligibilityStub.stubEligibilityResponse(testUtr)(true)
            SubscriptionStoreStub.stubSuccessfulRetrieval()
            KeystoreStub.stubKeystoreSave(IncomeSource)
            KeystoreStub.stubKeystoreSave(OtherIncome)
            KeystoreStub.stubKeystoreSave(AccountingPeriodDate)
            KeystoreStub.stubKeystoreSave(BusinessName)
            KeystoreStub.stubKeystoreSave(AccountingMethod)

          When("GET /index is called")
            val res = IncomeTaxSubscriptionFrontend.indexPage()

          Then("Should return SEE_OTHER with a redirect of confirm agent subscription")
            res should have(
              httpStatus(SEE_OTHER),
              redirectURI(authoriseAgentUri)
            )
        }
      }
    }
  }

}

class HomeControllerRegEnabledISpec extends ComponentSpecBase with FeatureSwitching {

  enable(featureswitch.RegistrationFeature)

  "GET /report-quarterly/income-and-expenses/sign-up/index" when {
    "the user both nino and utr enrolments" when {
      "the user does not have a subscription" should {
        "redirect to the preferences page" in {
          Given("I setup the Wiremock stubs")
            AuthStub.stubAuthSuccess()
            CitizenDetailsStub.stubCIDUserWithNinoAndUtr(testNino, testUtr)
            SubscriptionStub.stubGetNoSubscription()
            EligibilityStub.stubEligibilityResponse(testUtr)(true)

          When("GET /index is called")
            val res = IncomeTaxSubscriptionFrontend.indexPage()

          Then("Should return a SEE OTHER and re-direct to the preferences page")
            res should have(
              httpStatus(SEE_OTHER),
              redirectURI(preferencesURI)
            )
        }
      }
      "the subscription call fails" should {
        "return an internal server error" in {
          Given("I setup the Wiremock stubs")
            AuthStub.stubAuthSuccess()
            CitizenDetailsStub.stubCIDUserWithNinoAndUtr(testNino, testUtr)
            SubscriptionStub.stubGetSubscriptionFail()

          When("GET /index is called")
            val res = IncomeTaxSubscriptionFrontend.indexPage()

          Then("Should return an INTERNAL_SERVER_ERROR")
            res should have(
              httpStatus(INTERNAL_SERVER_ERROR)
            )
        }
      }
    }
    "the user only has a nino in enrolment" when {
      "the user does not have a subscription" should {
        "redirect to the preferences page" in {
          Given("I setup the Wiremock stubs")
            AuthStub.stubAuthNoUtr()
            SubscriptionStub.stubGetNoSubscription()
            CitizenDetailsStub.stubCIDUserWithNoUtr(testNino)
            EligibilityStub.stubEligibilityResponse(testUtr)(true)

          When("GET /index is called")
            val res = IncomeTaxSubscriptionFrontend.indexPage()

          Then("Should return a SEE OTHER and re-direct to the preferences page")
            res should have(
              httpStatus(SEE_OTHER),
              redirectURI(preferencesURI)
            )
        }
      }
    }
  }
}
