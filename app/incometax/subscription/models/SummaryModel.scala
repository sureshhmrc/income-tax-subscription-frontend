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

package incometax.subscription.models

import agent.models.AccountingPeriodPriorModel
import incometax.business.models.address.Address
import incometax.business.models._
import incometax.business.models.address.Address
import incometax.incomesource.models.{IncomeSourceModel, OtherIncomeModel}

case class SummaryModel(incomeSource: Option[IncomeSourceModel] = None,
                        otherIncome: Option[OtherIncomeModel] = None,
                        matchTaxYear: Option[MatchTaxYearModel] = None,
                        accountingPeriodPrior: Option[AccountingPeriodPriorModel] = None,
                        accountingPeriod: Option[AccountingPeriodModel] = None,
                        businessName: Option[BusinessNameModel] = None,
                        businessPhoneNumber: Option[BusinessPhoneNumberModel] = None,
                        businessAddress: Option[Address] = None,
                        businessStartDate: Option[BusinessStartDateModel] = None,
                        accountingMethod: Option[AccountingMethodModel] = None,
                        terms: Option[Boolean] = None)
