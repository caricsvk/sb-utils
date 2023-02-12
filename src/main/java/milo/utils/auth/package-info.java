@XmlJavaTypeAdapters({
		@XmlJavaTypeAdapter(value = ZonedDateTimeToLong.class, type = ZonedDateTime.class),
		@XmlJavaTypeAdapter(value = LocalDateTimeToString.class, type = LocalDateTime.class),
		@XmlJavaTypeAdapter(value = LocalTimeToString.class, type = LocalTime.class)
})
package milo.utils.auth;

import milo.utils.rest.jaxbadapters.LocalDateTimeToString;
import milo.utils.rest.jaxbadapters.LocalTimeToString;
import milo.utils.rest.jaxbadapters.ZonedDateTimeToLong;

import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapters;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
