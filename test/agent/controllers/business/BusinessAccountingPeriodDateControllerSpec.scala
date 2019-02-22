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

import agent.assets.MessageLookup
import agent.controllers.AgentControllerBaseSpec
import agent.forms.AccountingPeriodDateForm
import agent.services.mocks.MockKeystoreService
import agent.utils.TestModels._
import core.config.featureswitch.{FeatureSwitching}
import core.models.DateModel
import incometax.business.models.AccountingPeriodModel
import incometax.util.AccountingPeriodUtil
import org.jsoup.Jsoup
import play.api.http.Status
import play.api.mvc.{Action, AnyContent}
import play.api.test.FakeRequest
import play.api.test.Helpers._

class BusinessAccountingPeriodDateControllerSpec extends AgentControllerBaseSpec
  with MockKeystoreService with FeatureSwitching {

  override val controllerName: String = "BusinessAccountingPeriodDateController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "showSummary" -> TestBusinessAccountingPeriodController.show(isEditMode = false),
    "submitSummary" -> TestBusinessAccountingPeriodController.submit(isEditMode = false)
  )

  object TestBusinessAccountingPeriodController extends BusinessAccountingPeriodDateController(
    MockBaseControllerConfig,
    messagesApi,
    MockKeystoreService,
    mockAuthService
  )

  "show" when {
    "it is not the current period" should {
      "return the correct view" in {
        lazy val result = TestBusinessAccountingPeriodController.show(isEditMode = false)(subscriptionRequest)

        // required for backurl
        setupMockKeystore(fetchAccountingPeriodDate = None, fetchAccountingPeriodPrior = testAccountingPeriodPriorCurrent)

        status(result) must be(Status.OK)

        await(result)
        verifyKeystore(fetchAccountingPeriodDate = 1, saveAccountingPeriodDate = 0)


        val document = Jsoup.parse(contentAsString(result))
        document.select("h1").text mustBe MessageLookup.AccountingPeriod.heading_current

      }
    }
    "it is the current period" should {
      "return the correct view" in {
        lazy val result = TestBusinessAccountingPeriodController.show(isEditMode = false)(subscriptionRequest)

        // required for backurl
        setupMockKeystore(fetchAccountingPeriodDate = None, fetchAccountingPeriodPrior = testAccountingPeriodPriorNext)

        status(result) must be(Status.OK)

        await(result)
        verifyKeystore(fetchAccountingPeriodDate = 1, saveAccountingPeriodDate = 0, fetchAccountingPeriodPrior = 2)


        val document = Jsoup.parse(contentAsString(result))
        document.select("h1").text mustBe MessageLookup.AccountingPeriod.heading_next
      }
    }
  }

  "Calling the submitAccountingPeriod action of the BusinessAccountingPeriodDate with an authorised user and a valid submission" should {
    val testAccountingPeriodDates = AccountingPeriodModel(DateModel dateConvert AccountingPeriodDateForm.minStartDate, AccountingPeriodUtil.getCurrentTaxYearEndDate)
    val testAccountingPeriodDatesDifferentTaxYear = AccountingPeriodModel(DateModel dateConvert AccountingPeriodDateForm.minStartDate, AccountingPeriodUtil.getCurrentTaxYearEndDate.plusYears(1))

    def callShow(isEditMode: Boolean, accountingPeriod: AccountingPeriodModel = testAccountingPeriodDates) = TestBusinessAccountingPeriodController.submit(isEditMode = isEditMode)(subscriptionRequest
      .post(AccountingPeriodDateForm.accountingPeriodDateForm, accountingPeriod))

    "When it is not in edit mode" when {
      "the tax year remained the same" should {
        s"return a redirect status (SEE_OTHER - 303) but do not update terms" in {
          setupMockKeystore(
            fetchAll = testCacheMap(
              accountingPeriodDate = Some(testAccountingPeriodDates)
            ),
            fetchAccountingPeriodPrior = testAccountingPeriodPriorNext
          )
          setupMockKeystore(fetchAccountingPeriodDate = testAccountingPeriodDates)

          val goodRequest = callShow(isEditMode = false)

          status(goodRequest) must be(Status.SEE_OTHER)
          redirectLocation(goodRequest) mustBe Some(agent.controllers.business.routes.BusinessNameController.show().url)

          await(goodRequest)
          verifyKeystore(saveAccountingPeriodDate = 1, saveTerms = 0)
        }
      }

      "the tax year changed" should {
        s"return a redirect status (SEE_OTHER - 303) and update terms" in {
          setupMockKeystore(
            fetchAll = testCacheMap(
              accountingPeriodDate = Some(testAccountingPeriodDatesDifferentTaxYear)
            ),
            fetchAccountingPeriodPrior = testAccountingPeriodPriorNext
          )

          val goodRequest = callShow(isEditMode = false)

          status(goodRequest) must be(Status.SEE_OTHER)
          redirectLocation(goodRequest) mustBe Some(agent.controllers.business.routes.BusinessNameController.show().url)

          await(goodRequest)
          verifyKeystore(saveAccountingPeriodDate = 1, saveTerms = 1)
        }
      }
    }

    "When it is in edit mode" should {
      "tax year remains the same" should {
        "return a redirect status (SEE_OTHER - 303)" in {
          setupMockKeystore(
            fetchAll = testCacheMap(
              accountingPeriodDate = Some(testAccountingPeriodDates)
            ),
            fetchAccountingPeriodPrior = testAccountingPeriodPriorNext
          )

          val goodRequest = callShow(isEditMode = true)

          status(goodRequest) must be(Status.SEE_OTHER)
          redirectLocation(goodRequest) mustBe Some(agent.controllers.routes.CheckYourAnswersController.show().url)

          await(goodRequest)
          verifyKeystore(saveAccountingPeriodDate = 1)
        }
      }

      "tax year changes" when {
        "the client can report" should {
          "return a redirect status (SEE_OTHER - 303)" in {
            setupMockKeystore(
              fetchAll = testCacheMap(
                accountingPeriodDate = Some(testAccountingPeriodDatesDifferentTaxYear)
              ),
              fetchAccountingPeriodPrior = testAccountingPeriodPriorNext
            )

            val goodRequest = callShow(isEditMode = true)

            status(goodRequest) must be(Status.SEE_OTHER)
            redirectLocation(goodRequest) mustBe Some(agent.controllers.routes.TermsController.show(editMode = true).url)

            await(goodRequest)
            verifyKeystore(saveAccountingPeriodDate = 1)
          }
        }
      }
    }
  }

  "Calling the submitAccountingPeriod action of the BusinessAccountingPeriodDate with an authorised user and invalid submission" should {
    lazy val badrequest = TestBusinessAccountingPeriodController.submit(isEditMode = false)(subscriptionRequest)

    "return a bad request status (400)" in {
      // required for backurl
      setupMockKeystore(fetchIncomeSource = testIncomeSourceBusiness, fetchAccountingPeriodPrior = testAccountingPeriodPriorCurrent)

      status(badrequest) must be(Status.BAD_REQUEST)

      await(badrequest)
      verifyKeystore(fetchAccountingPeriodDate = 0, saveAccountingPeriodDate = 0)
    }
  }

  "The back url when the user is submitting details for current period" should {
    s"point to ${agent.controllers.business.routes.BusinessAccountingPeriodPriorController.show().url}" in {
      setupMockKeystore(fetchAccountingPeriodPrior = testAccountingPeriodPriorCurrent)
      await(TestBusinessAccountingPeriodController.backUrl(isEditMode = false)(FakeRequest())) mustBe agent.controllers.business.routes.BusinessAccountingPeriodPriorController.show().url
      verifyKeystore(fetchAccountingPeriodPrior = 1)
    }
  }

  "The back url when the user is submitting details for next period" should {
    s"point to ${agent.controllers.business.routes.RegisterNextAccountingPeriodController.show().url}" in {
      setupMockKeystore(fetchAccountingPeriodPrior = testAccountingPeriodPriorNext)
      await(TestBusinessAccountingPeriodController.backUrl(isEditMode = false)(FakeRequest())) mustBe agent.controllers.business.routes.RegisterNextAccountingPeriodController.show().url
      verifyKeystore(fetchAccountingPeriodPrior = 1)
    }
  }
  "The back url when in edit mode" should {
    s"point to ${agent.controllers.routes.CheckYourAnswersController.show().url}" in {
      await(TestBusinessAccountingPeriodController.backUrl(isEditMode = true)(FakeRequest())) mustBe agent.controllers.routes.CheckYourAnswersController.show().url
    }
  }

  authorisationTests()
}