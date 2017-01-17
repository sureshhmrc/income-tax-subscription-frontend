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

package assets

object MessageLookup {

  object Base {
    val continue = "Continue"
    val submit = "Submit"
    val startDate = "Start date"
    val endDate = "End date"
    val day = "Day"
    val month = "Month"
    val year = "Year"
    val errorHeading = "Error Summary"
    val chage = "Change"
  }

  object HelloWorld {
    val title = "Hello from income-tax-subscription-frontend"
    val heading = "Hello from income-tax-subscription-frontend !"
  }

  object AccountingPeriod {
    val title = "What is your accounting period?"
    val heading = "What is your accounting period?"
  }

  object ContactEmail {
    val title = "What is your contact email address?"
    val heading: String = title
  }

  object BusinessName {
    val title = "What is your business name?"
    val heading: String = title
    val hint = "This does not mean trading name"
  }

  object BusinessIncomeType {
    val title = "How do you receive your business income?"
    val heading: String = title
    val cash = "Cash"
    val accruals = "Accruals"
  }

  object Terms {
    val title = "Terms"
    val heading: String = title
  }

  object Summary {
    val title = "Check your answers"
    val heading: String = title
    val accounting_period = "Business accounting period"
    val accounting_period_month: Int => String = (month: Int) => s"$month month period"
    val business_name = "Business name"
    val income_type = "Income type"
    val contact_email = "Contact email"
    val terms = "Terms and conditions"
    val terms_agreed = "Read and agreed"
  }

  object Timeout {
    val title = "Session closed due to inactivity"
    val heading = "You've been signed out due to inactivity."
    val returnToHome = """You can start again from the <a href="{0}" rel="external">subscription</a> page."""
  }

  object Confirmation {
    val title = "Submitted"
    val heading: String = title
    val submissionReferenceLabel = "Submission number:"
    val submissionDateLabel = "Date:"
    val emailConfirmation = "You'll receive an email confirming that your application has been received."

    object whatHappensNext {
      val heading = "What happens next"
      val para1 = "Your process for HMRC."
      val bullet1 = "What correspondence user will expect to receive and within what time period"
      val bullet2 = "What implications and obligations on the user as a result of this subscription if any"
    }

    object registerForMoreTax {
      val heading = "Register for more tax"
      val link1 = "PAYE (Pay as you earn)"
      val link2 = "VAT (Value added tax)"
    }

    object guidanceSection {
      val heading = "Guidance"
      val link1 = "Quarterly filing instructions"
      val link2 = "Downloading software"
      val link3 = "Further reading"
    }

    object giveUsFeedback {
      val heading = "Give us feedback"
      val link1 = "What did you think of this service?"
      val feedbackDuration = "(takes 30 seconds)"
    }

  }

}
