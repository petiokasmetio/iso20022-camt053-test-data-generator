# ISO 20022 camt.053 Test Data Generator

Java console application for generating synthetic camt.053-style XML account statement test data.

This project demonstrates how QA engineers can use automation to generate repeatable banking test data for account statement, transaction and balance validation scenarios.

## Disclaimer

This is a portfolio/demo project using synthetic data only.

It does not contain real customer data, real bank data, real account data, production identifiers, internal project information or confidential client information.

The generated XML files are intended for QA practice, learning, demonstration and portfolio purposes only.

## Overview

The application generates a `camt.053.001.02`-style XML file using console input.

The user can configure account statement information such as:

- message recipient
- account owner
- bank/servicer details
- branch details
- opening booked balance
- transaction entries
- credit/debit indicators
- booking and value dates
- remittance information

The tool then builds an XML document containing account statement data and automatically calculates the closing booked balance.

## Why This Tool Exists

In banking and financial software testing, preparing XML-based account statement test data manually can be slow, repetitive and error-prone.

This tool demonstrates a simple approach for generating structured banking test data automatically, making test scenarios more repeatable and easier to prepare.

Typical QA use cases include:

- regression testing
- banking workflow validation
- account statement scenario preparation
- transaction and balance testing
- XML-based test data generation
- financial business rule validation

## Key Features

- Generates camt.053-style XML account statement files
- Creates a `BkToCstmrStmt` XML structure
- Generates `GrpHdr`, `Stmt`, `Acct`, `Bal` and `Ntry` sections
- Supports multiple transaction entries
- Supports `CRDT` and `DBIT` transaction types
- Randomizes transaction amounts within a user-defined range
- Calculates closing booked balance automatically
- Generates message ID, statement ID and electronic sequence number
- Generates account servicer references
- Uses `BigDecimal` for financial amount calculations
- Uses synthetic demo values by default
- Does not require external libraries

## Technologies Used

- Java
- Java XML DOM API
- BigDecimal
- Console input handling
- ISO 20022 camt.053-style XML structure

## Balance Calculation Logic

The application asks for an opening booked balance and its credit/debit indicator.

Each generated transaction entry can be either:

- `CRDT` - credit transaction
- `DBIT` - debit transaction

The closing booked balance is calculated using the following logic:

```text
Closing Balance = Opening Balance + Credit Entries - Debit Entries
```

If the final balance is positive or zero, the closing balance indicator is set to:

```text
CRDT
```

If the final balance is negative, the closing balance indicator is set to:

```text
DBIT
```

The absolute value of the calculated balance is written as the closing balance amount.

## Generated XML Structure

The generated XML contains the following high-level structure:

```text
Document
└── BkToCstmrStmt
    ├── GrpHdr
    │   ├── MsgId
    │   ├── CreDtTm
    │   ├── MsgRcpt
    │   └── MsgPgntn
    └── Stmt
        ├── Id
        ├── ElctrncSeqNb
        ├── CreDtTm
        ├── FrToDt
        ├── Acct
        ├── Bal - OPBD
        ├── Bal - CLBD
        └── Ntry
            └── NtryDtls
                └── TxDtls
```

## Example Generated Sections

The generated XML includes:

- group header data
- statement metadata
- account information
- account owner information
- financial institution/servicer information
- branch information
- opening booked balance
- calculated closing booked balance
- transaction entries
- debtor information
- debtor account information
- remittance information

## How to Compile

From the project folder, run:

```bash
javac -encoding UTF-8 Camt053Generator.java
```

## How to Run

```bash
java -cp . Camt053Generator
```

The application will start asking for input values through the console.

Example:

```text
=== CAMT.053.001.02 XML Generator (Java) ===
Output filename (example: statement.xml) [default: camt053_generated.xml]:
Timezone offset (example: +02:00) [default: +02:00]:
Creation date for message/statement (YYYY-MM-DD):
```

## Example Input Flow

The user can define:

```text
Output filename
Timezone offset
Creation date
Recipient name
Customer ID
Statement date range
Account ID
Owner details
Servicer/bank details
Branch details
Opening balance
Transaction amount range
Number of transaction entries
CRDT/DBIT indicator per entry
Booking date
Value date
Transaction code values
Debtor information
Remittance information
```

## Demo Data

The project uses synthetic demo values such as:

```text
Demo Recipient Ltd
DEMO-CUSTOMER-001
Demo Account Owner Ltd
Demo Bank Ltd
Demo Bank Branch
Demo Debtor Ltd
DEMO SEPA TEST PAYMENT
DEMO TRANSACTION INFO
```

These values are placeholders and are not related to real customers, banks, accounts or production systems.

## Example Use Case

A QA engineer needs to test account statement processing for a banking system.

Instead of preparing XML test data manually, the engineer can use this tool to generate account statement XML files with configurable balances and transaction entries.

The generated output can then be used as test data for regression testing, integration testing or business rule validation.

## Current Project Status

Current version:

- single-file Java console application
- synthetic demo data
- XML generation through Java DOM API
- configurable console prompts
- multiple transaction entries
- credit/debit handling
- automatic closing balance calculation

## Limitations

This is a demo/portfolio version.

Current limitations:

- no Maven/Gradle project structure yet
- no unit tests yet
- no external configuration file support yet
- no XML schema validation yet
- no sample XML file committed yet
- no CI/CD pipeline yet

## Potential Future Improvements

Possible next improvements:

- add Maven project structure
- add unit tests for balance calculation
- add XML schema validation
- add JSON or YAML input configuration
- add sample input file
- add sample generated XML file
- add GitHub Actions build pipeline
- add negative test data generation
- add support for additional camt message variations
- add command-line arguments instead of interactive prompts

## Skills Demonstrated

This project demonstrates:

- Java programming
- XML generation
- test data automation
- financial calculation logic
- banking domain understanding
- QA automation mindset
- structured test data preparation
- handling of credit/debit transaction scenarios
- synthetic data generation for testing purposes

## Author

Petar Nikolov

Test Automation Engineering Analyst with experience in ERP and banking systems, Java/Python automation, QA tooling, XML test data generation and business-critical software testing.