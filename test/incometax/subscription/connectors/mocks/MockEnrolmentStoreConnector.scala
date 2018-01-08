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

package incometax.subscription.connectors.mocks

import core.utils.MockTrait
import core.utils.TestConstants._
import incometax.subscription.connectors.EnrolmentStoreConnector
import incometax.subscription.httpparsers.AllocateEnrolmentResponseHttpParser._
import incometax.subscription.httpparsers.UpsertEnrolmentResponseHttpParser._
import incometax.subscription.models._
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

trait MockEnrolmentStoreConnector extends MockTrait {
  val mockEnrolmentStoreConnector = mock[EnrolmentStoreConnector]

  private def mockUpsertEnrolment(enrolmentKey: EnrolmentKey,
                                  enrolmentVerifiers: EnrolmentVerifiers
                                 )(response: Future[UpsertEnrolmentResponse]): Unit =
    when(mockEnrolmentStoreConnector.upsertEnrolment(
      ArgumentMatchers.eq(enrolmentKey),
      ArgumentMatchers.eq(enrolmentVerifiers)
    )(ArgumentMatchers.any[HeaderCarrier]))
      .thenReturn(response)

  def mockUpsertEnrolmentSuccess(enrolmentKey: EnrolmentKey,
                                 enrolmentVerifiers: EnrolmentVerifiers): Unit =
    mockUpsertEnrolment(enrolmentKey, enrolmentVerifiers)(Future.successful(Right(KnownFactsSuccess)))

  def mockUpsertEnrolmentFailure(enrolmentKey: EnrolmentKey,
                                 enrolmentVerifiers: EnrolmentVerifiers): Unit =
    mockUpsertEnrolment(enrolmentKey, enrolmentVerifiers)(Future.successful(Left(KnownFactsFailure(testErrorMessage))))

  def mockUpsertEnrolmentException(enrolmentKey: EnrolmentKey,
                                   enrolmentVerifiers: EnrolmentVerifiers): Unit =
    mockUpsertEnrolment(enrolmentKey, enrolmentVerifiers)(Future.failed(testException))

  private def mockAllocateEnrolment(groupId: String,
                                    enrolmentKey: EnrolmentKey,
                                    enrolmentRequest: EmacEnrolmentRequest
                                   )(response: Future[AllocateEnrolmentResponse]): Unit =
    when(mockEnrolmentStoreConnector.allocateEnrolment(
      ArgumentMatchers.eq(groupId),
      ArgumentMatchers.eq(enrolmentKey),
      ArgumentMatchers.eq(enrolmentRequest)
    )(ArgumentMatchers.any[HeaderCarrier]))
      .thenReturn(response)

  def mockAllocateEnrolmentSuccess(groupId: String,
                                   enrolmentKey: EnrolmentKey,
                                   enrolmentRequest: EmacEnrolmentRequest): Unit =
    mockAllocateEnrolment(groupId, enrolmentKey, enrolmentRequest)(Future.successful(Right(EnrolSuccess)))

  def mockAllocateEnrolmentFailure(groupId: String,
                                   enrolmentKey: EnrolmentKey,
                                   enrolmentRequest: EmacEnrolmentRequest): Unit =
    mockAllocateEnrolment(groupId, enrolmentKey, enrolmentRequest)(Future.successful(Left(EnrolFailure(testErrorMessage))))

  def mockAllocateEnrolmentException(groupId: String,
                                     enrolmentKey: EnrolmentKey,
                                     enrolmentRequest: EmacEnrolmentRequest): Unit =
    mockAllocateEnrolment(groupId, enrolmentKey, enrolmentRequest)(Future.failed(testException))
}
