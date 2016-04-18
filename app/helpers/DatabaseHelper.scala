// Webside Source Code Zauberstuhl.de
// Copyright (C) 2016  Lukas Matt <lukas@zauberstuhl.de>
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <http://www.gnu.org/licenses/>.
//
package helpers

import play.api.Logger
import play.api.db.DB
import play.api.Play.current

import anorm._
import anorm.SqlParser.get

import objects.Database.{DonationList, Donation}
import objects.Provider.Provider

object DatabaseHelper {
  val donations = {
    get[Double]("received") ~
    get[String]("currency") ~
    get[String]("provider") ~
    get[Int]("time") map {
      case received~currency~provider~time =>
        Donation(received, currency, provider, time)
    }
  }

  def insert(d: Donation): Boolean = DB.withConnection { implicit c =>
    Logger.debug("Insert into donations: " + d.toString)

    SQL("""insert into donations(received, currency, provider, time)
      values ({received}, {currency}, {provider}, {time});""")
    .on(
      'received -> d.received,
      'currency -> d.currency.toUpperCase,
      'provider -> d.provider,
      'time -> d.time)
    .execute()
  }

  def selectAll: DonationList = DB.withConnection { implicit c =>
    // NOTE select almost all since 2016
    // where strftime('%Y', time, 'unixepoch') == strftime('%Y');""")
    new DonationList(
      SQL("""select received, currency, provider, time
        from donations
        where strftime('%Y', time, 'unixepoch') >= "2016";""")
      .as(donations *)
    )
  }

  def selectAllFromThisYear: DonationList = DB.withConnection { implicit c =>
    new DonationList(
      SQL("""select received, currency, provider, time
        from donations
        where strftime('%Y', time, 'unixepoch') == strftime('%Y');""")
      .as(donations *)
    )
  }

  def firstEntry: Option[Donation] = DB.withConnection { implicit c =>
    //SQL("""select received, currency, provider, time
    //  from donations
    //  order by time ASC
    //  limit 1;""")
    //.as(donations *)
    //.headOption
    //
    // NOTE this is a hack since we received donations before 2016
    // which shouldn't be used for calculation
    Option(Donation(0.0, "EUR", "DoNotStartFromBeginning", 1460983688))
  }

  def lastEntry(p: Provider): Option[Donation] = DB.withConnection { implicit c =>
    SQL("""select received, currency, provider, time
      from donations
      where provider like '""" + p.name + """'
      order by time DESC
      limit 1;""")
    .as(donations *)
    .headOption
  }
}
