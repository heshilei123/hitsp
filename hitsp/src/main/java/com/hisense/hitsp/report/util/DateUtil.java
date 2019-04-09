package com.hisense.hitsp.report.util;

import java.util.Calendar;
import java.util.Date;

/**
 * @author yanglei
 * date: 2017-08-02.
 */
public class DateUtil {

    public static Date getOneDay(Date date, int days, boolean isZero) {
        Calendar oneDay = Calendar.getInstance();
        oneDay.setTime(date);
        oneDay.add(Calendar.DAY_OF_MONTH, days);
        if (!isZero) {
            oneDay.set(Calendar.HOUR_OF_DAY, 23);
            oneDay.set(Calendar.MINUTE, 59);
            oneDay.set(Calendar.SECOND, 59);
            oneDay.set(Calendar.MILLISECOND, 999);
        } else {
            oneDay.set(Calendar.HOUR_OF_DAY, 0);
            oneDay.set(Calendar.MINUTE, 0);
            oneDay.set(Calendar.SECOND, 0);
            oneDay.set(Calendar.MILLISECOND, 0);
        }

        return oneDay.getTime();
    }
}
