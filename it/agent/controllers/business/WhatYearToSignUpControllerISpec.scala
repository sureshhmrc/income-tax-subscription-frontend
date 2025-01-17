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

package agent.controllers.business

import java.time.LocalDate

import agent.helpers.{ComponentSpecBase, IntegrationTestModels}
import agent.helpers.IntegrationTestModels.keystoreData
import agent.helpers.servicemocks.{AuthStub, KeystoreStub}
import agent.models.AccountingYearModel
import core.models.{Current, Next}
import agent.helpers.IntegrationTestConstants._
import agent.services.CacheConstants
import incometax.util.AccountingPeriodUtil
import play.api.http.Status.{BAD_REQUEST, SEE_OTHER}
import play.api.i18n.Messages

class WhatYearToSignUpControllerISpec extends ComponentSpecBase {

  "GET /report-quarterly/income-and-expenses/sign-up//client/business/what-year-to-sign-up" when {

    "keystore returns some data" should {
      "show the What Tax Year To Sign Up with an option current tax year selected" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubFullKeystore()

        When("GET /client/business/what-year-to-sign-up is called")
        val res = IncomeTaxSubscriptionFrontend.accountingYear()

        val fromYear: String = (AccountingPeriodUtil.getTaxEndYear(LocalDate.now()) - 1).toString
        val toYear: String = AccountingPeriodUtil.getTaxEndYear(LocalDate.now()).toString

        val expectedText = removeHtmlMarkup(Messages("agent.business.what_year_to_sign_up.option_1", fromYear, toYear))

        Then("Should return a OK with the What Year To Sign Up page")
        res should have(
          httpStatus(200),
          pageTitle(Messages("agent.business.what_year_to_sign_up.heading")),
          radioButtonSet(id = "accountingYear-CurrentYear", selectedRadioButton = Some(expectedText)),
          radioButtonSet(id = "accountingYear-NextYear", selectedRadioButton = None)
        )
      }
    }

    "keystore returns no data" should {
      "show the What Year To Sign Up page without an option selected" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubKeystoreData(keystoreData(selectedTaxYear = None))

        When("GET /client/business/what-year-to-sign-up is called")
        val res = IncomeTaxSubscriptionFrontend.accountingYear()

        Then("Should return a OK with the What Year To Sign Up page")
        res should have(
          httpStatus(200),
          pageTitle(Messages("agent.business.what_year_to_sign_up.heading")),
          radioButtonSet(id = "accountingYear-CurrentYear", selectedRadioButton = None),
          radioButtonSet(id = "accountingYear-NextYear", selectedRadioButton = None)
        )
      }
    }

  }

  "POST /report-quarterly/income-and-expenses/sign-up//client/business/what-year-to-sign-up" when {

    "not in edit mode" should {

      "select the Current Year radio button on the What Year To Sign Up page" in {
        val userInput = AccountingYearModel(Current)

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubKeystoreSave(CacheConstants.WhatYearToSignUp, userInput)

        When("POST /client/business/what-year-to-sign-up is called")
        val res = IncomeTaxSubscriptionFrontend.submitAccountingYear(inEditMode = false, Some(userInput))

        Then("Should return a SEE_OTHER with a redirect location of Business Accounting Period Method page")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(businessAccountingMethodURI)
        )
      }

      "select the Next radio button on the What Year To Sign Up page" in {
        val userInput = AccountingYearModel(Next)

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubKeystoreSave(CacheConstants.WhatYearToSignUp, userInput)

        When("POST /client/business/what-year-to-sign-up is called")
        val res = IncomeTaxSubscriptionFrontend.submitAccountingYear(inEditMode = false, Some(userInput))

        Then("Should return a SEE_OTHER with a redirect location of Business Accounting Period Method page")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(businessAccountingMethodURI)
        )
      }
    }

    "not select an option on the accounting year page" in {
      Given("I setup the Wiremock stubs")
      AuthStub.stubAuthSuccess()
      KeystoreStub.stubKeystoreSave(CacheConstants.WhatYearToSignUp, "")

      When("POST /client/business/what-year-to-sign-up is called")

      val res = IncomeTaxSubscriptionFrontend.submitAccountingYear(inEditMode = false, None)

      Then("Should return a BAD_REQUEST and display an error box on screen without redirecting")
      res should have(
        httpStatus(BAD_REQUEST),
        errorDisplayed()
      )
    }

    "in edit mode" should {

      "changing from the Current radio button to Next on the accounting method page" in {


        val keystoreAccountingYearCurrent: AccountingYearModel = IntegrationTestModels.testAccountingYearCurrent
        val keystoreAccountingYearNext: AccountingYearModel = IntegrationTestModels.testAccountingYearNext
        val userInput = keystoreAccountingYearNext

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubKeystoreData(
          keystoreData(selectedTaxYear = Some(keystoreAccountingYearCurrent))
        )
        KeystoreStub.stubKeystoreSave(CacheConstants.WhatYearToSignUp, userInput)

        When("POST /client/business/what-year-to-sign-up is called")
        val res = IncomeTaxSubscriptionFrontend.submitAccountingYear(inEditMode = true, Some(userInput))

        Then("Should return a SEE_OTHER with a redirect location of CYA")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(checkYourAnswersURI)
        )
      }

      "simulate not changing accounting year when calling page from Check Your Answers" in {


        val keystoreAccountingYearCurrent: AccountingYearModel = IntegrationTestModels.testAccountingYearCurrent
        val userInput = keystoreAccountingYearCurrent

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubKeystoreData(
          keystoreData(
            selectedTaxYear = Some(keystoreAccountingYearCurrent)
          )
        )
        KeystoreStub.stubKeystoreSave(CacheConstants.WhatYearToSignUp, userInput)

        When("POST /client/business/what-year-to-sign-up is called")
        val res = IncomeTaxSubscriptionFrontend.submitAccountingYear(inEditMode = true, Some(userInput))

        Then("Should return a SEE_OTHER with a redirect location of check your answers")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(checkYourAnswersURI)
        )
      }

    }
  }
}