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

import _root_.agent.helpers.ComponentSpecBase
import _root_.agent.helpers.IntegrationTestConstants.testMTDID
import _root_.agent.helpers.servicemocks.{AuthStub, KeystoreStub}
import play.api.http.Status.{NOT_FOUND, OK}
import play.api.libs.json.Json

class ConfirmationControllerISpec extends ComponentSpecBase {

  "GET /confirmation" when {
    s"There is ${ITSASessionKeys.MTDITID} in session" should {
      "call subscription on the back end service" in {
        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubKeystoreData(Map("MtditId" -> Json.toJson(testMTDID)))

        When("I call GET /confirmation")
        val res = IncomeTaxSubscriptionFrontend.showConfirmation(hasSubmitted = true)

        Then("The result should have a status of OK and display the confirmation page")
        res should have(
          httpStatus(OK)
        )
      }
    }

    s"There is not ${ITSASessionKeys.MTDITID} in session" should {
      "call subscription on the back end service" in {
        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()

        When("I call GET /confirmation")
        val res = IncomeTaxSubscriptionFrontend.showConfirmation(hasSubmitted = false)

        Then("The result should have a status of NOT_FOUND")
        res should have(
          httpStatus(NOT_FOUND)
        )
      }
    }
  }
}
