--    Licensed to the Apache Software Foundation (ASF) under one or more
--    contributor license agreements.  See the NOTICE file distributed with
--    this work for additional information regarding copyright ownership.
--    The ASF licenses this file to You under the Apache License, Version 2.0
--    (the "License"); you may not use this file except in compliance with
--    the License.  You may obtain a copy of the License at
--
--       http://www.apache.org/licenses/LICENSE-2.0
--
--    Unless required by applicable law or agreed to in writing, software
--    distributed under the License is distributed on an "AS IS" BASIS,
--    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
--    See the License for the specific language governing permissions and
--    limitations under the License.

-- Each SQL statement in this file should terminate with a semicolon (;)
-- Lines starting with the pound character (#) are considered as comments
-- DROP TABLE HOLDINGEJB;
-- DROP TABLE ACCOUNTPROFILEEJB;
-- DROP TABLE QUOTEEJB;
-- DROP TABLE KEYGENEJB;
-- DROP TABLE ACCOUNTEJB;
-- DROP TABLE ORDEREJB;

CREATE TABLE HOLDINGEJB
  (PURCHASEPRICE DECIMAL(14, 2),
   HOLDINGID INT NOT NULL PRIMARY KEY,
   QUANTITY FLOAT NOT NULL,
   PURCHASEDATE DATETIME,
   ACCOUNT_ACCOUNTID INT,
   QUOTE_SYMBOL VARCHAR(255));


CREATE TABLE ACCOUNTPROFILEEJB
  (ADDRESS VARCHAR(255),
   PASSWD VARCHAR(255),
   USERID VARCHAR(255) NOT NULL PRIMARY KEY,
   EMAIL VARCHAR(255),
   CREDITCARD VARCHAR(255),
   FULLNAME VARCHAR(255));


CREATE TABLE QUOTEEJB
  (LOW DECIMAL(14, 2),
   OPEN1 DECIMAL(14, 2),
   VOLUME FLOAT NOT NULL,
   PRICE DECIMAL(14, 2),
   HIGH DECIMAL(14, 2),
   COMPANYNAME VARCHAR(255),
   SYMBOL VARCHAR(255) NOT NULL PRIMARY KEY,
   CHANGE1 FLOAT NOT NULL);


CREATE TABLE KEYGENEJB
  (KEYVAL INT NOT NULL,
   KEYNAME VARCHAR(255) NOT NULL PRIMARY KEY);


CREATE TABLE ACCOUNTEJB
  (CREATIONDATE DATETIME,
   OPENBALANCE DECIMAL(14, 2),
   LOGOUTCOUNT INT NOT NULL,
   BALANCE DECIMAL(14, 2),
   ACCOUNTID INT NOT NULL PRIMARY KEY,
   LASTLOGIN DATETIME,
   LOGINCOUNT INT NOT NULL,
   PROFILE_USERID VARCHAR(255));


CREATE TABLE ORDEREJB
  (ORDERFEE DECIMAL(14, 2),
   COMPLETIONDATE DATETIME,
   ORDERTYPE VARCHAR(255),
   ORDERSTATUS VARCHAR(255),
   PRICE DECIMAL(14, 2),
   QUANTITY FLOAT NOT NULL,
   OPENDATE DATETIME,
   ORDERID INT NOT NULL PRIMARY KEY,
   ACCOUNT_ACCOUNTID INT,
   QUOTE_SYMBOL VARCHAR(255),
   HOLDING_HOLDINGID INT);

CREATE INDEX ACCOUNT_USERID ON ACCOUNTEJB(PROFILE_USERID);
CREATE INDEX HOLDING_ACCOUNTID ON HOLDINGEJB(ACCOUNT_ACCOUNTID);
CREATE INDEX ORDER_ACCOUNTID ON ORDEREJB(ACCOUNT_ACCOUNTID);
CREATE INDEX ORDER_HOLDINGID ON ORDEREJB(HOLDING_HOLDINGID);
CREATE INDEX CLOSED_ORDERS ON ORDEREJB(ACCOUNT_ACCOUNTID,ORDERSTATUS);
