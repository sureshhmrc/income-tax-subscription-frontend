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

package agent.services

import agent.models.{AccountingMethodPropertyModel, _}
import core.config.featureswitch.WhatTaxYearToSignUp
import core.models.YesNo
import incometax.business.models.{AccountingPeriodModel, MatchTaxYearModel}
import incometax.subscription.models.IncomeSourceType
import javax.inject._
import play.api.libs.json.{Reads, Writes}
import uk.gov.hmrc.http.cache.client.{CacheMap, SessionCache}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class KeystoreService @Inject()(val session: SessionCache)(implicit ec: ExecutionContext) {

  type FO[T] = Future[Option[T]]
  type FC = Future[CacheMap]

  protected def fetch[T](location: String)(implicit hc: HeaderCarrier, reads: Reads[T]): FO[T] = session.fetchAndGetEntry(location)

  protected def save[T](location: String, obj: T)(implicit hc: HeaderCarrier, reads: Writes[T]): FC = session.cache(location, obj)

  def fetchAll()(implicit hc: HeaderCarrier): Future[Option[CacheMap]] = session.fetch()

  def deleteAll()(implicit hc: HeaderCarrier): Future[HttpResponse] = session.remove()

  import CacheConstants._

  def fetchIncomeSource()(implicit hc: HeaderCarrier, reads: Reads[IncomeSourceType]): FO[IncomeSourceType] =
    fetch[IncomeSourceType](IncomeSource)

  def saveIncomeSource(incomeSource: IncomeSourceType)(implicit hc: HeaderCarrier, reads: Reads[IncomeSourceType]): FC =
    save[IncomeSourceType](IncomeSource, incomeSource)

  def fetchBusinessName()(implicit hc: HeaderCarrier, reads: Reads[BusinessNameModel]): FO[BusinessNameModel] =
    fetch[BusinessNameModel](BusinessName)

  def saveBusinessName(businessName: BusinessNameModel)(implicit hc: HeaderCarrier, reads: Reads[BusinessNameModel]): FC =
    save[BusinessNameModel](BusinessName, businessName)

  def fetchMatchTaxYear()(implicit hc: HeaderCarrier, reads: Reads[MatchTaxYearModel]): FO[MatchTaxYearModel] =
    fetch[MatchTaxYearModel](MatchTaxYear)

  def saveMatchTaxYear(accountingPeriod: MatchTaxYearModel)(implicit hc: HeaderCarrier, reads: Reads[MatchTaxYearModel]): FC =
    save[MatchTaxYearModel](MatchTaxYear, accountingPeriod)

  def fetchWhatYearToSignUp()(implicit hc: HeaderCarrier, reads: Reads[AccountingYearModel]): FO[AccountingYearModel] =
    fetch[AccountingYearModel](WhatYearToSignUp)

  def saveWhatYearToSignUp(accountingPeriod: AccountingYearModel)(implicit hc: HeaderCarrier, reads: Reads[AccountingYearModel]): FC =
    save[AccountingYearModel](WhatYearToSignUp, accountingPeriod)

  def fetchAccountingPeriodDate()(implicit hc: HeaderCarrier, reads: Reads[AccountingPeriodModel]): FO[AccountingPeriodModel] =
    fetch[AccountingPeriodModel](AccountingPeriodDate)

  def saveAccountingPeriodDate(accountingPeriod: AccountingPeriodModel)(implicit hc: HeaderCarrier, reads: Reads[AccountingPeriodModel]): FC =
    save[AccountingPeriodModel](AccountingPeriodDate, accountingPeriod)

  def fetchAccountingMethod()(implicit hc: HeaderCarrier, reads: Reads[AccountingMethodModel]): FO[AccountingMethodModel] =
    fetch[AccountingMethodModel](AccountingMethod)

  def saveAccountingMethod(accountingMethod: AccountingMethodModel)(implicit hc: HeaderCarrier, reads: Reads[AccountingMethodModel]): FC =
    save[AccountingMethodModel](AccountingMethod, accountingMethod)

  def fetchAccountingMethodProperty()(implicit hc: HeaderCarrier, reads: Reads[AccountingMethodPropertyModel]): FO[AccountingMethodPropertyModel] =
    fetch[AccountingMethodPropertyModel](AccountingMethodProperty)

  def saveAccountingMethodProperty(accountingMethod: AccountingMethodPropertyModel)(implicit hc: HeaderCarrier, reads: Reads[AccountingMethodPropertyModel]): FC =
    save[AccountingMethodPropertyModel](AccountingMethodProperty, accountingMethod)

  def fetchTerms()(implicit hc: HeaderCarrier, reads: Reads[Boolean]): FO[Boolean] =
    fetch[Boolean](Terms)

  def saveTerms(terms: Boolean)(implicit hc: HeaderCarrier, reads: Reads[Boolean]): FC =
    save[Boolean](Terms, terms)

  def fetchOtherIncome()(implicit hc: HeaderCarrier, reads: Reads[YesNo]): FO[YesNo] =
    fetch[YesNo](OtherIncome)

  def saveOtherIncome(otherIncome: YesNo)(implicit hc: HeaderCarrier, reads: Reads[YesNo]): FC =
    save[YesNo](OtherIncome, otherIncome)

  def fetchSubscriptionId()(implicit hc: HeaderCarrier, reads: Reads[String]): FO[String] = fetch[String](MtditId)

  def saveSubscriptionId(mtditId: String)(implicit hc: HeaderCarrier, reads: Reads[String]): FC = save[String](MtditId, mtditId)

  def fetchAccountingPeriodPrior()(implicit hc: HeaderCarrier, reads: Reads[AccountingPeriodPriorModel]): FO[AccountingPeriodPriorModel] =
    fetch[AccountingPeriodPriorModel](AccountingPeriodPrior)

  def saveAccountingPeriodPrior(accountingPeriodPrior: AccountingPeriodPriorModel)
                               (implicit hc: HeaderCarrier, reads: Reads[AccountingPeriodPriorModel]): FC =
    save[AccountingPeriodPriorModel](AccountingPeriodPrior, accountingPeriodPrior)

}

