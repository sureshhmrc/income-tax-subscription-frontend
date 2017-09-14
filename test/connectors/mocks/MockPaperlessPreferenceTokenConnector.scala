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

package connectors.mocks

import connectors.models.preferences.PaperlessPreferenceTokenResult.{PaperlessPreferenceTokenSuccess, _}
import connectors.preferences.PaperlessPreferenceTokenConnector
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import uk.gov.hmrc.http.HeaderCarrier
import utils.MockTrait

import scala.concurrent.Future

trait MockPaperlessPreferenceTokenConnector extends MockTrait {
  val mockPaperlessPreferenceTokenConnector = mock[PaperlessPreferenceTokenConnector]

  private def mockStoreNino(nino: String)(result: Future[PaperlessPreferenceTokenResult]) =
    when(mockPaperlessPreferenceTokenConnector.storeNino(ArgumentMatchers.any[String], ArgumentMatchers.eq(nino))(ArgumentMatchers.any[HeaderCarrier]))
      .thenReturn(result)


  def mockStoreNinoSuccess(nino: String): Unit = mockStoreNino(nino)(Future.successful(Right(PaperlessPreferenceTokenSuccess)))

  def verifyStoreNino(nino: String): Unit = verify(mockPaperlessPreferenceTokenConnector)
    .storeNino(ArgumentMatchers.any[String], ArgumentMatchers.eq(nino))(ArgumentMatchers.any[HeaderCarrier])
}
