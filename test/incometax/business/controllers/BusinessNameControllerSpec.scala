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

import core.controllers.ControllerBaseSpec
import core.models.{No, Yes, YesNo}
import core.services.mocks.MockKeystoreService
import core.utils.TestModels
import incometax.business.forms.BusinessNameForm
import incometax.business.models.BusinessNameModel
import org.jsoup.Jsoup
import play.api.http.Status
import play.api.mvc.{Action, AnyContent, Result}
import play.api.test.Helpers.{contentAsString, _}

import scala.concurrent.Future

class BusinessNameControllerSpec extends ControllerBaseSpec
  with MockKeystoreService {

  override val controllerName: String = "BusinessNameController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "show" -> TestBusinessNameController.show(isEditMode = false),
    "submit" -> TestBusinessNameController.submit(isEditMode = false)
  )

  object TestBusinessNameController extends BusinessNameController(
    MockBaseControllerConfig,
    messagesApi,
    MockKeystoreService,
    mockAuthService
  )

  // answer to other income is only significant for testing the backurl.
  val defaultOtherIncomeAnswer: YesNo = TestModels.testOtherIncomeNo

  "Calling the show action of the BusinessNameController with an authorised user" should {

    lazy val result = TestBusinessNameController.show(isEditMode = false)(subscriptionRequest)

    "return ok (200)" in {
      setupMockKeystore(
        fetchBusinessName = None,
        fetchOtherIncome = defaultOtherIncomeAnswer
      )

      status(result) must be(Status.OK)

      await(result)
      verifyKeystore(fetchBusinessName = 1, saveBusinessName = 0)

    }
  }

  "Calling the submit action of the BusinessNameController with an authorised user on the sign up journey and valid submission" should {

    def callShow(isEditMode: Boolean): Future[Result] =
      TestBusinessNameController.submit(isEditMode = isEditMode)(
        subscriptionRequest
          .post(BusinessNameForm.businessNameForm.form, BusinessNameModel("Test business"))
      )

    "When it is not in edit mode" should {
      s"return a redirect status (SEE_OTHER - 303) '${incometax.business.controllers.routes.MatchTaxYearController.show().url}'" in {
        setupMockKeystoreSaveFunctions()

        val goodRequest = callShow(isEditMode = false)

        status(goodRequest) must be(Status.SEE_OTHER)
        redirectLocation(goodRequest) mustBe Some(incometax.business.controllers.routes.MatchTaxYearController.show().url)

        await(goodRequest)
        verifyKeystore(fetchBusinessName = 0, saveBusinessName = 1)
      }
    }

    "When it is in edit mode" should {
      s"return a redirect status (SEE_OTHER - 303) to '${incometax.subscription.controllers.routes.CheckYourAnswersController.show().url}" in {
        setupMockKeystoreSaveFunctions()

        val goodRequest = callShow(isEditMode = true)

        status(goodRequest) must be(Status.SEE_OTHER)
        redirectLocation(goodRequest) mustBe Some(incometax.subscription.controllers.routes.CheckYourAnswersController.show().url)

        await(goodRequest)
        verifyKeystore(fetchBusinessName = 0, saveBusinessName = 1)
      }
    }
  }

  "Calling the submit action of the BusinessNameController with an authorised user on the registration journey and valid submission" should {

    def callShow(isEditMode: Boolean): Future[Result] =
      TestBusinessNameController.submit(isEditMode = isEditMode)(
        registrationRequest
          .post(BusinessNameForm.businessNameForm.form, BusinessNameModel("Test business"))
      )

    "When it is not in edit mode" should {
      s"return a redirect status (SEE_OTHER - 303) redirect to '${incometax.business.controllers.routes.BusinessPhoneNumberController.show().url}'" in {
        setupMockKeystoreSaveFunctions()

        val goodRequest = callShow(isEditMode = false)

        status(goodRequest) must be(Status.SEE_OTHER)
        redirectLocation(goodRequest) mustBe Some(incometax.business.controllers.routes.BusinessPhoneNumberController.show().url)

        await(goodRequest)
        verifyKeystore(fetchBusinessName = 0, saveBusinessName = 1)
      }
    }

    "When it is in edit mode" should {
      s"return a redirect status (SEE_OTHER - 303) redirect to '${incometax.subscription.controllers.routes.CheckYourAnswersController.show().url}'" in {
        setupMockKeystoreSaveFunctions()

        val goodRequest = callShow(isEditMode = true)

        status(goodRequest) must be(Status.SEE_OTHER)
        redirectLocation(goodRequest) mustBe Some(incometax.subscription.controllers.routes.CheckYourAnswersController.show().url)

        await(goodRequest)
        verifyKeystore(fetchBusinessName = 0, saveBusinessName = 1)
      }
    }
  }

  "Calling the submit action of the BusinessNameController with an authorised user and invalid submission" should {
    lazy val badRequest = TestBusinessNameController.submit(isEditMode = false)(subscriptionRequest)

    "return a bad request status (400)" in {
      setupMockKeystore(fetchOtherIncome = defaultOtherIncomeAnswer)

      status(badRequest) must be(Status.BAD_REQUEST)

      await(badRequest)
      verifyKeystore(fetchBusinessName = 0, saveBusinessName = 0)
    }
  }


  "The back url not in edit mode" should {

    def result(choice: YesNo): Future[Result] = {
      setupMockKeystore(
        fetchBusinessName = None,
        fetchOtherIncome = choice
      )
      TestBusinessNameController.show(isEditMode = false)(subscriptionRequest)
    }

    s"When the user previously answered yes to otherIncome, it should point to '${incometax.incomesource.controllers.routes.OtherIncomeErrorController.show().url}'" in {
      val document = Jsoup.parse(contentAsString(result(Yes)))
      document.select("#back").attr("href") mustBe incometax.incomesource.controllers.routes.OtherIncomeErrorController.show().url
    }

    s"When the user previously answered no to otherIncome, it should point to '${incometax.incomesource.controllers.routes.OtherIncomeController.show().url}'" in {
      val document = Jsoup.parse(contentAsString(result(No)))
      document.select("#back").attr("href") mustBe incometax.incomesource.controllers.routes.OtherIncomeController.show().url
    }
  }

  "The back url in edit mode" should {
    s"point to ${incometax.subscription.controllers.routes.CheckYourAnswersController.show().url} on other income page" in {
      def backUrl = TestBusinessNameController.backUrl(isEditMode = true)(subscriptionRequest)

      val callBack = backUrl

      await(callBack) mustBe incometax.subscription.controllers.routes.CheckYourAnswersController.show().url
    }
  }

  authorisationTests()

}
