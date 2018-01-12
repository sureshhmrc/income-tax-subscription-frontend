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

package incometax.unauthorisedagent.services

import core.config.featureswitch.FeatureSwitching
import core.utils.UnitTestTrait
import incometax.unauthorisedagent.services.mocks.TestAgencyNameService
import core.utils.TestConstants._
import play.api.test.Helpers._
import uk.gov.hmrc.http.InternalServerException

class AgencyNameServiceSpec extends UnitTestTrait with TestAgencyNameService with FeatureSwitching {
  //TODO Turn back on when agent services are ready
  "getAgencyName" ignore {
    "the connector returns a successful agency name" should {
      "return the agency name" in {
        mockGetAgencyName(testArn)(getAgencyNameSuccess)

        await(TestAgencyNameService.getAgencyName(testArn)) mustBe testAgencyName
      }
    }

    "the connector returns a failure" should {
      "throw an Internal Server Exception" in {
        mockGetAgencyName(testArn)(getAgencyNameFailure)

        intercept[InternalServerException](await(TestAgencyNameService.getAgencyName(testArn)))
      }
    }
  }
}
