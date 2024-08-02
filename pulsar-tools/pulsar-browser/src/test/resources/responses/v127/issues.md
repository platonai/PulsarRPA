Issues
=

ResponseReceivedExtraInfo problem
==

com.fasterxml.jackson.databind.exc.MismatchedInputException: Cannot deserialize value of type `java.lang.String` from Object value (token `JsonToken.START_OBJECT`)
at [Source: UNKNOWN; byte offset: #UNKNOWN] (through reference chain: com.github.kklisura.cdt.protocol.v2023.events.network.ResponseReceivedExtraInfo["cookiePartitionKey"])

Solution:

remove cookiePartitionKey from ResponseReceivedExtraInfo
