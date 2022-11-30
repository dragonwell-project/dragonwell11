package jdk.jfr.startupargs;

import java.io.IOException;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;

/**
 * @test
 * @summary Checks that locale is respected when using -XX:FlightRecorderOptions
 *          See JDK-8244508
 * @key jfr
 * @requires vm.hasJFR
 * @modules jdk.jfr
 * @library /test/lib
 * @run main jdk.jfr.startupargs.TestOptionsWithLocale
 */
public class TestOptionsWithLocale {

    public static class PrintDate {
        public static void main(String... args) {
            GregorianCalendar date = new GregorianCalendar(2020, Calendar.JANUARY, 1);
            DateFormat formatter = DateFormat.getDateTimeInstance();
            System.out.println(formatter.format(date.getTime()));
        }
    }

    public static void main(String... args) throws Exception {
        OutputAnalyzer output = ProcessTools.executeTestJvm(
                "-Duser.country=DE",
                "-Duser.language=de",
                "-XX:FlightRecorderOptions:stackdepth=128",
                PrintDate.class.getName());
        output.shouldContain("01.01.2020, 00:00:00");
    }
}
