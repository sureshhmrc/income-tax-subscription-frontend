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

package incometax.business.controllers

import core.config.MockConfig
import core.config.featureswitch._
import core.controllers.ControllerBaseSpec
import core.models.{No, Yes, YesNo}
import core.services.mocks.MockKeystoreService
import core.utils.TestModels._
import incometax.business.forms.MatchTaxYearForm
import incometax.business.models.MatchTaxYearModel
import incometax.incomesource.services.mocks.MockCurrentTimeService
import org.jsoup.Jsoup
import play.api.http.Status
import play.api.mvc.{Action, AnyContent, Result}
import play.api.test.Helpers._

import scala.concurrent.Future

class MatchTaxYearControllerSpec extends ControllerBaseSpec
  with MockKeystoreService
  with MockCurrentTimeService
  with FeatureSwitching {

  override val controllerName: String = "MatchTaxYearController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "show" -> TestMatchTaxYearController.show(isEditMode = false),
    "submit" -> TestMatchTaxYearController.submit(isEditMode = false)
  )

  object TestMatchTaxYearController extends MatchTaxYearController(
    MockBaseControllerConfig,
    messagesApi,
    MockKeystoreService,
    mockAuthService,
    mockCurrentTimeService
  )

  object TestMatchTaxYearControllerFSOn extends MatchTaxYearController(
    mockBaseControllerConfig(new MockConfig {
      override val whatTaxYearToSignUpEnabled: Boolean = true
    }),
    messagesApi,
    MockKeystoreService,
    mockAuthService,
    mockCurrentTimeService
  )

  override def beforeEach(): Unit = {
    super.beforeEach()
  }

  override def afterEach(): Unit = {
    super.afterEach()
  }

  "Calling the show action of the MatchTaxYearController with an authorised user" should {

    def result: Future[Result] = {
      setupMockKeystore(
        fetchMatchTaxYear = None
      )
      TestMatchTaxYearController.show(isEditMode = false)(subscriptionRequest)
    }

    "return ok (200)" in {
      status(result) must be(Status.OK)
    }

    "retrieve one value from keystore" in {
      await(result)
      verifyKeystore(fetchMatchTaxYear = 1, saveMatchTaxYear = 0)
    }

    s"The back url should point to '${incometax.business.controllers.routes.BusinessNameController.show().url}'" in {
      val document = Jsoup.parse(contentAsString(result))
      document.select("#back").attr("href") mustBe incometax.business.controllers.routes.BusinessNameController.show().url
    }

  }


  "The back url" should {
    s"in linear journey for subscription points to ${incometax.business.controllers.routes.BusinessNameController.show().url}" in {
      TestMatchTaxYearController.backUrl(isEditMode = false)(subscriptionRequest) mustBe incometax.business.controllers.routes.BusinessNameController.show().url
    }
    s"in linear journey for registration points to ${incometax.business.controllers.routes.BusinessStartDateController.show().url}" in {
      TestMatchTaxYearController.backUrl(isEditMode = false)(registrationRequest) mustBe incometax.business.controllers.routes.BusinessStartDateController.show().url
    }
    s"edit mode points to ${incometax.subscription.controllers.routes.CheckYourAnswersController.show().url}" in {
      TestMatchTaxYearController.backUrl(isEditMode = true)(subscriptionRequest) mustBe incometax.subscription.controllers.routes.CheckYourAnswersController.show().url
    }
  }

  "Calling the submit action of the MatchTaxYearController with an authorised user and valid submission" when {

    def callSubmitCore(answer: YesNo, isEditMode: Boolean, whatTaxYearFs: Boolean = false): Future[Result] = {
      if (whatTaxYearFs) {
        TestMatchTaxYearControllerFSOn.submit(isEditMode)(subscriptionRequest.post(MatchTaxYearForm.matchTaxYearForm, MatchTaxYearModel(answer)))
      } else {
        TestMatchTaxYearController.submit(isEditMode)(subscriptionRequest.post(MatchTaxYearForm.matchTaxYearForm, MatchTaxYearModel(answer)))
      }
    }

    "Not in edit mode and " when {

      def callSubmit(answer: YesNo, featureSwitch: Boolean = false): Future[Result] = callSubmitCore(answer, isEditMode = false, whatTaxYearFs = featureSwitch)

      "Option 'Yes' is selected" when {
        "the what tax year to sign up feature switch is disabled" in {
          setupMockKeystore(fetchAll = None)

          val goodRequest = callSubmit(Yes)
          status(goodRequest) mustBe Status.SEE_OTHER
          redirectLocation(goodRequest).get mustBe incometax.business.controllers.routes.BusinessAccountingMethodController.show().url
          verifyKeystore(fetchAll = 1, saveMatchTaxYear = 1)
        }
        "the what tax year to sign up feature switch is enabled and the the user is business only" in {
          setupMockKeystore(
            fetchAll = testCacheMap(rentUkProperty = Some(testRentUkProperty_no_property), areYouSelfEmployed = Some(testAreYouSelfEmployed_yes))
          )

          val goodRequest = callSubmit(Yes, featureSwitch = true)
          status(goodRequest) mustBe Status.SEE_OTHER
          redirectLocation(goodRequest) mustBe Some(incometax.business.controllers.routes.WhatYearToSignUpController.show().url)
          verifyKeystore(fetchAll = 1, saveMatchTaxYear = 1)
        }
        "the what tax year to sign up feature switch is enabled and the user has business and property income" in {
          setupMockKeystore(
            fetchAll = testCacheMap(rentUkProperty = Some(testRentUkProperty_property_and_other), areYouSelfEmployed = Some(testAreYouSelfEmployed_yes))
          )

          val goodRequest = callSubmit(Yes, featureSwitch = true)
          status(goodRequest) mustBe Status.SEE_OTHER
          redirectLocation(goodRequest) mustBe Some(incometax.business.controllers.routes.BusinessAccountingMethodController.show().url)
          verifyKeystore(fetchAll = 1, saveMatchTaxYear = 1)
        }
      }

      "Option 'No' is selected and there were no previous entries" in {
        setupMockKeystore(fetchAll = None)

        val goodRequest = callSubmit(No)
        status(goodRequest) mustBe Status.SEE_OTHER
        redirectLocation(goodRequest).get mustBe incometax.business.controllers.routes.BusinessAccountingPeriodDateController.show().url
        await(goodRequest)
        verifyKeystore(fetchAll = 1, saveMatchTaxYear = 1, saveTerms = 0)
      }
    }

    "Is in edit mode and " when {
      def callSubmit(answer: YesNo): Future[Result] = callSubmitCore(answer, isEditMode = true)

      "Option 'Yes' is selected and the answer has not changed" should {
        "Redirect to Check Your Answers page" in {
          setupMockKeystore(fetchAll = testCacheMap(matchTaxYear = Some(testMatchTaxYearYes)))

          val goodRequest = callSubmit(Yes)
          status(goodRequest) mustBe Status.SEE_OTHER
          redirectLocation(goodRequest).get mustBe incometax.subscription.controllers.routes.CheckYourAnswersController.show().url
          verifyKeystore(fetchAll = 1, saveMatchTaxYear = 1, saveTerms = 0)
        }
      }

      "Option 'Yes' is selected and the answer has changed" should {
        "Redirect to Check Your Answers Page page" in {
          setupMockKeystore(fetchAll = testCacheMap(matchTaxYear = Some(testMatchTaxYearNo)))

          val goodRequest = callSubmit(Yes)
          status(goodRequest) mustBe Status.SEE_OTHER
          redirectLocation(goodRequest).get mustBe incometax.subscription.controllers.routes.CheckYourAnswersController.show().url
          verifyKeystore(fetchAll = 1, saveMatchTaxYear = 1, saveTerms = 1)
        }
      }


      "Option 'No' is selected " in {
        setupMockKeystore(fetchAll = testCacheMap(matchTaxYear = Some(testMatchTaxYearYes)))

        val goodRequest = callSubmit(No)
        status(goodRequest) mustBe Status.SEE_OTHER
        redirectLocation(goodRequest).get mustBe incometax.business.controllers.routes.BusinessAccountingPeriodDateController.show(editMode = true, editMatch = true).url
        await(goodRequest)
        verifyKeystore(fetchAll = 1, saveMatchTaxYear = 1, saveTerms = 1)
      }

      "if the answer is not changed then do not update terms" in {
        setupMockKeystore(fetchAll = testCacheMap(matchTaxYear = Some(testMatchTaxYearNo)))

        val goodRequest = callSubmit(No)
        status(goodRequest) mustBe Status.SEE_OTHER
        redirectLocation(goodRequest).get mustBe incometax.business.controllers.routes.BusinessAccountingPeriodDateController.show(editMode = true, editMatch = true).url
        await(goodRequest)
        verifyKeystore(fetchAll = 1, saveMatchTaxYear = 1, saveTerms = 0)
      }

    }
  }

  "Calling the submit action of the MatchTaxYearController with an authorised user and invalid submission" should {

    def badRequest: Future[Result] = {
      TestMatchTaxYearController.submit(isEditMode = false)(subscriptionRequest)
    }

    "return a bad request status (400)" in {
      status(badRequest) must be(Status.BAD_REQUEST)
    }

    "not update or retrieve anything from keystore" in {
      await(badRequest)
      verifyKeystore(fetchAll = 0, saveMatchTaxYear = 0)
    }
  }

  authorisationTests()

}
