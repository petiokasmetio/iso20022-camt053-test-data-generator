# ISO 20022 camt.053 Test Data Generator

Java-based console application for generating demo camt.053-style XML account statement files for QA and test data preparation.

This project is intended as a portfolio/demo tool that demonstrates test data generation for banking account statement scenarios, transaction entries and balance calculation logic.

## Disclaimer

This project uses synthetic demo data only.

It does not contain real customer data, real bank data, real account data, production identifiers, internal project information or confidential client information.

The generated XML files are intended for testing, learning and portfolio demonstration purposes only.

## What the Tool Does

The application generates a camt.053.001.02-style XML file with configurable account statement data.

It allows the user to define message, account, servicer, branch, balance and transaction-related information through console prompts.

The tool can generate multiple transaction entries and automatically calculate the closing booked balance based on the opening balance and generated credit/debit entries.

## Key Features

- Generates camt.053.001.02-style XML files
- Creates a `BkToCstmrStmt` account statement structure
- Generates `GrpHdr`, `Stmt`, `Acct`, `Bal` and `Ntry` XML sections
- Supports configurable recipient, account owner, servicer and branch data
- Supports multiple transaction entries
- Supports CRDT and DBIT transaction types
- Randomizes transaction amounts within a user-defined range
- Calculates closing booked balance automatically
- Generates unique message ID, statement ID and electronic sequence number
- Generates account servicer references
- Uses synthetic demo data by default

## Technologies Used

- Java
- XML DOM API
- BigDecimal for financial calculations
- Console-based input handling
- ISO 20022 camt.053-style XML structure

## Business Logic Overview

The tool asks for an opening booked balance and whether it is a credit or debit balance.

Each generated transaction entry can be either:

- `CRDT` - credit transaction
- `DBIT` - debit transaction

The closing booked balance is calculated using the following logic:

Closing Balance = Opening Balance + Credit Entries - Debit Entries

How to compile:
javac -encoding UTF-8 Camt053Generator.java

How to run:
java -cp . Camt053Generator

The application will start asking for input values through the console.

Example Use Case

This tool can be used to generate synthetic banking statement test data for:

QA testing, Regression testing, Banking workflow validation, Account statement scenario preparation, XML-based test data generation practice, Demonstrating test data automation skills

# Project Status

Current version:

Console-based Java application
Single-file implementation
Synthetic demo data
XML generation through Java DOM API
Automatic balance calculation

Potential future improvements:

Maven project structure
Unit tests for balance calculation
JSON/YAML input configuration
XML schema validation
Sample input file
Sample generated XML file
GitHub Actions build pipeline
