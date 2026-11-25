package edu.ap.tobadtoostay.ui

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 17:30 */
fun formatTime(millis: Long): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(millis))

/** 25/11 */
fun formatDay(millis: Long): String =
    SimpleDateFormat("dd/MM", Locale.getDefault()).format(Date(millis))
