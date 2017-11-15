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

package usermatching.services

import java.net.URLEncoder

import org.scalatest.EitherValues
import org.scalatest.Matchers._
import play.api.test.Helpers._
import uk.gov.hmrc.http.UserId
import usermatching.models.{LockoutStatusFailureResponse, NotLockedOut}
import usermatching.services.mocks.TestUserLockoutService
import core.utils.TestConstants._

class UserLockOutServiceSpec extends TestUserLockoutService with EitherValues {

  "UserLockoutService.lockoutUser" should {

    def call = await(TestUserLockoutService.lockoutUser(token = testUserId.value))

    "return the not locked out status" in {
      def stripUserId(userId: UserId): String = URLEncoder.encode(userId.value, "UTF-8")
      setupMockLockCreated(escapedUserId)
      call.right.value shouldBe testLockoutResponse
    }

    "return the error if lock status fails on bad request" in {
      setupMockLockFailureResponse(escapedUserId)
      call.left.value shouldBe LockoutStatusFailureResponse(BAD_REQUEST)
    }

    "return the error if locked out throws an exception" in {
      setupMockLockException(escapedUserId)
      intercept[Exception](call) shouldBe testException
    }
  }

  "UserLockoutService.getLockOutStatus" should {

    def call = await(TestUserLockoutService.getLockoutStatus(token = testUserId.value))

    "return the not locked out status" in {
      setupMockNotLockedOut(escapedUserId)
      call.right.value shouldBe NotLockedOut
    }

    "return the locked out status" in {
      setupMockLockedOut(escapedUserId)
      call.right.value shouldBe testLockoutResponse
    }

    "return the error if lock status fails on bad request" in {
      setupMockLockStatusFailureResponse(escapedUserId)
      call.left.value shouldBe LockoutStatusFailureResponse(BAD_REQUEST)
    }

    "return the error if locked out throws an exception" in {
      setupMockLockStatusException(escapedUserId)
      intercept[Exception](call) shouldBe testException
    }
  }

}
