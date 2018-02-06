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

package incometax.business.controllers

import assets.MessageLookup
import core.config.MockConfig
import core.config.featureswitch.{FeatureSwitching, TaxYearDeferralFeature}
import core.controllers.ControllerBaseSpec
import core.models.DateModel
import core.services.mocks.MockKeystoreService
import core.utils.TestModels
import core.utils.TestModels.testCacheMapCustom
import incometax.business.forms.AccountingPeriodDateForm
import incometax.business.models.AccountingPeriodModel
import incometax.incomesource.services.mocks.MockCurrentTimeService
import org.jsoup.Jsoup
import play.api.http.Status
import play.api.mvc.{Action, AnyContent}
import play.api.test.Helpers._

class BusinessAccountingPeriodDateControllerSpec extends ControllerBaseSpec
  with MockKeystoreService
  with MockCurrentTimeService
  with FeatureSwitching {

  override val controllerName: String = "BusinessAccountingPeriodDateController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "show" -> TestBusinessAccountingPeriodController.show(isEditMode = false, editMatch = false),
    "submit" -> TestBusinessAccountingPeriodController.submit(isEditMode = false, editMatch = false)
  )

  override def beforeEach(): Unit = {
    super.beforeEach()
    disable(TaxYearDeferralFeature)
  }

  override def afterEach(): Unit = {
    super.afterEach()
    disable(TaxYearDeferralFeature)
  }

  def createTestBusinessAccountingPeriodController(setEnableRegistration: Boolean): BusinessAccountingPeriodDateController =
    new BusinessAccountingPeriodDateController(
      mockBaseControllerConfig(new MockConfig {
        override val enableRegistration = setEnableRegistration
      }),
      messagesApi,
      MockKeystoreService,
      mockAuthService,
      mockCurrentTimeService
    )

  lazy val TestBusinessAccountingPeriodController: BusinessAccountingPeriodDateController =
    createTestBusinessAccountingPeriodController(setEnableRegistration = false)

  val testAccountingPeriodDates2018 = AccountingPeriodModel(DateModel dateConvert AccountingPeriodDateForm.minStartDate, DateModel("5", "4", "2018"))
  val testAccountingPeriodDates2019 = AccountingPeriodModel(DateModel dateConvert AccountingPeriodDateForm.minStartDate, DateModel("5", "4", "2019"))


  "When the user is in the sign up journey, BusinessAccountingPeriodDateController" when {

    lazy val request = subscriptionRequest

    "Calling the showAccountingPeriod action of the BusinessAccountingPeriodDate with an authorised user on a signup journey" should {

      lazy val result = TestBusinessAccountingPeriodController.show(isEditMode = false, editMatch = false)(request)

      "return ok (200)" in {
        setupMockKeystore(fetchAccountingPeriodDate = None)

        status(result) must be(Status.OK)

        await(result)
        verifyKeystore(fetchAccountingPeriodDate = 1, saveAccountingPeriodDate = 0)

      }

      s"the rendered view should have the heading '${MessageLookup.AccountingPeriod.heading_signup}'" in {
        val document = Jsoup.parse(contentAsString(result))
        document.select("h1").text mustBe MessageLookup.AccountingPeriod.heading_signup
      }
    }

    "Calling the submit action of the BusinessAccountingPeriodDate with an authorised user and a valid submission" when {

      "tax year deferral is disabled" should {
        val testAccountingPeriodDates = AccountingPeriodModel(DateModel dateConvert AccountingPeriodDateForm.minStartDate, DateModel("5", "4", "2018"))
        val testAccountingPeriodDatesDifferentTaxYear = AccountingPeriodModel(DateModel dateConvert AccountingPeriodDateForm.minStartDate, DateModel("5", "4", "2019"))

        def callShow(isEditMode: Boolean) = TestBusinessAccountingPeriodController.submit(isEditMode = isEditMode, editMatch = false)(request
          .post(AccountingPeriodDateForm.accountingPeriodDateForm, testAccountingPeriodDates))

        "When it is not in edit mode" when {
          "the tax year remained the same" should {
            s"return a redirect status (SEE_OTHER - 303) and do not update terms" in {
              setupMockKeystore(
                fetchAll = testCacheMapCustom(
                  incomeSource = TestModels.testIncomeSourceBusiness,
                  matchTaxYear = TestModels.testMatchTaxYearNo, // required for backurl
                  accountingPeriodDate = testAccountingPeriodDates)
              )

              val goodRequest = callShow(isEditMode = false)

              status(goodRequest) must be(Status.SEE_OTHER)
              redirectLocation(goodRequest) mustBe Some(incometax.business.controllers.routes.BusinessAccountingMethodController.show().url)

              await(goodRequest)
              verifyKeystore(fetchAll = 1, saveAccountingPeriodDate = 1, saveTerms = 0)
            }
          }

          "the tax year changed" should {
            s"return a redirect status (SEE_OTHER - 303) and update terms" in {
              setupMockKeystore(
                fetchAll = testCacheMapCustom(
                  incomeSource = TestModels.testIncomeSourceBusiness,
                  matchTaxYear = TestModels.testMatchTaxYearNo, // required for backurl
                  accountingPeriodDate = testAccountingPeriodDatesDifferentTaxYear)
              )

              val goodRequest = callShow(isEditMode = false)

              status(goodRequest) must be(Status.SEE_OTHER)
              redirectLocation(goodRequest) mustBe Some(incometax.business.controllers.routes.BusinessAccountingMethodController.show().url)

              await(goodRequest)
              verifyKeystore(fetchAll = 1, saveAccountingPeriodDate = 1, saveTerms = 1)
            }
          }
        }

        "When it is in edit mode" when {
          "the tax year remained the same" should {
            s"return a redirect status (SEE_OTHER - 303) and redirect to '${incometax.subscription.controllers.routes.CheckYourAnswersController.show().url}" in {
              setupMockKeystore(
                fetchAll = testCacheMapCustom(
                  incomeSource = TestModels.testIncomeSourceBusiness,
                  matchTaxYear = TestModels.testMatchTaxYearNo, // required for backurl
                  accountingPeriodDate = testAccountingPeriodDates)
              )

              val goodRequest = callShow(isEditMode = true)

              status(goodRequest) must be(Status.SEE_OTHER)
              await(goodRequest)

              redirectLocation(goodRequest) mustBe Some(incometax.subscription.controllers.routes.CheckYourAnswersController.show().url)
              verifyKeystore(fetchAll = 1, saveAccountingPeriodDate = 1, saveTerms = 0)
            }
          }

          "the tax year changed" should {
            s"return a redirect status (SEE_OTHER - 303) and redirect to '${
              incometax.subscription.controllers.routes.TermsController.show(editMode = true).url
            }" in {
              setupMockKeystore(
                fetchAll = testCacheMapCustom(
                  incomeSource = TestModels.testIncomeSourceBusiness,
                  matchTaxYear = TestModels.testMatchTaxYearNo, // required for backurl
                  accountingPeriodDate = testAccountingPeriodDatesDifferentTaxYear)
              )

              val goodRequest = callShow(isEditMode = true)

              status(goodRequest) must be(Status.SEE_OTHER)
              await(goodRequest)

              redirectLocation(goodRequest) mustBe Some(incometax.subscription.controllers.routes.CheckYourAnswersController.show().url)
              verifyKeystore(fetchAll = 1, saveAccountingPeriodDate = 1, saveTerms = 1)
            }
          }

        }
      }

      "tax year deferral is enabled but current tax year end is 2019 OR later" should {
        "When it is not in edit mode" when {
            s"return a redirect status (SEE_OTHER - 303)" in {
              enable(TaxYearDeferralFeature)

              setupMockKeystore(
                fetchAll = testCacheMapCustom(
                  incomeSource = TestModels.testIncomeSourceBusiness,
                  matchTaxYear = TestModels.testMatchTaxYearNo, // required for backurl
                  accountingPeriodDate = testAccountingPeriodDates2019)
              )

              mockGetTaxYearEnd(2019)

              val goodRequest =  TestBusinessAccountingPeriodController.submit(isEditMode = false, editMatch = false)(request
                .post(AccountingPeriodDateForm.accountingPeriodDateForm, testAccountingPeriodDates2019))

              status(goodRequest) must be(Status.SEE_OTHER)
              redirectLocation(goodRequest) mustBe Some(incometax.business.controllers.routes.BusinessAccountingMethodController.show().url)
          }
        }
      }

      "tax year deferral is enabled and the current tax end year is 2018" should {

        def callShow(isEditMode: Boolean)(submission: AccountingPeriodModel = testAccountingPeriodDates2019) =
          TestBusinessAccountingPeriodController.submit(isEditMode = isEditMode, editMatch = false)(
            request.post(AccountingPeriodDateForm.accountingPeriodDateForm, submission))

        "When it is not in edit mode" when {
          "the tax year remained the same" when {
            "end tax year is 2017 - 2018" should {
              s"return a redirect status (SEE_OTHER - 303) and do not update terms" in {
                enable(TaxYearDeferralFeature)

                setupMockKeystore(
                  fetchAll = testCacheMapCustom(
                    incomeSource = TestModels.testIncomeSourceBusiness,
                    matchTaxYear = TestModels.testMatchTaxYearNo, // required for backurl
                    accountingPeriodDate = testAccountingPeriodDates2018)
                )

                mockGetTaxYearEnd(2018)

                val goodRequest = callShow(isEditMode = false)(testAccountingPeriodDates2018)

                status(goodRequest) must be(Status.SEE_OTHER)
                redirectLocation(goodRequest) mustBe Some(incometax.incomesource.controllers.routes.CannotReportYetController.show().url)

                await(goodRequest)
                verifyKeystore(fetchAll = 1, saveAccountingPeriodDate = 1, saveTerms = 0)
              }
            }

            "the tax year is 2018 - 2019 and the Income type is both Property and Sole Trader" should {
              s"return a redirect status (SEE_OTHER - 303) but do not update terms" in {
                enable(TaxYearDeferralFeature)

                setupMockKeystore(
                  fetchAll = testCacheMapCustom(
                    incomeSource = TestModels.testIncomeSourceBoth,
                    accountingPeriodDate = None)
                )

                mockGetTaxYearEnd(2018)

                val goodRequest = callShow(isEditMode = false)(testAccountingPeriodDates2019)

                status(goodRequest) must be(Status.SEE_OTHER)
                redirectLocation(goodRequest) mustBe Some(incometax.incomesource.controllers.routes.CannotReportYetController.show().url)

                await(goodRequest)
                verifyKeystore(fetchAll = 1, saveAccountingPeriodDate = 1, saveTerms = 0)
              }
            }

            "end tax year is after 2017 - 2018" should {
              s"return a redirect status (SEE_OTHER - 303) and do not update terms" in {
                enable(TaxYearDeferralFeature)

                setupMockKeystore(
                  fetchAll = testCacheMapCustom(
                    incomeSource = TestModels.testIncomeSourceBusiness,
                    matchTaxYear = TestModels.testMatchTaxYearNo, // required for backurl
                    accountingPeriodDate = testAccountingPeriodDates2019)
                )

                mockGetTaxYearEnd(2018)

                val goodRequest = callShow(isEditMode = false)(testAccountingPeriodDates2019)

                status(goodRequest) must be(Status.SEE_OTHER)
                redirectLocation(goodRequest) mustBe Some(incometax.business.controllers.routes.BusinessAccountingMethodController.show().url)

                await(goodRequest)
                verifyKeystore(fetchAll = 1, saveAccountingPeriodDate = 1, saveTerms = 0)
              }
            }
          }

          "the tax year changed" when {
            "the new tax year is 2017 - 2018" should {
              s"return a redirect status (SEE_OTHER - 303) and update terms" in {
                enable(TaxYearDeferralFeature)

                setupMockKeystore(
                  fetchAll = testCacheMapCustom(
                    incomeSource = TestModels.testIncomeSourceBusiness,
                    matchTaxYear = TestModels.testMatchTaxYearNo, // required for backurl
                    accountingPeriodDate = testAccountingPeriodDates2019)
                )

                mockGetTaxYearEnd(2018)

                val goodRequest = callShow(isEditMode = false)(testAccountingPeriodDates2018)

                status(goodRequest) must be(Status.SEE_OTHER)
                redirectLocation(goodRequest) mustBe Some(incometax.incomesource.controllers.routes.CannotReportYetController.show().url)

                await(goodRequest)
                verifyKeystore(fetchAll = 1, saveAccountingPeriodDate = 1, saveTerms = 1)
              }
            }

            "the new tax year is after 2017 - 2018" should {
              s"return a redirect status (SEE_OTHER - 303) and update terms" in {
                enable(TaxYearDeferralFeature)

                setupMockKeystore(
                  fetchAll = testCacheMapCustom(
                    incomeSource = TestModels.testIncomeSourceBusiness,
                    matchTaxYear = TestModels.testMatchTaxYearNo, // required for backurl
                    accountingPeriodDate = testAccountingPeriodDates2018)
                )

                mockGetTaxYearEnd(2018)

                val goodRequest = callShow(isEditMode = false)(testAccountingPeriodDates2019)

                status(goodRequest) must be(Status.SEE_OTHER)
                redirectLocation(goodRequest) mustBe Some(incometax.business.controllers.routes.BusinessAccountingMethodController.show().url)

                await(goodRequest)
                verifyKeystore(fetchAll = 1, saveAccountingPeriodDate = 1, saveTerms = 1)
              }
            }
          }
        }

        "When it is in edit mode" when {

          "the newly entered tax year is 2017 - 2018 and" when {
            "the tax year remained the same" should {
              s"return a redirect status (SEE_OTHER - 303) and redirect to '${incometax.subscription.controllers.routes.CheckYourAnswersController.show().url}" in {
                enable(TaxYearDeferralFeature)
                setupMockKeystore(
                  fetchAll = testCacheMapCustom(
                    incomeSource = TestModels.testIncomeSourceBusiness,
                    matchTaxYear = TestModels.testMatchTaxYearNo, // required for backurl
                    accountingPeriodDate = testAccountingPeriodDates2018)
                )

                mockGetTaxYearEnd(2018)

                val goodRequest = callShow(isEditMode = true)(testAccountingPeriodDates2018)

                status(goodRequest) must be(Status.SEE_OTHER)
                await(goodRequest)

                redirectLocation(goodRequest) mustBe Some(incometax.subscription.controllers.routes.CheckYourAnswersController.show().url)
                verifyKeystore(fetchAll = 1, saveAccountingPeriodDate = 1, saveTerms = 0)
              }
            }

            "the tax year changed" should {
              s"return a redirect status (SEE_OTHER - 303) and redirect to '${
                incometax.incomesource.controllers.routes.CannotReportYetController.show(editMode = true).url
              }" in {
                enable(TaxYearDeferralFeature)

                setupMockKeystore(
                  fetchAll = testCacheMapCustom(
                    incomeSource = TestModels.testIncomeSourceBusiness,
                    matchTaxYear = TestModels.testMatchTaxYearNo, // required for backurl
                    accountingPeriodDate = testAccountingPeriodDates2019)
                )

                mockGetTaxYearEnd(2018)

                val goodRequest = callShow(isEditMode = true)(testAccountingPeriodDates2018)

                status(goodRequest) must be(Status.SEE_OTHER)
                await(goodRequest)

                redirectLocation(goodRequest) mustBe Some(incometax.incomesource.controllers.routes.CannotReportYetController.show(editMode = true).url)
                verifyKeystore(fetchAll = 1, saveAccountingPeriodDate = 1, saveTerms = 1)
              }
            }
          }
        }

        "the newly entered tax year is after 2017 - 2018 and" when {
          "the tax year remained the same" should {
            s"return a redirect status (SEE_OTHER - 303) and redirect to '${incometax.subscription.controllers.routes.CheckYourAnswersController.show().url}" in {
              enable(TaxYearDeferralFeature)

              setupMockKeystore(
                fetchAll = testCacheMapCustom(
                  incomeSource = TestModels.testIncomeSourceBusiness,
                  matchTaxYear = TestModels.testMatchTaxYearNo, // required for backurl
                  accountingPeriodDate = testAccountingPeriodDates2019)
              )

              mockGetTaxYearEnd(2018)

              val goodRequest = callShow(isEditMode = true)(testAccountingPeriodDates2019)

              status(goodRequest) must be(Status.SEE_OTHER)
              await(goodRequest)

              redirectLocation(goodRequest) mustBe Some(incometax.subscription.controllers.routes.CheckYourAnswersController.show().url)
              verifyKeystore(fetchAll = 1, saveAccountingPeriodDate = 1, saveTerms = 0)
            }
          }

          "the tax year changed" should {
            s"return a redirect status (SEE_OTHER - 303) and redirect to '${
              incometax.subscription.controllers.routes.TermsController.show(editMode = true).url
            }" in {
              enable(TaxYearDeferralFeature)

              setupMockKeystore(
                fetchAll = testCacheMapCustom(
                  incomeSource = TestModels.testIncomeSourceBusiness,
                  matchTaxYear = TestModels.testMatchTaxYearNo, // required for backurl
                  accountingPeriodDate = testAccountingPeriodDates2018)
              )

              mockGetTaxYearEnd(2018)

              val goodRequest = callShow(isEditMode = true)(testAccountingPeriodDates2019)

              status(goodRequest) must be(Status.SEE_OTHER)
              await(goodRequest)

              redirectLocation(goodRequest) mustBe Some(incometax.subscription.controllers.routes.CheckYourAnswersController.show().url)
              verifyKeystore(fetchAll = 1, saveAccountingPeriodDate = 1, saveTerms = 1)
            }

            s"return a redirect status (SEE_OTHER - 303) and redirect to '${
              incometax.incomesource.controllers.routes.CannotReportYetController.show(editMode = true).url
            } when the Income Type is BOTH" in {
              enable(TaxYearDeferralFeature)

              setupMockKeystore(
                fetchAll = testCacheMapCustom(
                  incomeSource = TestModels.testIncomeSourceBoth,
                  matchTaxYear = TestModels.testMatchTaxYearNo, // required for backurl
                  accountingPeriodDate = testAccountingPeriodDates2018)
              )

              mockGetTaxYearEnd(2018)

              val goodRequest = callShow(isEditMode = true)(testAccountingPeriodDates2019)

              status(goodRequest) must be(Status.SEE_OTHER)
              await(goodRequest)

              redirectLocation(goodRequest) mustBe Some(incometax.incomesource.controllers.routes.CannotReportYetController.show(editMode = true).url)
              verifyKeystore(fetchAll = 1, saveAccountingPeriodDate = 1, saveTerms = 1)
            }
          }
        }
      }
    }

    "Calling the submit action of the BusinessAccountingPeriodDate with an authorised user and invalid submission" should {
      lazy val badrequest = TestBusinessAccountingPeriodController.submit(isEditMode = false, editMatch = false)(request)

      "return a bad request status (400)" in {
        // required for backurl
        setupMockKeystore(
          fetchAll = testCacheMapCustom(
            incomeSource = TestModels.testIncomeSourceBusiness,
            matchTaxYear = TestModels.testMatchTaxYearNo)
        )

        status(badrequest) must be(Status.BAD_REQUEST)

        await(badrequest)
        verifyKeystore(fetchAll = 0, saveAccountingPeriodDate = 0)
      }
    }

    "The back url for linear journey" should {
      s"point to ${incometax.business.controllers.routes.MatchTaxYearController.show().url}" in {
        TestBusinessAccountingPeriodController.backUrl(isEditMode = false, editMatch = false)(request) mustBe incometax.business.controllers.routes.MatchTaxYearController.show().url
      }
    }

    "The back url for edit journey if editmatch is false" should {
      s"point to ${incometax.subscription.controllers.routes.CheckYourAnswersController.show().url}" in {
        TestBusinessAccountingPeriodController.backUrl(isEditMode = true, editMatch = false)(request) mustBe incometax.subscription.controllers.routes.CheckYourAnswersController.show().url
      }
    }

    "The back url for edit journey if editmatch is true" should {
      s"point to ${incometax.business.controllers.routes.MatchTaxYearController.show(editMode = true).url}" in {
        TestBusinessAccountingPeriodController.backUrl(isEditMode = true, editMatch = true)(request) mustBe incometax.business.controllers.routes.MatchTaxYearController.show(editMode = true).url
      }
    }

  }


  "When the user is in the registration journey, BusinessAccountingPeriodDateController" when {

    lazy val request = registrationRequest

    "Calling the showAccountingPeriod action of the BusinessAccountingPeriodDate with an authorised user on a registration journey" should {

      lazy val result = TestBusinessAccountingPeriodController.show(isEditMode = false, editMatch = false)(request)

      "return ok (200)" in {
        setupMockKeystore(fetchAccountingPeriodDate = None)

        status(result) must be(Status.OK)

        await(result)
        verifyKeystore(fetchAccountingPeriodDate = 1, saveAccountingPeriodDate = 0)

      }

      s"the rendered view should have the heading '${MessageLookup.AccountingPeriod.heading_registration}'" in {
        val document = Jsoup.parse(contentAsString(result))
        document.select("h1").text mustBe MessageLookup.AccountingPeriod.heading_registration
      }
    }
  }

  authorisationTests()
}