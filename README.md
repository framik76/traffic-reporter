This exercise implements a Java method in order to produce the following report /reports/ipaddr.csv 

The created report is a text/csv file containing the traffic data per IP Address.

Each rows have the following fields :
1. IP Address 
2. Number of requests
3. Percentage of the total amount of requests
4. Total Bytes sent
5. Percentage of the total amount of bytes

The data set is sorted by the number of requests (DESC). 

An example of the source data for the report is stored in the file /logfiles/requests.log 
where each row (record) contains the following semicolon-separated values:
1. TIMESTAMP: the moment when the event occurred.
2. BYTES: the number of bytes sent to a client.
3. STATUS: HTTP response status.
4. REMOTE_ADDR: IP address of the client.

The lines in the source file where the STATUS is different from “OK” (RFC 2616) are excluded to the report.

Note:

• No external libraries to the Java Standard Edition are used to implement this app

• The user can choose between two possible output modes on daily report file: csv o
r json
