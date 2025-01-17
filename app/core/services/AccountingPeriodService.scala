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

package core.services

import java.time.LocalDate
import java.time.Month.{APRIL, MARCH}

import incometax.util.AccountingPeriodUtil._
import incometax.util.{AccountingPeriodUtil, CurrentDateProvider}
import javax.inject.{Inject, Singleton}

import Ordering.Implicits._

@Singleton
class AccountingPeriodService @Inject()(currentDateProvider: CurrentDateProvider) {

  def checkEligibleAccountingPeriod(startDate: LocalDate, endDate: LocalDate): Boolean = {
    val taxYear = AccountingPeriodUtil.getTaxEndYear(endDate)

    val isEligibleTaxYear = taxYear == currentTaxYear || taxYear == currentTaxYear + 1

    val isEligibleStartDate = if (endDate.getDayOfMonth == 5 && endDate.getMonth == APRIL) {
      val minimumStartDate = LocalDate.of(endDate.getYear - 1, APRIL, 1)
      val maximumStartDate = LocalDate.of(endDate.getYear - 1, APRIL, 6)
      startDate >= minimumStartDate && startDate <= maximumStartDate
    } else startDate == endDate.minusYears(1).plusDays(1)

    isEligibleTaxYear && isEligibleStartDate
  }

  def currentTaxYear: Int = AccountingPeriodUtil.getTaxEndYear(currentDateProvider.getCurrentDate())
}
