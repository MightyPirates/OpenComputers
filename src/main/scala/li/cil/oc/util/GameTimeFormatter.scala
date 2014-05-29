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
    'a' -> (t => shortWeekDays(t.weekDay - 1)),
    'A' -> (t => weekDays(t.weekDay - 1)),
    'b' -> (t => shortMonths(t.month - 1)),
    'B' -> (t => months(t.month - 1)),
    'c' -> (t => format("%a %b %d %H:%M:%S %Y", t)),
    'C' -> (t => "%02d".format(t.year / 100)),
    'd' -> (t => "%02d".format(t.day)),
    'D' -> (t => format("%m/%d/%y", t)),
    'e' -> (t => "% 2d".format(t.day)),
    'F' -> (t => format("%Y-%m-%d", t)),
    //'g' -> (t => ""),
    //'G' -> (t => ""),
    'h' -> (t => format("%b", t)),
    'H' -> (t => "%02d".format(t.hour)),
    'I' -> (t => "%02d".format((t.hour + 11) % 12 + 1)),
    'j' -> (t => "%03d".format(t.yearDay)),
    'm' -> (t => "%02d".format(t.month)),
    'M' -> (t => "%02d".format(t.minute)),
    'n' -> (t => "\n"),
    'p' -> (t => amPm(if (t.hour < 12) 0 else 1)),
    'r' -> (t => format("%I:%M:%S %p", t)),
    'R' -> (t => format("%H:%M", t)),
    'S' -> (t => "%02d".format(t.second)),
    't' -> (t => "\t"),
    'T' -> (t => format("%H:%M:%S", t)),
    //'u' -> (t => ""),
    //'U' -> (t => ""),
    //'V' -> (t => ""),
    'w' -> (t => "%d".format(t.weekDay - 1)),
    //'W' -> (t => ""),
    'x' -> (t => format("%D", t)),
    'X' -> (t => format("%T", t)),
    'y' -> (t => "%02d".format(t.year % 100)),
    'Y' -> (t => "%04d".format(t.year)),
    //'z' -> (t => ""),
    //'Z' -> (t => ""),
    '%' -> (t => "%")
  )

  private val monthLengths = Array(
    Array(31, 29, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31),
    Array(31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31))

  private def monthLengthsForYear(year: Int) = {
    if (year % 4 == 0 && year % 100 != 0 || year % 400 == 0) monthLengths(0) else monthLengths(1)
  }

  def parse(time: Double) = {
    var day = (time / 24000).toLong
    val weekDay = ((4 + day) % 7).toInt
    val year = 1970 + (day / 365.2425).toInt
    val yearDay = (day % 365.2425).toInt
    day = yearDay
    val monthLengths = monthLengthsForYear(year)
    var month = 0
    while (day > monthLengths(month)) {
      day = day - monthLengths(month)
      month = month + 1
    }

    var seconds = ((time % 24000) * 60 * 60 / 1000).toInt
    var minutes = seconds / 60
    seconds = seconds % 60
    val hours = (1 + minutes / 60) % 24
    minutes = minutes % 60

    new DateTime(year, month + 1, day.toInt + 1, weekDay + 1, yearDay + 1, hours, minutes, seconds)
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

  def mktime(year: Int, mon: Int, mday: Int, hour: Int, min: Int, sec: Int): Option[Int] = {
    if (year < 1970 || mon < 1 || mon > 12) return None
    val monthLengths = monthLengthsForYear(year)
    val days = ((year - 1970) * 365.2425).ceil.toInt + (0 until mon - 1).foldLeft(0)((d, m) => d + monthLengths(m)) + mday - 1
    val secs = sec + (min + (hour - 1 + days * 24) * 60) * 60
    if (secs < 0) None
    else Option(secs)
  }
}
