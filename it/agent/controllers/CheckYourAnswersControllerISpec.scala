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

package agent.controllers

import _root_.agent.helpers.IntegrationTestConstants._
import _root_.agent.helpers.IntegrationTestModels.{fullKeystoreData, testStoredSubscription}
import _root_.agent.helpers.servicemocks._
import _root_.agent.helpers.{ComponentSpecBase, SessionCookieCrumbler}
import _root_.agent.services.CacheConstants._
import core.config.featureswitch.{EligibilityPagesFeature, FeatureSwitching}
import helpers.IntegrationTestModels.testEnrolmentKey
import helpers.servicemocks.{SubscriptionStoreStub, SubscriptionStub, TaxEnrolmentsStub}
import play.api.http.Status._
import play.api.i18n.Messages

class CheckYourAnswersControllerISpec extends ComponentSpecBase with FeatureSwitching {

  override def beforeEach(): Unit = {
    super.beforeEach()
    disable(EligibilityPagesFeature)
  }



  "GET /check-your-answers" when {
    "keystore returns all data" should {
      "show the check your answers page" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubFullKeystore()

        When("GET /check-your-answers is called")
        val res = IncomeTaxSubscriptionFrontend.checkYourAnswers()

        Then("Should return a OK with the check your answers page")
        res should have(
          httpStatus(OK),
          pageTitle(Messages("agent.summary.title"))
        )
      }
    }

    "keystore does not return the terms field" in {
      Given("I setup the Wiremock stubs")
      AuthStub.stubAuthSuccess()
      KeystoreStub.stubKeystoreData(fullKeystoreData - Terms)

      When("GET /check-your-answers is called")
      val res = IncomeTaxSubscriptionFrontend.checkYourAnswers()

      Then("Should return a SEE_OTHER with a redirect location of terms")
      res should have(
        httpStatus(SEE_OTHER),
        redirectURI(termsURI)
      )
    }

    "keystore does not return the terms field and the eligibility pages feature switch is on" in {
      Given("I setup the Wiremock stubs")
      AuthStub.stubAuthSuccess()
      KeystoreStub.stubKeystoreData(fullKeystoreData - Terms)
      enable(EligibilityPagesFeature)

      When("GET /check-your-answers is called")
      val res = IncomeTaxSubscriptionFrontend.checkYourAnswers()

      Then("Should return an OK and show the check your answers page")
      res should have(
        httpStatus(OK),
        pageTitle(Messages("agent.summary.title"))
      )
    }

  }


  "POST /check-your-answers" when {
    "The whole subscription process was successful" when {
      "agent is authorised" should {
        "call subscription on the back end service and redirect to confirmation page" in {
          Given("I setup the wiremock stubs")
          AuthStub.stubAuthSuccess()
          KeystoreStub.stubFullKeystore()
          SubscriptionStub.stubSuccessfulSubscription(checkYourAnswersURI)
          TaxEnrolmentsStub.stubUpsertEnrolmentResult(testEnrolmentKey.asString, NO_CONTENT)
          KeystoreStub.stubPutMtditId()

          When("I call POST /check-your-answers")
          val res = IncomeTaxSubscriptionFrontend.submitCheckYourAnswers()

          Then("The result should have a status of SEE_OTHER and redirect to the confirmation page")
          res should have(
            httpStatus(SEE_OTHER),
            redirectURI(confirmationURI)
          )

          val cookieMap = SessionCookieCrumbler.getSessionMap(res)
          cookieMap(ITSASessionKeys.MTDITID) shouldBe testMTDID

        }

        "call subscription on the back end service and redirect to confirmation page when the eligibility pages feature switch is on" in {
          Given("I setup the wiremock stubs")
          AuthStub.stubAuthSuccess()
          KeystoreStub.stubKeystoreData(fullKeystoreData - Terms)
          SubscriptionStub.stubSuccessfulSubscription(checkYourAnswersURI)
          TaxEnrolmentsStub.stubUpsertEnrolmentResult(testEnrolmentKey.asString, NO_CONTENT)
          KeystoreStub.stubPutMtditId()
          enable(EligibilityPagesFeature)

          When("I call POST /check-your-answers")
          val res = IncomeTaxSubscriptionFrontend.submitCheckYourAnswers()

          Then("The result should have a status of SEE_OTHER and redirect to the confirmation page")
          res should have(
            httpStatus(SEE_OTHER),
            redirectURI(confirmationURI)
          )

          val cookieMap = SessionCookieCrumbler.getSessionMap(res)
          cookieMap(ITSASessionKeys.MTDITID) shouldBe testMTDID

        }
      }

      "agent is unauthorised" should {
        "call subscription on the back end service and redirect to confirmation page" in {
          Given("I setup the wiremock stubs")
          AuthStub.stubAuthSuccess()
          KeystoreStub.stubFullKeystore()
          SubscriptionStoreStub.stubSuccessfulStore(testStoredSubscription)

          When("I call POST /check-your-answers")
          val res = IncomeTaxSubscriptionFrontend.submitCheckYourAnswers(isAgentUnauthorised = true)

          Then("The result should have a status of SEE_OTHER and redirect to the confirmation page")
          res should have(
            httpStatus(SEE_OTHER),
            redirectURI(unauthorisedAgentConfirmationURI)
          )
        }
      }


      "keystore does not return the terms field" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubKeystoreData(fullKeystoreData - Terms)

        When("POST /check-your-answers is called")
        val res = IncomeTaxSubscriptionFrontend.submitCheckYourAnswers()

        Then("Should return a SEE_OTHER with a redirect location of terms")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(termsURI)
        )
      }

      "keystore does not return the terms field and the eligibility pages feature switch is on" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubKeystoreData(fullKeystoreData - Terms)
        SubscriptionStoreStub.stubSuccessfulStore(testStoredSubscription)
        enable(EligibilityPagesFeature)

        When("POST /check-your-answers is called")
        val res = IncomeTaxSubscriptionFrontend.submitCheckYourAnswers(isAgentUnauthorised = true)

        Then("The result should have a status of SEE_OTHER and redirect to the confirmation page")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(unauthorisedAgentConfirmationURI)
        )
      }
    }
  }

}
