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

package incometax.incomesource

import java.time.LocalDate

import core.config.featureswitch.NewIncomeSourceFlowFeature
import core.services.CacheConstants
import helpers.ComponentSpecBase
import helpers.IntegrationTestConstants._
import helpers.IntegrationTestModels._
import helpers.servicemocks.{AuthStub, KeystoreStub}
import incometax.subscription.models.{Both, Business, Property}
import incometax.util.AccountingPeriodUtil
import play.api.http.Status._
import play.api.i18n.Messages

class IncomeSourceControllerISpec extends ComponentSpecBase {

  override def beforeEach(): Unit = {
    super.beforeEach()
    disable(NewIncomeSourceFlowFeature)
  }

  override def afterEach(): Unit = {
    super.afterEach()
    disable(NewIncomeSourceFlowFeature)
  }


  "GET /report-quarterly/income-and-expenses/sign-up/income" when {

    "keystore returns all data" should {
      "show the income source page with an option selected" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubFullKeystore()

        When("GET /income is called")
        val res = IncomeTaxSubscriptionFrontend.income()

        Then("Should return a OK with the income source page")
        res should have(
          httpStatus(OK),
          pageTitle(Messages("income_source.title")),
          radioButtonSet(id = "incomeSource", selectedRadioButton = Some(Messages("income_source.both")))
        )
      }
    }

    "keystore returns no data" should {
      "show the income source page without an option selected" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubEmptyKeystore()

        When("GET /income is called")
        val res = IncomeTaxSubscriptionFrontend.income()

        Then("Should return a OK with the income source page")
        res should have(
          httpStatus(OK),
          pageTitle(Messages("income_source.title")),
          radioButtonSet(id = "incomeSource", selectedRadioButton = None)
        )
      }
    }

  }

  "POST /report-quarterly/income-and-expenses/sign-up/income" when {

    "not in edit mode" should {
      "select the Both income source radio button on the income source page" in {
        val userInput = Both

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubKeystoreSave(CacheConstants.IncomeSource, userInput)

        When("POST /income is called")
        val res = IncomeTaxSubscriptionFrontend.submitIncome(inEditMode = false, Some(userInput))

        Then("Should return a SEE_OTHER with a redirect location of other income")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(otherIncomeURI)
        )
      }

      "select the Business income source radio button on the income source page" in {
        val userInput = Business

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubKeystoreSave(CacheConstants.IncomeSource, userInput)

        When("POST /income is called")
        val res = IncomeTaxSubscriptionFrontend.submitIncome(inEditMode = false, Some(userInput))

        Then("Should return a SEE_OTHER with a redirect location of other income")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(otherIncomeURI)
        )
      }

      "select the Property income source radio button on the income source page" in {

        val userInput = Property

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubKeystoreSave(CacheConstants.IncomeSource, userInput)

        When("POST /income is called")
        val res = IncomeTaxSubscriptionFrontend.submitIncome(inEditMode = false, Some(userInput))

        if (AccountingPeriodUtil.getTaxEndYear(LocalDate.now()) <= 2018) {
          Then("Should return a SEE_OTHER with a redirect location of cannot report yet")
          res should have(
            httpStatus(SEE_OTHER),
            redirectURI(cannotReportYetURI)
          )
        } else {
          Then("Should return a SEE_OTHER with a redirect location of other income")
          res should have(
            httpStatus(SEE_OTHER),
            redirectURI(otherIncomeURI)
          )
        }
      }

      "select no income source option on the income source page" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubKeystoreSave(CacheConstants.IncomeSource, "")

        When("POST /income is called")
        val res = IncomeTaxSubscriptionFrontend.submitIncome(inEditMode = false, None)

        Then("Should return a BAD_REQUEST and display an error box on screen without redirecting")
        res should have(
          httpStatus(BAD_REQUEST),
          errorDisplayed()
        )
      }
    }

    "when in edit mode" should {

      "simulate not changing income source from business when calling page from Check Your Answers" in {
        val keystoreIncomeSource = Business
        val userInput = Business

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubKeystoreData(keystoreData(incomeSource = Some(keystoreIncomeSource)))
        KeystoreStub.stubKeystoreSave(CacheConstants.IncomeSource, keystoreIncomeSource)

        When("POST /income is called")
        val res = IncomeTaxSubscriptionFrontend.submitIncome(inEditMode = true, Some(userInput))

        Then("Should return a SEE_OTHER with a redirect location of check your answers")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(checkYourAnswersURI)
        )
      }

      "simulate not changing income source from property when calling page from Check Your Answers" in {
        val keystoreIncomeSource = Property
        val userInput = Property

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubKeystoreData(keystoreData(incomeSource = Some(keystoreIncomeSource)))
        KeystoreStub.stubKeystoreSave(CacheConstants.IncomeSource, keystoreIncomeSource)

        When("POST /income is called")
        val res = IncomeTaxSubscriptionFrontend.submitIncome(inEditMode = true, Some(userInput))

        Then("Should return a SEE_OTHER with a redirect location of check your answers")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(checkYourAnswersURI)
        )
      }

      "simulate not changing income source from business and property to both when calling page from Check Your Answers" in {
        val keystoreIncomeSource = Both
        val userInput = Both
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubKeystoreData(keystoreData(incomeSource = Some(keystoreIncomeSource)))
        KeystoreStub.stubKeystoreSave(CacheConstants.IncomeSource, keystoreIncomeSource)

        When("POST /income is called")
        val res = IncomeTaxSubscriptionFrontend.submitIncome(inEditMode = true, Some(userInput))

        Then("Should return a SEE_OTHER with a redirect location of check your answers")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(checkYourAnswersURI)
        )
      }

      "simulate changing income source from business to property when calling page from Check Your Answers" in {
        val keystoreIncomeSource = Business
        val userInput = Property

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubKeystoreData(keystoreData(incomeSource = Some(keystoreIncomeSource)))
        KeystoreStub.stubKeystoreSave(CacheConstants.IncomeSource, keystoreIncomeSource)

        When("POST /income is called")
        val res = IncomeTaxSubscriptionFrontend.submitIncome(inEditMode = true, Some(userInput))

        Then("Should return a SEE_OTHER with a redirect location of income other")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(otherIncomeURI)
        )
      }

      "simulate changing income source from property to both when calling page from Check Your Answers" in {
        val keystoreIncomeSource = Property
        val userInput = Both

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubKeystoreData(keystoreData(incomeSource = Some(keystoreIncomeSource)))
        KeystoreStub.stubKeystoreSave(CacheConstants.IncomeSource, keystoreIncomeSource)

        When("POST /income is called")
        val res = IncomeTaxSubscriptionFrontend.submitIncome(inEditMode = true, Some(userInput))

        Then("Should return a SEE_OTHER with a redirect location of income other")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(otherIncomeURI)
        )
      }

      "simulate changing income source from business and property to business when calling page from Check Your Answers" in {
        val keystoreIncomeSource = Both
        val userInput = Business

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubKeystoreData(keystoreData(incomeSource = Some(keystoreIncomeSource)))
        KeystoreStub.stubKeystoreSave(CacheConstants.IncomeSource, keystoreIncomeSource)

        When("POST /income is called")
        val res = IncomeTaxSubscriptionFrontend.submitIncome(inEditMode = true, Some(userInput))

        Then("Should return a SEE_OTHER with a redirect location of income other")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(otherIncomeURI)
        )
      }
    }
  }
}