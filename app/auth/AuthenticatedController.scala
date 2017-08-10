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

package auth

import auth.AuthPredicate._
import auth.AuthPredicates._
import config.BaseControllerConfig
import controllers.ITSASessionKeys
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, Request, Result}
import services.AuthService
import uk.gov.hmrc.auth.core.Retrievals._
import uk.gov.hmrc.auth.core.~
import uk.gov.hmrc.play.frontend.controller.FrontendController

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * Created by rob on 04/08/17.
  */
trait AuthenticatedController extends FrontendController with I18nSupport {

  val authService: AuthService
  val baseConfig: BaseControllerConfig
  lazy implicit val applicationConfig = baseConfig.applicationConfig

  object Authenticated {
    type ActionBody = Request[AnyContent] => IncomeTaxSAUser => Future[Result]
    type AuthenticatedAction = ActionBody => Action[AnyContent]

    def apply(action: Request[AnyContent] => IncomeTaxSAUser => Result): Action[AnyContent] = async(action andThen (_ andThen Future.successful))

    val async: AuthenticatedAction = asyncInternal(subscriptionPredicates)

    val asyncEnrolled: AuthenticatedAction = asyncInternal(enrolledPredicates)

    val asyncForHomeController: AuthenticatedAction = { actionBody: ActionBody =>
      asyncInternal(homePredicates)({
        implicit request =>
          user =>
            actionBody(request)(user) map (_.addingToSession(ITSASessionKeys.GoHome -> "et"))
      })
    }

    private def asyncInternal(predicate: AuthPredicate)(action: ActionBody): Action[AnyContent] =
      Action.async { implicit request =>
        authService.authorised().retrieve(allEnrolments and affinityGroup) {
          case enrolments ~ affinity =>
            implicit val user = IncomeTaxSAUser(enrolments, affinity)

            predicate.apply(request)(user) match {
              case Right(AuthPredicateSuccess) => action(request)(user)
              case Left(failureResult) => failureResult
            }
        }
      }
  }

  implicit class FormUtil[T](form: Form[T]) {
    def fill(data: Option[T]): Form[T] = data.fold(form)(form.fill)
  }

}