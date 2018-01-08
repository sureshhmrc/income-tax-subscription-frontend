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

package usermatching.userjourneys

import core.ITSASessionKeys
import core.auth.AuthPredicate.AuthPredicateSuccess
import core.auth.AuthPredicates._
import core.auth.IncomeTaxSAUser
import core.config.MockConfig
import core.config.featureswitch.FeatureSwitching
import core.utils.UnitTestTrait
import org.scalatest.EitherValues
import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core.{AffinityGroup, ConfidenceLevel, Enrolments}
import play.api.test.Helpers._


class ConfirmAgentSubscriptionSpec extends UnitTestTrait with FeatureSwitching with MockConfig with EitherValues {

  implicit val config = MockConfig

  lazy val journeyState = ConfirmAgentSubscription

  journeyState.featureSwitch foreach { featureSwitch =>
    "isEnabled" should {
      s"return true if the config for ${featureSwitch.displayText} is enabled" in {
        enable(featureSwitch)
        journeyState.isEnabled mustBe true
      }
      s"return false if the config for ${featureSwitch.displayText} is disabled" in {
        disable(featureSwitch)
        journeyState.isEnabled mustBe false
      }
    }
  }

  lazy val request = FakeRequest().withSession(ITSASessionKeys.JourneyStateKey -> journeyState.name)
  lazy val testUser = IncomeTaxSAUser(Enrolments(Set.empty), Some(AffinityGroup.Individual), ConfidenceLevel.L200)

  "journeyStatePredicate" should {
    s"return $AuthPredicateSuccess when the session contains the correct state" in {
      val res = journeyState.journeyStatePredicate.apply(request)(testUser)
      res.right.value mustBe AuthPredicateSuccess
    }

    s"return $homeRoute when the session is not in the correct state" in {
      val res = journeyState.journeyStatePredicate.apply(FakeRequest())(testUser)
      await(res.left.value) mustBe homeRoute
    }
  }

}