package controllers

import Application._
import models._
import com.codahale.jerkson.Json
import play.api.libs.ws.WS

import play.api.mvc._

object Networks extends Controller with Secured {

  def index(network: String) = IsAuthenticated {
    username =>
      implicit request => {
        request.queryString.get("code") match {
          // try to get token, if creating campaign
          case Some(code) => {
            val response_token = WS.url(url_OAuthToken).post(
              "grant_type=authorization_code" +
                "&code=" + code(0) +
                "&client_id=" + app_id +
                "&client_secret=" + app_secret).value.get.body

            try {
              val net = "Yandex"
              val login = "vlad.ch01" //"krisp0" //45ps001
              val token = Json.parse[Map[String, String]](response_token).get("access_token").get

              /*
             * val response_login = WS.url(url_apiLogin).post(
             * "oauth_token=" + token).value.get.body
             */

              Redirect(routes.Networks.externalCampaigns(net, login, token))              

            } catch {
              //case t => Ok(views.html.index(t.toString()))
              case t => BadRequest
            }
          }
          case None => {
            network match {

              case "Yandex" => Ok(views.html.campaigns.yandex(username, "Yandex"))

              case "Google" => Ok(views.html.campaigns.google(username, "Goggle"))

              case "Begun" => Ok(views.html.campaigns.begun(username, "Begun"))

              case "" => Ok(views.html.campaigns.index(username))

            }
          }
        } 
      }
  }  
 
  def campaignReport(network: String, campaign: String) = IsAuthenticated {
    username => _ => Ok(views.html.reports.report(username, network, Json.parse[models.Campaign](campaign)))
  }
 
  def externalCampaigns(network: String, login: String, token: String) = IsAuthenticated {
    username => _ => Ok(views.html.campaigns.external(username, network, login, token))
  }

}