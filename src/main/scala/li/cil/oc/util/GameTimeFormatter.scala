package li.cil.oc.util

import java.util.Calendar
import java.util.GregorianCalendar
import java.util.TimeZone

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
    'C' -> (t => f"${t.year / 100}%02d"),
    'd' -> (t => f"${t.day}%02d"),
    'D' -> (t => format("%m/%d/%y", t)),
    'e' -> (t => f"${t.day}% 2d"),
    'F' -> (t => format("%Y-%m-%d", t)),
    //'g' -> (t => ""),
    //'G' -> (t => ""),
    'h' -> (t => format("%b", t)),
    'H' -> (t => f"${t.hour}%02d"),
    'I' -> (t => f"${(t.hour + 11) % 12 + 1}%02d"),
    'j' -> (t => f"${t.yearDay}%03d"),
    'm' -> (t => f"${t.month}%02d"),
    'M' -> (t => f"${t.minute}%02d"),
    'n' -> (t => "\n"),
    'p' -> (t => amPm(if (t.hour < 12) 0 else 1)),
    'r' -> (t => format("%I:%M:%S %p", t)),
    'R' -> (t => format("%H:%M", t)),
    'S' -> (t => f"${t.second}%02d"),
    't' -> (t => "\t"),
    'T' -> (t => format("%H:%M:%S", t)),
    //'u' -> (t => ""),
    //'U' -> (t => ""),
    //'V' -> (t => ""),
    'w' -> (t => s"${t.weekDay - 1}"),
    //'W' -> (t => ""),
    'x' -> (t => format("%D", t)),
    'X' -> (t => format("%T", t)),
    'y' -> (t => f"${t.year % 100}%02d"),
    'Y' -> (t => f"${t.year}%04d"),
    //'z' -> (t => ""),
    //'Z' -> (t => ""),
    '%' -> (t => "%")
  )

  def parse(time: Double) = {
    val calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"))
    calendar.setTimeInMillis((time * 1000).toLong)

    new DateTime(
      calendar.get(Calendar.YEAR),
      calendar.get(Calendar.MONTH) + 1,
      calendar.get(Calendar.DAY_OF_MONTH),
      calendar.get(Calendar.DAY_OF_WEEK),
      calendar.get(Calendar.DAY_OF_YEAR),
      calendar.get(Calendar.HOUR_OF_DAY),
      calendar.get(Calendar.MINUTE),
      calendar.get(Calendar.SECOND))
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
    val calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"))
    calendar.set(Calendar.YEAR, year)
    calendar.set(Calendar.MONTH, mon - 1)
    calendar.set(Calendar.DAY_OF_MONTH, mday)
    calendar.set(Calendar.HOUR_OF_DAY, hour)
    calendar.set(Calendar.MINUTE, min)
    calendar.set(Calendar.SECOND, sec)

    Option((calendar.getTimeInMillis / 1000).toInt)
  }
}
