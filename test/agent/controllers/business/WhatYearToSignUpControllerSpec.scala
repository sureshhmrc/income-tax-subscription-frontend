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

package agent.controllers.business

import agent.forms.AccountingYearForm
import agent.models.AccountingYearModel
import agent.services.mocks.MockKeystoreService
import core.config.MockConfig
import core.config.featureswitch.FeatureSwitching
import agent.controllers.AgentControllerBaseSpec
import core.models.Current
import core.services.mocks.MockAccountingPeriodService
import play.api.http.Status
import play.api.mvc.{Action, AnyContent}
import play.api.test.Helpers._

class WhatYearToSignUpControllerSpec extends AgentControllerBaseSpec
  with MockKeystoreService
  with MockAccountingPeriodService
  with FeatureSwitching {

  override val controllerName: String = "WhatYearToSignUpMethod"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "show" -> TestWhatYearToSignUpController.show(isEditMode = false),
    "submit" -> TestWhatYearToSignUpController.submit(isEditMode = false)
  )

  object TestWhatYearToSignUpController extends WhatYearToSignUpController(
    MockBaseControllerConfig,
    messagesApi,
    MockKeystoreService,
    mockAuthService,
    MockConfig,
    mockAccountingPeriodService
  )

  "show" should {
    "display the What Year To Sign Up view with pre-saved tax year option and return OK (200)" when {
      "there is a pre-saved tax year option in keystore" in {

        lazy val result = await(TestWhatYearToSignUpController.show(isEditMode = false)(subscriptionRequest))

        setupMockKeystore(
          fetchWhatYearToSignUp = Some(AccountingYearModel(Current))
        )

        status(result) must be(Status.OK)

        verifyKeystore(fetchWhatYearToSignUp = 1)

      }
    }

    "display the What Year To Sign Up view with empty form and return OK (200)" when {
      "there is a no pre-saved tax year option in keystore" in {

        lazy val result = await(TestWhatYearToSignUpController.show(isEditMode = false)(subscriptionRequest))

        setupMockKeystore(
          fetchWhatYearToSignUp = None
        )

        status(result) must be(Status.OK)

        verifyKeystore(fetchWhatYearToSignUp = 1)

      }
    }
  }


  "submit" should {

    def callShow(isEditMode: Boolean) = TestWhatYearToSignUpController.submit(isEditMode = isEditMode)(
      subscriptionRequest.post(AccountingYearForm.accountingYearForm, AccountingYearModel(Current))
    )

    def callShowWithErrorForm(isEditMode: Boolean) = TestWhatYearToSignUpController.submit(isEditMode = isEditMode)(
      subscriptionRequest
    )

    "When it is not in edit mode" should {
      "return a redirect status (SEE_OTHER - 303)" in {
        setupMockKeystoreSaveFunctions()
        val goodRequest = callShow(isEditMode = false)

        status(goodRequest) must be(Status.SEE_OTHER)

        await(goodRequest)
        verifyKeystore(saveWhatYearToSignUp = 1)
      }

      "redirect to business accounting period page" in {
        setupMockKeystoreSaveFunctions()

        val goodRequest = callShow(isEditMode = false)

        redirectLocation(goodRequest) mustBe Some(agent.controllers.business.routes.BusinessAccountingMethodController.show().url)

        await(goodRequest)
        verifyKeystore(saveWhatYearToSignUp = 1)
      }

    }

    "When it is in edit mode" should {
      "return a redirect status (SEE_OTHER - 303)" in {
        setupMockKeystoreSaveFunctions()

        val goodRequest = callShow(isEditMode = true)

        status(goodRequest) must be(Status.SEE_OTHER)

        await(goodRequest)
        verifyKeystore(saveWhatYearToSignUp = 1)
      }

      "redirect to checkYourAnswer page" in {
        setupMockKeystoreSaveFunctions()

        val goodRequest = callShow(isEditMode = true)

        redirectLocation(goodRequest) mustBe Some(agent.controllers.routes.CheckYourAnswersController.show().url)

        await(goodRequest)
        verifyKeystore(saveWhatYearToSignUp = 1)

      }
    }

    "when there is an invalid submission with an error form" should {
      "return bad request status (400)" in {

        val badRequest = callShowWithErrorForm(isEditMode = false)

        status(badRequest) must be(Status.BAD_REQUEST)

      }
    }

    "The back url is not in edit mode" when {
      "the user click back url" should {
        "redirect to Match Tax Year page" in {
          TestWhatYearToSignUpController.backUrl(isEditMode = false) mustBe
            agent.controllers.business.routes.MatchTaxYearController.show().url
        }
      }
    }


    "The back url is in edit mode" when {
      "the user click back url" should {
        "redirect to check your answer page" in {
          TestWhatYearToSignUpController.backUrl(isEditMode = true) mustBe
            agent.controllers.routes.CheckYourAnswersController.show().url
        }
      }
    }
  }
}
