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

package agent.services.mocks

import _root_.agent.services.KeystoreService
import _root_.core.utils.MockTrait
import agent.models._
import core.models.YesNo
import incometax.business.models.{AccountingPeriodModel, MatchTaxYearModel}
import incometax.subscription.models.IncomeSourceType
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.http.cache.client.{CacheMap, SessionCache}

import scala.concurrent.{ExecutionContext, Future}


trait MockKeystoreService extends MockTrait {

  import _root_.agent.services.CacheConstants._

  val returnedCacheMap: CacheMap = CacheMap("", Map())

  object MockKeystoreService extends KeystoreService(mock[SessionCache])

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(MockKeystoreService.session)
  }

  private final def mockFetchFromKeyStore[T](key: String, config: MFO[T]): Unit =
    config ifConfiguredThen (dataToReturn => when(MockKeystoreService.session.fetchAndGetEntry[T](ArgumentMatchers.eq(key))(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any[ExecutionContext])).thenReturn(dataToReturn))

  private final def verifyKeystoreFetch[T](key: String, someCount: Option[Int]): Unit =
    someCount ifDefinedThen (count => verify(MockKeystoreService.session, times(count)).fetchAndGetEntry[T](ArgumentMatchers.eq(key))(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any[ExecutionContext]))

  private final def verifyKeystoreSave[T](key: String, someCount: Option[Int]): Unit =
    someCount ifDefinedThen (count => verify(MockKeystoreService.session, times(count)).cache[T](ArgumentMatchers.eq(key), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any[ExecutionContext]))


  protected final def setupMockKeystoreSaveFunctions(): Unit =
    when(MockKeystoreService.session.cache(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any[ExecutionContext])).thenReturn(Future.successful(returnedCacheMap))

  protected final def setupMockKeystore(
                                         fetchIncomeSource: MFO[IncomeSourceType] = DoNotConfigure,
                                         fetchBusinessName: MFO[BusinessNameModel] = DoNotConfigure,
                                         fetchAccountingPeriodDate: MFO[AccountingPeriodModel] = DoNotConfigure,
                                         fetchAccountingMethod: MFO[AccountingMethodModel] = DoNotConfigure,
                                         fetchPropertyAccountingMethod: MFO[AccountingMethodPropertyModel] = DoNotConfigure,
                                         fetchTerms: MFO[Boolean] = DoNotConfigure,
                                         fetchOtherIncome: MFO[YesNo] = DoNotConfigure,
                                         fetchSubscriptionId: MFO[String] = DoNotConfigure,
                                         fetchAccountingPeriodPrior: MFO[AccountingPeriodPriorModel] = DoNotConfigure,
                                         fetchMatchTaxYear: MFO[MatchTaxYearModel] = DoNotConfigure,
                                         fetchWhatYearToSignUp: MFO[AccountingYearModel] = DoNotConfigure,
                                         fetchAll: MFO[CacheMap] = DoNotConfigure,
                                         deleteAll: MF[HttpResponse] = DoNotConfigure
                                       ): Unit = {
    mockFetchFromKeyStore[IncomeSourceType](IncomeSource, fetchIncomeSource)
    mockFetchFromKeyStore[BusinessNameModel](BusinessName, fetchBusinessName)
    mockFetchFromKeyStore[AccountingPeriodModel](AccountingPeriodDate, fetchAccountingPeriodDate)
    mockFetchFromKeyStore[AccountingMethodModel](AccountingMethod, fetchAccountingMethod)
    mockFetchFromKeyStore[AccountingMethodPropertyModel](AccountingMethodProperty, fetchPropertyAccountingMethod)
    mockFetchFromKeyStore[Boolean](Terms, fetchTerms)
    mockFetchFromKeyStore[YesNo](OtherIncome, fetchOtherIncome)
    mockFetchFromKeyStore[String](MtditId, fetchSubscriptionId)
    mockFetchFromKeyStore[AccountingPeriodPriorModel](AccountingPeriodPrior, fetchAccountingPeriodPrior)
    mockFetchFromKeyStore[MatchTaxYearModel](MatchTaxYear, fetchMatchTaxYear)
    mockFetchFromKeyStore[AccountingYearModel](WhatYearToSignUp, fetchWhatYearToSignUp)

    setupMockKeystoreSaveFunctions()

    fetchAll ifConfiguredThen (dataToReturn => when(MockKeystoreService.session.fetch()(ArgumentMatchers.any(), ArgumentMatchers.any[ExecutionContext])).thenReturn(dataToReturn))
    deleteAll ifConfiguredThen (dataToReturn => when(MockKeystoreService.session.remove()(ArgumentMatchers.any(), ArgumentMatchers.any[ExecutionContext])).thenReturn(dataToReturn))
  }

  protected final def verifyKeystore(
                                      fetchIncomeSource: Option[Int] = None,
                                      saveIncomeSource: Option[Int] = None,
                                      fetchBusinessName: Option[Int] = None,
                                      saveBusinessName: Option[Int] = None,
                                      fetchMatchTaxYear: Option[Int] = None,
                                      saveMatchTaxYear: Option[Int] = None,
                                      fetchWhatYearToSignUp: Option[Int] = None,
                                      saveWhatYearToSignUp: Option[Int] = None,
                                      fetchAccountingPeriodDate: Option[Int] = None,
                                      saveAccountingPeriodDate: Option[Int] = None,
                                      fetchAccountingMethod: Option[Int] = None,
                                      saveAccountingMethod: Option[Int] = None,
                                      fetchPropertyAccountingMethod: Option[Int] = None,
                                      savePropertyAccountingMethod: Option[Int] = None,
                                      fetchTerms: Option[Int] = None,
                                      saveTerms: Option[Int] = None,
                                      fetchOtherIncome: Option[Int] = None,
                                      saveOtherIncome: Option[Int] = None,
                                      fetchSubscriptionId: Option[Int] = None,
                                      saveSubscriptionId: Option[Int] = None,
                                      fetchAccountingPeriodPrior: Option[Int] = None,
                                      saveAccountingPeriodPrior: Option[Int] = None,
                                      fetchAll: Option[Int] = None,
                                      deleteAll: Option[Int] = None
                                    ): Unit = {
    verifyKeystoreFetch(IncomeSource, fetchIncomeSource)
    verifyKeystoreSave(IncomeSource, saveIncomeSource)
    verifyKeystoreFetch(BusinessName, fetchBusinessName)
    verifyKeystoreSave(BusinessName, saveBusinessName)
    verifyKeystoreFetch(MatchTaxYear, fetchMatchTaxYear)
    verifyKeystoreSave(MatchTaxYear, saveMatchTaxYear)
    verifyKeystoreFetch(WhatYearToSignUp, fetchWhatYearToSignUp)
    verifyKeystoreSave(WhatYearToSignUp, saveWhatYearToSignUp)
    verifyKeystoreFetch(AccountingPeriodDate, fetchAccountingPeriodDate)
    verifyKeystoreSave(AccountingPeriodDate, saveAccountingPeriodDate)
    verifyKeystoreFetch(AccountingMethod, fetchAccountingMethod)
    verifyKeystoreSave(AccountingMethod, saveAccountingMethod)
    verifyKeystoreFetch(AccountingMethodProperty, fetchPropertyAccountingMethod)
    verifyKeystoreSave(AccountingMethodProperty, savePropertyAccountingMethod)
    verifyKeystoreFetch(Terms, fetchTerms)
    verifyKeystoreSave(Terms, saveTerms)
    verifyKeystoreFetch(OtherIncome, fetchOtherIncome)
    verifyKeystoreSave(OtherIncome, saveOtherIncome)
    verifyKeystoreFetch(MtditId, fetchSubscriptionId)
    verifyKeystoreSave(MtditId, saveSubscriptionId)
    verifyKeystoreFetch(AccountingPeriodPrior, fetchAccountingPeriodPrior)
    verifyKeystoreSave(AccountingPeriodPrior, saveAccountingPeriodPrior)

    fetchAll ifDefinedThen (count => verify(MockKeystoreService.session, times(count)).fetch()(ArgumentMatchers.any(), ArgumentMatchers.any[ExecutionContext]))
    deleteAll ifDefinedThen (count => verify(MockKeystoreService.session, times(count)).remove()(ArgumentMatchers.any(), ArgumentMatchers.any[ExecutionContext]))
  }

}
