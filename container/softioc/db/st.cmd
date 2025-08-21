dbLoadRecords("/db/softioc.db")
dbLoadRecords("/db/hallmacros.db", "HALL=A")
dbLoadRecords("/db/hallmacros.db", "HALL=C")
dbLoadRecords("/db/hallmacros.db", "HALL=D")
dbLoadRecords("/db/icamacros.db", "IC=IIC1H00")
dbLoadRecords("/db/icamacros.db", "IC=IIC1H05")
dbLoadRecords("/db/icamacros.db", "IC=IIC1H04A")
dbLoadRecords("/db/icamacros.db", "IC=IIC1P03")
dbLoadRecords("/db/icamacros.db", "IC=IICD107")
dbLoadRecords("/db/icamacros.db", "IC=IICD107A")

iocInit
iocRun