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
import agent.utils.TestConstants
import agent.utils.TestModels._
import core.config.featureswitch.{AgentPropertyCashOrAccruals, AgentTaxYear, EligibilityPagesFeature, FeatureSwitching}
import core.models.DateModel
import core.services.mocks.MockAccountingPeriodService
import incometax.business.models.AccountingPeriodModel
import incometax.util.CurrentDateProvider
import org.jsoup.Jsoup
import play.api.mvc.{Action, AnyContent}
import play.api.test.FakeRequest
import play.api.test.Helpers._

class BusinessAccountingPeriodDateControllerSpec extends AgentControllerBaseSpec
  with MockKeystoreService with FeatureSwitching with MockAccountingPeriodService {

  override def beforeEach(): Unit = {
    super.beforeEach()
    disable(EligibilityPagesFeature)
    disable(AgentPropertyCashOrAccruals)
    disable(AgentTaxYear)
  }

  override val controllerName: String = "BusinessAccountingPeriodDateController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "showSummary" -> TestBusinessAccountingPeriodController.show(isEditMode = false),
    "submitSummary" -> TestBusinessAccountingPeriodController.submit(isEditMode = false)
  )

  object DateProvider extends CurrentDateProvider

  object TestBusinessAccountingPeriodController extends BusinessAccountingPeriodDateController(
    MockBaseControllerConfig,
    messagesApi,
    MockKeystoreService,
    mockAuthService,
    mockAccountingPeriodService,
    DateProvider
  )

  "show" when {
    "it is not the current period" should {
      "return the correct view" in {
        setupMockKeystore(fetchAccountingPeriodDate = None, fetchAccountingPeriodPrior = testAccountingPeriodPriorCurrent)
        lazy val result = await(TestBusinessAccountingPeriodController.show(isEditMode = false)(subscriptionRequest))

        status(result) mustBe OK
        verifyKeystore(fetchAccountingPeriodDate = 1, saveAccountingPeriodDate = 0)

        val document = Jsoup.parse(contentAsString(result))
        document.select("h1").text mustBe MessageLookup.AccountingPeriod.heading
      }
    }
    "it is the current period" should {
      "return the correct view" in {
        setupMockKeystore(fetchAccountingPeriodDate = None, fetchAccountingPeriodPrior = testAccountingPeriodPriorNext)
        lazy val result = await(TestBusinessAccountingPeriodController.show(isEditMode = false)(subscriptionRequest))

        status(result) mustBe OK
        verifyKeystore(fetchAccountingPeriodDate = 1, saveAccountingPeriodDate = 0, fetchAccountingPeriodPrior = 2)

        val document = Jsoup.parse(contentAsString(result))
        document.select("h1").text mustBe MessageLookup.AccountingPeriod.heading
      }
    }
  }

  "Calling the submitAccountingPeriod action of the BusinessAccountingPeriodDate with an authorised user and a valid submission" when {
    val testAccountingPeriodDates = AccountingPeriodModel(DateModel dateConvert TestConstants.minStartDate, DateModel dateConvert TestConstants.minStartDate.plusYears(1))
    val testAccountingPeriodDatesDifferentTaxYear = AccountingPeriodModel(DateModel dateConvert TestConstants.minStartDate, DateModel dateConvert TestConstants.minStartDate.plusYears(2))

    def callShow(isEditMode: Boolean, accountingPeriod: AccountingPeriodModel = testAccountingPeriodDates) = TestBusinessAccountingPeriodController.submit(isEditMode = isEditMode)(subscriptionRequest
      .post(AccountingPeriodDateForm.accountingPeriodDateForm, accountingPeriod))

    "When it is ineligible dates" should {
      s"return a redirect status (SEE_OTHER - 303) to kickout page" in {
        setupMockKeystore(
          fetchAll = testCacheMap(
            accountingPeriodDate = Some(testAccountingPeriodDates)
          ),
          fetchAccountingPeriodPrior = testAccountingPeriodPriorCurrent
        )
        mockCheckEligibleAccountingPeriod(TestConstants.minStartDate, TestConstants.minStartDate.plusYears(1))(false)

        val goodRequest = callShow(isEditMode = false)

        status(goodRequest) mustBe SEE_OTHER
        redirectLocation(goodRequest) mustBe Some(agent.controllers.eligibility.routes.NotEligibleForIncomeTaxController.show().url)
      }
    }
    "When it is not in edit mode and is eligible" when {
      "the tax year remained the same" should {
        s"return a redirect status (SEE_OTHER - 303) but do not update terms" in {
          setupMockKeystore(
            fetchAll = testCacheMap(
              accountingPeriodDate = Some(testAccountingPeriodDates)
            ),
            fetchAccountingPeriodPrior = testAccountingPeriodPriorNext
          )
          setupMockKeystore(fetchAccountingPeriodDate = testAccountingPeriodDates)
          mockCheckEligibleAccountingPeriod(TestConstants.minStartDate, TestConstants.minStartDate.plusYears(1))(true)

          val goodRequest = await(callShow(isEditMode = false))

          status(goodRequest) mustBe SEE_OTHER
          redirectLocation(goodRequest) mustBe Some(agent.controllers.business.routes.BusinessAccountingMethodController.show().url)
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
          mockCheckEligibleAccountingPeriod(TestConstants.minStartDate, TestConstants.minStartDate.plusYears(1))(true)

          val goodRequest = await(callShow(isEditMode = false))

          status(goodRequest) mustBe SEE_OTHER
          redirectLocation(goodRequest) mustBe Some(agent.controllers.business.routes.BusinessAccountingMethodController.show().url)
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
          mockCheckEligibleAccountingPeriod(TestConstants.minStartDate, TestConstants.minStartDate.plusYears(1))(true)


          val goodRequest = await(callShow(isEditMode = true))

          status(goodRequest) mustBe SEE_OTHER
          redirectLocation(goodRequest) mustBe Some(agent.controllers.routes.CheckYourAnswersController.show().url)
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
            mockCheckEligibleAccountingPeriod(TestConstants.minStartDate, TestConstants.minStartDate.plusYears(1))(true)


            val goodRequest = await(callShow(isEditMode = true))

            status(goodRequest) mustBe SEE_OTHER
            redirectLocation(goodRequest) mustBe Some(agent.controllers.routes.TermsController.show(editMode = true).url)
            verifyKeystore(saveAccountingPeriodDate = 1)
          }
        }
      }
    }
  }

  "Calling the submitAccountingPeriod action of the BusinessAccountingPeriodDate with an authorised user and invalid submission" should {
    lazy val badrequest = await(TestBusinessAccountingPeriodController.submit(isEditMode = false)(subscriptionRequest))

    "return a bad request status (400)" in {
      // required for backurl
      setupMockKeystore(fetchIncomeSource = testIncomeSourceBusiness, fetchAccountingPeriodPrior = testAccountingPeriodPriorCurrent)

      status(badrequest) mustBe BAD_REQUEST
      verifyKeystore(fetchAccountingPeriodDate = 0, saveAccountingPeriodDate = 0)
    }
  }

  "The back url when it is not in edit mode" when {
    "FS Agent tax Year is enabled" when {
      "the user is submitting details for tax period" should {
        s"point to ${agent.controllers.business.routes.MatchTaxYearController.show().url}" in {
          enable(AgentTaxYear)
          await(TestBusinessAccountingPeriodController.backUrl(isEditMode = false)(FakeRequest())) mustBe agent.controllers.business.routes.MatchTaxYearController.show().url
        }
      }
    }

    "FS Agent tax Year is disabled" when {
      "the user is submitting details for tax period" when {
        "User selected YES on the Accounting Period Prior page" should {
          s"point to ${agent.controllers.business.routes.RegisterNextAccountingPeriodController.show().url}" in {
            setupMockKeystore(fetchAccountingPeriodPrior = testAccountingPeriodPriorNext)
            await(TestBusinessAccountingPeriodController.backUrl(isEditMode = false)(FakeRequest())) mustBe agent.controllers.business.routes.RegisterNextAccountingPeriodController.show().url
            verifyKeystore(fetchAccountingPeriodPrior = 1)
          }
        }

        "the user is submitting details for tax period" when {
          "User selected No on the Accounting Period Prior page" should {
            s"point to ${agent.controllers.business.routes.RegisterNextAccountingPeriodController.show().url}" in {
              setupMockKeystore(fetchAccountingPeriodPrior = testAccountingPeriodPriorNext)
              await(TestBusinessAccountingPeriodController.backUrl(isEditMode = false)(FakeRequest())) mustBe agent.controllers.business.routes.RegisterNextAccountingPeriodController.show().url
              verifyKeystore(fetchAccountingPeriodPrior = 1)
            }
          }
        }
      }

      "The back url when in edit mode" should {
        s"point to ${agent.controllers.routes.CheckYourAnswersController.show().url}" in {
          await(TestBusinessAccountingPeriodController.backUrl(isEditMode = true)(FakeRequest())) mustBe agent.controllers.routes.CheckYourAnswersController.show().url
        }
      }

      authorisationTests()
    }
  }
}