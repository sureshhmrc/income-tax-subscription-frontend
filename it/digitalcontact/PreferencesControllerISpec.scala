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

package digitalcontact

import core.ITSASessionKeys
import core.config.featureswitch.FeatureSwitching
import core.services.CacheConstants._
import helpers.ComponentSpecBase
import helpers.IntegrationTestConstants._
import helpers.servicemocks.{AuthStub, KeystoreStub, PreferencesStub, PreferencesTokenStub}
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK, SEE_OTHER}
import play.api.i18n.Messages
import play.api.libs.json.JsString

class PreferencesControllerISpec extends ComponentSpecBase with FeatureSwitching {

  "GET /preferences" when {
    "where the user has previously accepted paperless where optedIn is set to True" in {

      Given("I setup the Wiremock stubs")
      AuthStub.stubAuthSuccess()
      PreferencesTokenStub.stubStoreNinoSuccess()
      KeystoreStub.stubKeystoreSave(PaperlessPreferenceToken)
      PreferencesStub.newStubPaperlessActivated()

      When("GET /preferences is called")
      val res = IncomeTaxSubscriptionFrontend.preferences()

      Then("Should return a SEE_OTHER with a re-direct location of the next page")
      res should have(
        httpStatus(SEE_OTHER),
        redirectURI(rentUkPropertyURI)
      )
    }

    "where the user has previously accepted paperless where optedIn is set to false and a redirect location is returned" in {
      Given("I setup the Wiremock stubs")
      AuthStub.stubAuthSuccess()
      PreferencesTokenStub.stubStoreNinoSuccess()
      KeystoreStub.stubKeystoreSave(PaperlessPreferenceToken)
      PreferencesStub.newStubPaperlessInactiveWithUri()

      When("GET /preferences is called")
      val res = IncomeTaxSubscriptionFrontend.preferences()

      Then("Should return a SEE_OTHER with a re-direct location of choose paperless page")
      res should have(
        httpStatus(SEE_OTHER),
        redirectURI(testUrl)
      )
    }

    "where the user needs to be re-directed to set paperless options" in {
      Given("I setup the Wiremock stubs")
      AuthStub.stubAuthSuccess()
      PreferencesTokenStub.stubStoreNinoSuccess()
      KeystoreStub.stubKeystoreSave(PaperlessPreferenceToken)
      PreferencesStub.newStubPaperlessPreconditionFail()

      When("GET /preferences is called")
      val res = IncomeTaxSubscriptionFrontend.preferences()

      Then("Should return a SEE_OTHER using the redirect location returned in the response")
      res should have(
        httpStatus(SEE_OTHER),
        redirectURI(testUrl)
      )
    }

    "where the GET/preferences returns an error" in {
      Given("I setup the Wiremock stubs")
      AuthStub.stubAuthSuccess()
      PreferencesTokenStub.stubStoreNinoSuccess()
      KeystoreStub.stubKeystoreSave(PaperlessPreferenceToken)
      PreferencesStub.newStubPaperlessError()

      When("GET /preferences is called")
      val res = IncomeTaxSubscriptionFrontend.preferences()

      Then("Should return a INTERNAL_SERVER_ERROR")
      res should have(
        httpStatus(INTERNAL_SERVER_ERROR)
      )
    }

    "Where the user has already stored their NINO against a token when " in {

      Given("I setup the Wiremock stubs")
      AuthStub.stubAuthSuccess()
      KeystoreStub.stubKeystoreData(Map(PaperlessPreferenceToken -> JsString(testPaperlessPreferenceToken)))
      PreferencesStub.newStubPaperlessActivated()

      When("GET /preferences is called")
      val res = IncomeTaxSubscriptionFrontend.preferences()

      Then("Should return a SEE_OTHER with a re-direct location of the next page")
      res should have(
        httpStatus(SEE_OTHER),
        redirectURI(rentUkPropertyURI)
      )
    }

  }

  "GET /callback" should {

    "where the user has previously accepted paperless where optedIn is set to False and redirect location returned" in {
      Given("I setup the Wiremock stubs")
      AuthStub.stubAuthSuccess()
      PreferencesTokenStub.stubStoreNinoSuccess()
      KeystoreStub.stubKeystoreSave(PaperlessPreferenceToken)
      PreferencesStub.newStubPaperlessInactiveWithUri()

      When("GET /callback is called")
      val res = IncomeTaxSubscriptionFrontend.callback()

      Then("Should return a SEE_OTHER with a re-direct location of paperless error page")
      res should have(
        httpStatus(SEE_OTHER),
        redirectURI(errorPreferencesURI)
      )
    }
  }

  "GET /paperless-error" should {

    "where the GET /paperless-error is called" in {
      Given("I setup the Wiremock stubs")
      AuthStub.stubAuthSuccess()

      When("GET /paperless-error is called")
      val res = IncomeTaxSubscriptionFrontend.paperlessError()

      Then("Should return a OK status with the paperless error page")
      res should have(
        httpStatus(OK),
        pageTitle(Messages("preferences_callback.title"))
      )
    }
  }

  "POST /paperless-error" should {

    "where the POST /paperless-error is called" in {
      Given("I setup the Wiremock stubs")
      AuthStub.stubAuthSuccess()

      When("POST /paperless-error is called")
      val res = IncomeTaxSubscriptionFrontend.submitPaperlessError(Map(ITSASessionKeys.PreferencesRedirectUrl -> choosePaperlessURI))

      Then("Should return a SEE_OTHER with a re-direct location of choose paperless page")
      res should have(
        httpStatus(SEE_OTHER),
        redirectURI(choosePaperlessURI)
      )
    }
  }
}