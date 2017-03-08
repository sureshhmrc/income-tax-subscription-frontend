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

package controllers.business

import auth._
import controllers.ControllerBaseSpec
import forms.AccountingPeriodForm
import models.{AccountingPeriodModel, DateModel}
import play.api.http.Status
import play.api.mvc.{Action, AnyContent}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.mocks.MockKeystoreService
import utils.TestModels

class BusinessAccountingPeriodControllerSpec extends ControllerBaseSpec
  with MockKeystoreService {

  override val controllerName: String = "BusinessAccountingPeriodController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "showSummary" -> TestBusinessAccountingPeriodController.showAccountingPeriod(isEditMode = false),
    "submitSummary" -> TestBusinessAccountingPeriodController.submitAccountingPeriod(isEditMode = false)
  )

  object TestBusinessAccountingPeriodController extends BusinessAccountingPeriodController(
    MockBaseControllerConfig,
    messagesApi,
    MockKeystoreService
  )

  "Calling the showAccountingPeriod action of the BusinessAccountingPeriod with an authorised user" should {

    lazy val result = TestBusinessAccountingPeriodController.showAccountingPeriod(isEditMode = false)(authenticatedFakeRequest())

    "return ok (200)" in {
      // required for backurl
      setupMockKeystore(fetchIncomeSource = TestModels.testIncomeSourceBusiness)

      setupMockKeystore(fetchAccountingPeriod = None)

      status(result) must be(Status.OK)

      await(result)
      verifyKeystore(fetchAccountingPeriod = 1, saveAccountingPeriod = 0)

    }
  }

  "Calling the submitAccountingPeriod action of the BusinessAccountingPeriod with an authorised user and a valid submission" should {

    def callShow(isEditMode: Boolean) = TestBusinessAccountingPeriodController.submitAccountingPeriod(isEditMode = isEditMode)(authenticatedFakeRequest()
      .post(AccountingPeriodForm.accountingPeriodForm, AccountingPeriodModel(DateModel("1", "4", "2017"), DateModel("1", "4", "2018"))))

    "When it is not in edit mode" should {
      "return a redirect status (SEE_OTHER - 303)" in {
        // required for backurl
        setupMockKeystore(fetchIncomeSource = TestModels.testIncomeSourceBusiness)

        val goodRequest = callShow(isEditMode = false)

        status(goodRequest) must be(Status.SEE_OTHER)

        await(goodRequest)
        verifyKeystore(fetchAccountingPeriod = 0, saveAccountingPeriod = 1)
      }

      s"redirect to '${controllers.business.routes.BusinessNameController.showBusinessName().url}'" in {
        // required for backurl
        setupMockKeystore(fetchIncomeSource = TestModels.testIncomeSourceBusiness)

        val goodRequest = callShow(isEditMode = false)

        redirectLocation(goodRequest) mustBe Some(controllers.business.routes.BusinessNameController.showBusinessName().url)

        await(goodRequest)
        verifyKeystore(fetchAccountingPeriod = 0, saveAccountingPeriod = 1)
      }
    }

    "When it is in edit mode" should {
      "return a redirect status (SEE_OTHER - 303)" in {
        // required for backurl
        setupMockKeystore(fetchIncomeSource = TestModels.testIncomeSourceBusiness)

        val goodRequest = callShow(isEditMode = true)

        status(goodRequest) must be(Status.SEE_OTHER)

        await(goodRequest)
        verifyKeystore(fetchAccountingPeriod = 0, saveAccountingPeriod = 1)
      }

      s"redirect to '${controllers.routes.SummaryController.showSummary().url}'" in {
        // required for backurl
        setupMockKeystore(fetchIncomeSource = TestModels.testIncomeSourceBusiness)

        val goodRequest = callShow(isEditMode = true)

        redirectLocation(goodRequest) mustBe Some(controllers.routes.SummaryController.showSummary().url)

        await(goodRequest)
        verifyKeystore(fetchAccountingPeriod = 0, saveAccountingPeriod = 1)
      }
    }
  }

  "Calling the submitAccountingPeriod action of the BusinessAccountingPeriod with an authorised user and invalid submission" should {
    lazy val badrequest = TestBusinessAccountingPeriodController.submitAccountingPeriod(isEditMode = false)(authenticatedFakeRequest())

    "return a bad request status (400)" in {
      // required for backurl
      setupMockKeystore(fetchIncomeSource = TestModels.testIncomeSourceBusiness)

      status(badrequest) must be(Status.BAD_REQUEST)

      await(badrequest)
      verifyKeystore(fetchAccountingPeriod = 0, saveAccountingPeriod = 0)
    }
  }

  "The back url" should {
    s"point to ${controllers.business.routes.BusinessIncomeTypeController.showBusinessIncomeType().url}" in {
      setupMockKeystore(fetchIncomeSource = TestModels.testIncomeSourceBusiness)
      TestBusinessAccountingPeriodController.backUrl mustBe controllers.routes.IncomeSourceController.showIncomeSource().url
    }
  }
    authorisationTests
  }