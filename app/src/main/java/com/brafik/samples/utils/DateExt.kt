package com.brafik.samples.utils

import java.text.DateFormat
import java.util.Date

fun Date.toReadableString(): String = let { DateFormat.getDateTimeInstance().format(it) }