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

import auth.MockConfig
import controllers.ControllerBaseSpec
import forms.BusinessStartDateForm
import forms.OtherIncomeForm.{option_no, option_yes}
import org.jsoup.Jsoup
import play.api.http.Status
import play.api.mvc.{Action, AnyContent, Result}
import play.api.test.Helpers.{contentAsString, _}
import services.mocks.MockKeystoreService
import uk.gov.hmrc.http.NotFoundException
import utils.TestModels.testBusinessStartDate

import scala.concurrent.Future

class BusinessStartDateControllerSpec extends ControllerBaseSpec
  with MockKeystoreService {

  override val controllerName: String = "BusinessStartDateController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "show" -> TestBusinessStartDateController.show(isEditMode = false),
    "submit" -> TestBusinessStartDateController.submit(isEditMode = false)
  )

  def createTestBusinessStartDateController(setEnableRegistration: Boolean): BusinessStartDateController =
    new BusinessStartDateController(
      mockBaseControllerConfig(new MockConfig {
        override val enableRegistration = setEnableRegistration
      }),
      messagesApi,
      MockKeystoreService,
      mockAuthService
    )

  lazy val TestBusinessStartDateController: BusinessStartDateController =
    createTestBusinessStartDateController(setEnableRegistration = true)

  "When registration is disabled" should {
    lazy val TestBusinessStartDateController: BusinessStartDateController =
      createTestBusinessStartDateController(setEnableRegistration = false)

    lazy val request = subscriptionRequest

    "show" should {
      "return NOT FOUND" in {
        val result = TestBusinessStartDateController.show(isEditMode = true)(request)
        val ex = intercept[NotFoundException] {
          await(result)
        }
        ex.message must startWith("This page for registration is not yet available to the public:")
      }
    }

    "submit" should {
      "return NOT FOUND" in {
        val result = TestBusinessStartDateController.submit(isEditMode = true)(request)
        val ex = intercept[NotFoundException] {
          await(result)
        }
        ex.message must startWith("This page for registration is not yet available to the public:")
      }
    }
  }

  "When registration is enabled" should {

    lazy val request = registrationRequest

    "Calling the show action of the BusinessStartDateController with an authorised user" should {

      lazy val result = TestBusinessStartDateController.show(isEditMode = false)(request)

      "return ok (200)" in {
        setupMockKeystore(
          fetchBusinessStartDate = None
        )

        status(result) must be(Status.OK)

        await(result)
        verifyKeystore(fetchBusinessStartDate = 1, saveBusinessStartDate = 0)

      }
    }

    "Calling the submit action of the BusinessStartDateController with an authorised user and valid submission" should {

      def callShow(isEditMode: Boolean) =
        TestBusinessStartDateController.submit(isEditMode = isEditMode)(
          request
            .post(BusinessStartDateForm.businessStartDateForm, testBusinessStartDate)
        )

      "When it is not in edit mode" should {
        "return a redirect status (SEE_OTHER - 303)" in {
          setupMockKeystoreSaveFunctions()

          val goodRequest = callShow(isEditMode = false)

          status(goodRequest) must be(Status.SEE_OTHER)

          await(goodRequest)
          verifyKeystore(fetchBusinessStartDate = 0, saveBusinessStartDate = 1)
        }

        s"redirect to '${controllers.business.routes.BusinessAccountingPeriodDateController.show().url}'" in {
          setupMockKeystoreSaveFunctions()

          val goodRequest = callShow(isEditMode = false)

          redirectLocation(goodRequest) mustBe Some(controllers.business.routes.BusinessAccountingPeriodDateController.show().url)

          await(goodRequest)
          verifyKeystore(fetchBusinessStartDate = 0, saveBusinessStartDate = 1)
        }
      }

      "When it is in edit mode" should {
        "return a redirect status (SEE_OTHER - 303)" in {
          setupMockKeystoreSaveFunctions()

          val goodRequest = callShow(isEditMode = true)

          status(goodRequest) must be(Status.SEE_OTHER)

          await(goodRequest)
          verifyKeystore(fetchBusinessStartDate = 0, saveBusinessStartDate = 1)
        }

        s"redirect to '${controllers.routes.CheckYourAnswersController.show().url}'" in {
          setupMockKeystoreSaveFunctions()

          val goodRequest = callShow(isEditMode = true)

          redirectLocation(goodRequest) mustBe Some(controllers.routes.CheckYourAnswersController.show().url)

          await(goodRequest)
          verifyKeystore(fetchBusinessStartDate = 0, saveBusinessStartDate = 1)
        }
      }
    }

    "Calling the submit action of the BusinessStartDateController with an authorised user and invalid submission" should {
      lazy val badRequest = TestBusinessStartDateController.submit(isEditMode = false)(request)

      "return a bad request status (400)" in {
        status(badRequest) must be(Status.BAD_REQUEST)

        await(badRequest)
        verifyKeystore(fetchBusinessStartDate = 0, saveBusinessStartDate = 0)
      }
    }

    "The back url" should {

      def result(choice: String): Future[Result] = {
        setupMockKeystore(
          fetchBusinessStartDate = None
        )
        TestBusinessStartDateController.show(isEditMode = false)(request)
      }

      // TODO change the end point when the edit page comes into play
      s"When it not is not in edit mode, it should point to '${controllers.business.routes.BusinessAddressController.init().url}'" in {
        val document = Jsoup.parse(contentAsString(result(option_yes)))
        document.select("#back").attr("href") mustBe controllers.business.routes.BusinessAddressController.init().url
      }

      s"WWhen it is in edit mode it should point to '${controllers.routes.CheckYourAnswersController.show().url}'" ignore {
        val document = Jsoup.parse(contentAsString(result(option_no)))
        document.select("#back").attr("href") mustBe controllers.routes.CheckYourAnswersController.show().url
      }

    }
  }

  authorisationTests()

}