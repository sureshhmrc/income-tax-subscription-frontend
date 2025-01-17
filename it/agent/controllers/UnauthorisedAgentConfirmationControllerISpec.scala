/*
 * Copyright 2017 HM Revenue & Customs
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

import _root_.agent.helpers.ComponentSpecBase
import _root_.agent.helpers.IntegrationTestConstants.testMTDID
import _root_.agent.helpers.servicemocks.{AuthStub, KeystoreStub}
import core.config.featureswitch.{FeatureSwitching, UnauthorisedAgentFeature}
import play.api.http.Status.{NOT_FOUND, OK}
import play.api.libs.json.Json

class UnauthorisedAgentConfirmationControllerISpec extends ComponentSpecBase with FeatureSwitching {

  "GET /send-client-link" when {
    "the unauthorised agent feature switch is enabled" should {
      "return the confirmation page" in {
        enable(UnauthorisedAgentFeature)

        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()

        When("I call GET /send-client-link")
        val res = IncomeTaxSubscriptionFrontend.showUnauthorisedAgentConfirmation()

        Then("The result should have a status of OK and display the Unauthorised Agent confirmation page")
        res should have(
          httpStatus(OK)
        )
      }
    }
  }
}
