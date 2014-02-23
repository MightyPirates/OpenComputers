package li.cil.oc.util

import scala.collection.mutable

object GameTimeFormatter {
  // Locale? What locale? Seriously though, since this would depend on the
  // server's locale I think it makes more sense to keep it English always.
  private val weekDays = Array("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
  private val shortWeekDays = Array("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
  private val months = Array("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
  private val shortMonths = Array("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
  private val amPm = Array("AM", "PM")

  class DateTime(val year: Int, val month: Int, val day: Int,
                 val weekDay: Int, val yearDay: Int,
                 val hour: Int, val minute: Int, val second: Int)

  // See http://www.cplusplus.com/reference/ctime/strftime/
  private val specifiers: Map[Char, (DateTime) => String] = Map(
    'a' -> (t => shortWeekDays(t.weekDay)),
    'A' -> (t => weekDays(t.weekDay)),
    'b' -> (t => shortMonths(t.month)),
    'B' -> (t => months(t.month)),
    'c' -> (t => format("%a %b %d %H:%M:%S %Y", t)),
    'C' -> (t => "%02d".format(t.year / 100)),
    'd' -> (t => "%02d".format(t.day + 1)),
    'D' -> (t => format("%m/%d/%y", t)),
    'e' -> (t => "% 2d".format(t.day + 1)),
    'F' -> (t => format("%Y-%m-%d", t)),
    //'g' -> (t => ""),
    //'G' -> (t => ""),
    'h' -> (t => format("%b", t)),
    'H' -> (t => "%02d".format(t.hour)),
    'I' -> (t => "%02d".format(t.hour % 12 + 1)),
    'j' -> (t => "%03d".format(t.yearDay)),
    'm' -> (t => "%02d".format(t.month + 1)),
    'M' -> (t => "%02d".format(t.minute)),
    'n' -> (t => "\n"),
    'p' -> (t => amPm(if (t.hour < 12) 0 else 1)),
    'r' -> (t => format("%I:%M:%S %p", t)),
    'R' -> (t => format("%H:%M", t)),
    'S' -> (t => "%02d".format(t.second)),
    't' -> (t => "\t"),
    'T' -> (t => format("%H:%M:%S", t)),
    'u' -> (t => ""),
    //'U' -> (t => ""),
    //'V' -> (t => ""),
    'w' -> (t => "%d".format(t.weekDay)),
    //'W' -> (t => ""),
    'x' -> (t => format("%D", t)),
    'X' -> (t => format("%T", t)),
    'y' -> (t => "%02d".format(t.year % 100)),
    'Y' -> (t => "%04d".format(t.year)),
    //'z' -> (t => ""),
    //'Z' -> (t => ""),
    '%' -> (t => "%")
  )

  def parse(time: Double) = {
    var day = (time / 24000).toLong
    val weekDay = ((4 + day) % 7).toInt
    val year = 1970 + (day / 365.2425).toInt
    val yearDay = (day % 365.2425).toInt
    day = yearDay
    val monthLengths =
      if (year % 4 == 0 && year % 100 != 0 || year % 400 == 0)
        Array(31, 29, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
      else
        Array(31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
    var month = 0
    while (day > monthLengths(month)) {
      day = day - monthLengths(month)
      month = month + 1
    }

    var seconds = ((time % 24000) * 60 * 60 / 1000).toInt
    var minutes = seconds / 60
    seconds = seconds % 60
    val hours = (minutes / 60) % 24
    minutes = minutes % 60

    new DateTime(year, month, day.toInt, weekDay, yearDay, hours, minutes, seconds)
  }

  def format(format: String, time: DateTime) = {
    val result = new mutable.StringBuilder()
    val iterator = format.iterator
    while (iterator.hasNext) {
      iterator.next() match {
        case '%' if iterator.hasNext =>
          specifiers.get(iterator.next()) match {
            case Some(specifier) => result.append(specifier(time))
            case _ =>
          }
        case c => result.append(c)
      }
    }
    result.toString()
  }
}
