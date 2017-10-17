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

package incometax.subscription.connectors

import incometax.subscription.connectors.mocks.TestSubscriptionConnector
import org.scalatest.Matchers._
import org.scalatest.{EitherValues, OptionValues}
import core.utils.TestConstants._

class SubscriptionConnectorSpec extends TestSubscriptionConnector with EitherValues with OptionValues {
  "SubscriptionConnector.subscribe" should {

    "Post to the correct url" in {
      TestSubscriptionConnector.subscriptionUrl(testNino) should endWith(s"/income-tax-subscription/subscription/$testNino")
    }
  }

  "SubscriptionConnector.getSubscription" should {

    "GET to the correct url" in {
      TestSubscriptionConnector.subscriptionUrl(testNino) should endWith(s"/income-tax-subscription/subscription/$testNino")
    }
  }

}
