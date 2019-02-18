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

import agent.audit.Logging
import agent.forms.OtherIncomeForm
import agent.services.mocks.MockKeystoreService
import agent.utils.TestModels
import core.models.No
import org.jsoup.Jsoup
import play.api.http.Status
import play.api.mvc.{Action, AnyContent}
import play.api.test.FakeRequest
import play.api.test.Helpers._

class OtherIncomeErrorControllerSpec extends AgentControllerBaseSpec with MockKeystoreService {

  override val controllerName: String = "OtherIncomeErrorController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map()

  object TestOtherIncomeErrorController extends OtherIncomeErrorController()(
    MockBaseControllerConfig,
    stubMCC,
    MockKeystoreService,
    mockAuthService,
    app.injector.instanceOf[Logging]
  )

  "Calling the showOtherIncomeError action of the OtherIncomeErrorController" should {

    lazy val result = TestOtherIncomeErrorController.show(FakeRequest())
    lazy val document = Jsoup.parse(contentAsString(result))

    "return 200" in {
      status(result) must be(Status.OK)
    }

    "return HTML" in {
      contentType(result) must be(Some("text/html"))
      charset(result) must be(Some("utf-8"))
    }

  }

  "Calling the submitOtherIncomeError action of the OtherIncomeError controller with an authorised user" should {

    def callSubmit = TestOtherIncomeErrorController.submit(subscriptionRequest
      .post(OtherIncomeForm.otherIncomeForm, No))

    "return a redirect status (SEE_OTHER - 303)" in {

      setupMockKeystore(fetchIncomeSource = TestModels.testIncomeSourceBusiness)

      val goodRequest = callSubmit

      status(goodRequest) must be(Status.SEE_OTHER)

      await(goodRequest)
      verifyKeystore(fetchIncomeSource = 1)
    }

    s"redirect to '${agent.controllers.business.routes.BusinessAccountingPeriodPriorController.show().url}' on the business journey" in {

      setupMockKeystore(fetchIncomeSource = TestModels.testIncomeSourceBusiness)

      val goodRequest = callSubmit

      redirectLocation(goodRequest) mustBe Some(agent.controllers.business.routes.BusinessAccountingPeriodPriorController.show().url)

      await(goodRequest)
      verifyKeystore(fetchIncomeSource = 1)
    }

    s"redirect to '${agent.controllers.routes.TermsController.show().url}' on the property journey" in {

      setupMockKeystore(fetchIncomeSource = TestModels.testIncomeSourceProperty)

      val goodRequest = callSubmit

      redirectLocation(goodRequest) mustBe Some(agent.controllers.routes.TermsController.show().url)

      await(goodRequest)
      verifyKeystore(fetchIncomeSource = 1)
    }

    s"redirect to '${agent.controllers.business.routes.BusinessAccountingPeriodPriorController.show().url}' on the both journey" in {

      setupMockKeystore(fetchIncomeSource = TestModels.testIncomeSourceBoth)

      val goodRequest = callSubmit

      redirectLocation(goodRequest) mustBe Some(agent.controllers.business.routes.BusinessAccountingPeriodPriorController.show().url)

      await(goodRequest)
      verifyKeystore(fetchIncomeSource = 1)
    }

  }

  authorisationTests()
}

