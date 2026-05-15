import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * CAMT.053.001.02 XML generator (console app).
 * Portfolio/demo version using synthetic data only.
 * This project does not contain real customer, bank, account,
 * transaction, production or internal project information.
 *
 * - Prompts are in ENGLISH.
 * - Generates unique MsgId / Stmt Id / random ElectronicSeqNb.
 * - Asks how many <Ntry> blocks to create, and whether each is CRDT or DBIT.
 * - Randomizes amounts within a user-defined range.
 * - Calculates closing booked balance (CLBD) from opening booked balance (OPBD)
 * + entries.
 *
 * Compile:
 * javac Camt053Generator.java
 *
 * Run:
 * java Camt053Generator
 */
public class Camt053Generator {

    // Namespaces
    private static final String NS = "urn:iso:std:iso:20022:tech:xsd:camt.053.001.02";
    private static final String XSI = "http://www.w3.org/2001/XMLSchema-instance";
    private static final String XMLNS = "http://www.w3.org/2000/xmlns/";
    private static final String SCHEMA_LOCATION = NS + " camt.053.001.02.xsd";

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ROOT);
    private static final DateTimeFormatter TIME_HMS = DateTimeFormatter.ofPattern("HH:mm:ss", Locale.ROOT);

    private static final Random RNG = new Random();

    private static final Pattern ACCT_SHORT_PATTERN = Pattern.compile("/(\\d{6,20})");

    private static BufferedReader IN;

    public static void main(String[] args) {
        try {
            IN = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
            System.out.println("=== CAMT.053.001.02 XML Generator (Java) ===");

            String outFile = promptString("Output filename", "camt053_generated.xml", "statement.xml");

            String tz = promptString("Timezone offset", "+02:00", "+02:00");

            LocalDate creationDate = promptDate("Creation date for message/statement (YYYY-MM-DD)", LocalDate.now());

            String msgRcptName = promptString("MsgRcpt/Nm (recipient name)", null, "Demo Recipient Ltd");
            String ebicsCustomerId = promptString("EBICS Customer ID", null, "DEMO-CUSTOMER-001");

            LocalDate fromDate = promptDate("FrToDt/FrDtTm date (YYYY-MM-DD)", creationDate);
            LocalDate toDate = promptDate("FrToDt/ToDtTm date (YYYY-MM-DD)", creationDate);

            String acctOtherId = promptString("Acct/Id/Othr/Id (account id)", null, "10000000/999999999EUR");
            String acctShort = extractAcctShort(acctOtherId);
            if (acctShort == null)
                acctShort = randomDigits(9);

            String msgIdMiddle = promptString("MsgId middle code (after currency)", "DEMOREF", "DEMOREF");

            // Owner
            String ownerName = promptString("Account owner name (Ownr/Nm)", null, "Demo Account Owner Ltd");
            String ownerAdrTp = promptString("Owner address type (Ownr/PstlAdr/AdrTp)", "ADRR", "ADRR");
            String ownerAdrLine1 = promptString("Owner address line 1", null, "Demo Street 1");
            String ownerAdrLine2 = promptString("Owner address line 2", null, "10000");
            String ownerAdrLine3 = promptString("Owner address line 3", null, "Demo City");
            String ownerBicOrBei = promptString("Owner BICOrBEI (Ownr/Id/OrgId/BICOrBEI)", null, "DEMOORG0XXX");

            // Servicer
            String svcrBic = promptString("Servicer BIC (Svcr/FinInstnId/BIC)", null, "DEMOBAK0XXX");
            String svcrName = promptString("Servicer name (Svcr/FinInstnId/Nm)", null, "Demo Bank Ltd");
            String svcrAdrTp = promptString("Servicer address type (Svcr/FinInstnId/PstlAdr/AdrTp)", "ADRR", "ADRR");
            String svcrAdrLine = promptString("Servicer address line", null, "Demo Bank Address");
            String svcrOtherId = promptString("Servicer other ID (Svcr/FinInstnId/Othr/Id)", null, "DEMO-ISSUER-ID");
            String svcrIssr = promptString("Servicer issuer (Svcr/FinInstnId/Othr/Issr)", "DEMO", "DEMO");

            // Branch
            String branchId = promptString("Branch ID (Svcr/FinInstnId/BrnchId/Id)", null, "DEMOBAK0XXX");
            String branchName = promptString("Branch name (Svcr/FinInstnId/BrnchId/Nm)", null, "Demo Bank Branch");
            String branchAdrTp = promptString("Branch address type (BrnchId/PstlAdr/AdrTp)", "ADRR", "ADRR");
            String branchAdrLine1 = promptString("Branch address line 1", null, "Demo Branch Street 1");
            String branchAdrLine2 = promptString("Branch address line 2", null, "10000 Demo City");

            // Opening balance (OPBD)
            BigDecimal openingBal = promptDecimal("Opening booked balance amount (OPBD)", new BigDecimal("1000.00"));
            String openingInd = promptChoice("Opening balance CdtDbtInd (CRDT or DBIT)", "CRDT",
                    new String[] { "CRDT", "DBIT" });

            LocalDate defaultOpeningDate = fromDate.minusDays(1);
            LocalDate openingDate = promptDate("Opening balance date (OPBD/Dt) (YYYY-MM-DD)", defaultOpeningDate);

            // Entry randomization range
            BigDecimal minAmt = promptDecimal("Random entry min amount", new BigDecimal("10.00"));
            BigDecimal maxAmt = promptDecimal("Random entry max amount", new BigDecimal("500.00"));
            if (maxAmt.compareTo(minAmt) < 0) {
                System.out.println("Max amount was smaller than min amount. Swapping them.");
                BigDecimal tmp = minAmt;
                minAmt = maxAmt;
                maxAmt = tmp;
            }

            int nEntries = promptInt("How many <Ntry> entries do you want to generate?", 1, 1, 10_000);
            Entry[] entries = new Entry[nEntries];
            for (int i = 0; i < nEntries; i++) {
                String ind = promptChoice("Entry #" + (i + 1) + " type (CRDT or DBIT)", "CRDT",
                        new String[] { "CRDT", "DBIT" });
                BigDecimal amt = randomAmount(minAmt, maxAmt);
                entries[i] = new Entry(ind, amt);
            }

            LocalDate bookDate = promptDate("Booking date for entries (BookgDt/Dt) (YYYY-MM-DD)", toDate);
            LocalDate valDate = promptDate("Value date for entries (ValDt/Dt) (YYYY-MM-DD)", toDate);

            String acctSvcrRefPrefix = promptString("AcctSvcrRef prefix", "1", "1");

            // Ntry/BkTxCd/Prtry values
            String ntryPrtryCd = promptString("Ntry/BkTxCd/Prtry/Cd", "999", "999");
            String ntryPrtryIssr = promptString("Ntry/BkTxCd/Prtry/Issr", "DEMO", "DEMO");

            // TxDtls/BkTxCd/Prtry values
            String txPrtryCd = promptString("TxDtls/BkTxCd/Prtry/Cd", "DEMO+999+00000", "DEMO+999+00000");
            String txPrtryIssr = promptString("TxDtls/BkTxCd/Prtry/Issr", "DEMO", "DEMO");

            // Debtor & remittance (shared across all entries)
            String debtorName = promptString("Debtor name (RltdPties/Dbtr/Nm)", "Demo Debtor Ltd", "Demo Debtor Ltd");
            String debtorIban = promptString("Debtor IBAN (RltdPties/DbtrAcct/Id/IBAN)", "DEMOIBAN00000000000000",
                    "DE89370400440532013000");
            String debtorAgentBic = promptString("Debtor agent BIC (RltdAgts/DbtrAgt/FinInstnId/BIC)", svcrBic,
                    "DEMOBAK0XXX");

            String ustrd = promptString("Remittance info Ustrd (RmtInf/Ustrd)", "DEMO SEPA TEST PAYMENT",
                    "DEMO SEPA TEST PAYMENT");
            String addtlNtryInf = promptString("AddtlNtryInf", "DEMO TRANSACTION INFO", "DEMO TRANSACTION INFO");

            // Build XML
            Document xml = buildXml(
                    creationDate, tz, msgRcptName, ebicsCustomerId,
                    fromDate, toDate,
                    acctOtherId, acctShort, msgIdMiddle,
                    ownerName, ownerAdrTp, ownerAdrLine1, ownerAdrLine2, ownerAdrLine3, ownerBicOrBei,
                    svcrBic, svcrName, svcrAdrTp, svcrAdrLine, svcrOtherId, svcrIssr,
                    branchId, branchName, branchAdrTp, branchAdrLine1, branchAdrLine2,
                    openingBal, openingInd, openingDate,
                    entries, bookDate, valDate,
                    acctSvcrRefPrefix,
                    ntryPrtryCd, ntryPrtryIssr,
                    txPrtryCd, txPrtryIssr,
                    debtorName, debtorIban, debtorAgentBic,
                    ustrd, addtlNtryInf);

            writeXml(xml, outFile);
            System.out.println("✅ XML written to: " + outFile);

        } catch (Exception e) {
            System.err.println("ERROR: " + e.getMessage());
            e.printStackTrace(System.err);
            System.exit(1);
        }
    }

    private static class Entry {
        final String ind; // CRDT / DBIT
        final BigDecimal amt; // 2dp

        Entry(String ind, BigDecimal amt) {
            this.ind = ind;
            this.amt = amt;
        }
    }

    // -------------------- XML BUILDING --------------------

    private static Document buildXml(
            LocalDate creationDate,
            String tz,
            String msgRcptName,
            String ebicsCustomerId,
            LocalDate fromDate,
            LocalDate toDate,
            String acctOtherId,
            String acctShort,
            String msgIdMiddle,
            String ownerName,
            String ownerAdrTp,
            String ownerAdrLine1,
            String ownerAdrLine2,
            String ownerAdrLine3,
            String ownerBicOrBei,
            String svcrBic,
            String svcrName,
            String svcrAdrTp,
            String svcrAdrLine,
            String svcrOtherId,
            String svcrIssr,
            String branchId,
            String branchName,
            String branchAdrTp,
            String branchAdrLine1,
            String branchAdrLine2,
            BigDecimal openingBal,
            String openingInd,
            LocalDate openingDate,
            Entry[] entries,
            LocalDate bookDate,
            LocalDate valDate,
            String acctSvcrRefPrefix,
            String ntryPrtryCd,
            String ntryPrtryIssr,
            String txPrtryCd,
            String txPrtryIssr,
            String debtorName,
            String debtorIban,
            String debtorAgentBic,
            String ustrd,
            String addtlNtryInf) throws Exception {

        int seq = randomInt(1, 9999);
        String yyyymmdd = creationDate.format(DateTimeFormatter.BASIC_ISO_DATE);

        String msgId = genMsgId(yyyymmdd, acctShort, "EUR", msgIdMiddle, seq);
        String stmtId = genStmtId(yyyymmdd, acctShort, "EUR", seq);

        String grpHdrCreDtTm = fmtDtWithMsAndTz(creationDate, LocalTime.now().withNano(0), tz);
        String stmtCreDtTm = fmtDtWithMsAndTz(toDate, LocalTime.of(23, 59, 59), tz);

        String frDtTm = fmtDtNoMs(fromDate, LocalTime.of(0, 0, 0), tz);
        String toDtTm = fmtDtNoMs(toDate, LocalTime.of(23, 59, 59), tz);

        // Closing balance calculation:
        BigDecimal signedOpen = openingInd.equals("CRDT") ? openingBal : openingBal.negate();
        BigDecimal signedDelta = BigDecimal.ZERO;
        for (Entry e : entries) {
            if (e.ind.equals("CRDT"))
                signedDelta = signedDelta.add(e.amt);
            else
                signedDelta = signedDelta.subtract(e.amt);
        }
        BigDecimal signedClose = signedOpen.add(signedDelta);
        String closeInd = signedClose.signum() >= 0 ? "CRDT" : "DBIT";
        BigDecimal closeAmt = signedClose.abs().setScale(2, BigDecimal.ROUND_HALF_UP);

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.newDocument();

        Element root = doc.createElementNS(NS, "Document");
        // Namespace declarations
        root.setAttributeNS(XMLNS, "xmlns", NS);
        root.setAttributeNS(XMLNS, "xmlns:xsi", XSI);
        root.setAttributeNS(XSI, "xsi:schemaLocation", SCHEMA_LOCATION);
        doc.appendChild(root);

        Element bk = el(doc, root, "BkToCstmrStmt");

        // GrpHdr
        Element grpHdr = el(doc, bk, "GrpHdr");
        elText(doc, grpHdr, "MsgId", msgId);
        elText(doc, grpHdr, "CreDtTm", grpHdrCreDtTm);

        Element msgRcpt = el(doc, grpHdr, "MsgRcpt");
        elText(doc, msgRcpt, "Nm", msgRcptName);
        Element msgRcptId = el(doc, msgRcpt, "Id");
        Element orgId = el(doc, msgRcptId, "OrgId");
        Element othr = el(doc, orgId, "Othr");
        elText(doc, othr, "Id", ebicsCustomerId);

        Element msgPgntn = el(doc, grpHdr, "MsgPgntn");
        elText(doc, msgPgntn, "PgNb", "1");
        elText(doc, msgPgntn, "LastPgInd", "true");

        // Stmt
        Element stmt = el(doc, bk, "Stmt");
        elText(doc, stmt, "Id", stmtId);
        elText(doc, stmt, "ElctrncSeqNb", String.valueOf(seq));
        elText(doc, stmt, "CreDtTm", stmtCreDtTm);

        Element frToDt = el(doc, stmt, "FrToDt");
        elText(doc, frToDt, "FrDtTm", frDtTm);
        elText(doc, frToDt, "ToDtTm", toDtTm);

        // Acct
        Element acct = el(doc, stmt, "Acct");
        Element acctId = el(doc, acct, "Id");
        Element acctOthr = el(doc, acctId, "Othr");
        elText(doc, acctOthr, "Id", acctOtherId);

        Element tp = el(doc, acct, "Tp");
        elText(doc, tp, "Cd", "CACC");
        elText(doc, acct, "Ccy", "EUR");

        Element ownr = el(doc, acct, "Ownr");
        elText(doc, ownr, "Nm", ownerName);
        Element ownrPstl = el(doc, ownr, "PstlAdr");
        elText(doc, ownrPstl, "AdrTp", ownerAdrTp);
        if (!isBlank(ownerAdrLine1))
            elText(doc, ownrPstl, "AdrLine", ownerAdrLine1);
        if (!isBlank(ownerAdrLine2))
            elText(doc, ownrPstl, "AdrLine", ownerAdrLine2);
        if (!isBlank(ownerAdrLine3))
            elText(doc, ownrPstl, "AdrLine", ownerAdrLine3);

        Element ownrId = el(doc, ownr, "Id");
        Element ownrOrg = el(doc, ownrId, "OrgId");
        elText(doc, ownrOrg, "BICOrBEI", ownerBicOrBei);

        // Svcr
        Element svcr = el(doc, acct, "Svcr");
        Element fin = el(doc, svcr, "FinInstnId");
        elText(doc, fin, "BIC", svcrBic);
        elText(doc, fin, "Nm", svcrName);
        Element finPstl = el(doc, fin, "PstlAdr");
        elText(doc, finPstl, "AdrTp", svcrAdrTp);
        elText(doc, finPstl, "AdrLine", svcrAdrLine);

        Element finOthr = el(doc, fin, "Othr");
        elText(doc, finOthr, "Id", svcrOtherId);
        elText(doc, finOthr, "Issr", svcrIssr);

        Element brnch = el(doc, fin, "BrnchId");
        elText(doc, brnch, "Id", branchId);
        elText(doc, brnch, "Nm", branchName);
        Element brPstl = el(doc, brnch, "PstlAdr");
        elText(doc, brPstl, "AdrTp", branchAdrTp);
        if (!isBlank(branchAdrLine1))
            elText(doc, brPstl, "AdrLine", branchAdrLine1);
        if (!isBlank(branchAdrLine2))
            elText(doc, brPstl, "AdrLine", branchAdrLine2);

        // Balances: OPBD
        Element balOpen = el(doc, stmt, "Bal");
        Element balOpenTp = el(doc, balOpen, "Tp");
        Element cdOrPrtryOpen = el(doc, balOpenTp, "CdOrPrtry");
        elText(doc, cdOrPrtryOpen, "Cd", "OPBD");
        Element amtOpen = el(doc, balOpen, "Amt");
        amtOpen.setAttribute("Ccy", "EUR");
        amtOpen.setTextContent(money2dp(openingBal));
        elText(doc, balOpen, "CdtDbtInd", openingInd);
        Element dtOpen = el(doc, balOpen, "Dt");
        elText(doc, dtOpen, "Dt", openingDate.format(DATE));

        // Balances: CLBD (computed)
        Element balClose = el(doc, stmt, "Bal");
        Element balCloseTp = el(doc, balClose, "Tp");
        Element cdOrPrtryClose = el(doc, balCloseTp, "CdOrPrtry");
        elText(doc, cdOrPrtryClose, "Cd", "CLBD");
        Element amtCloseEl = el(doc, balClose, "Amt");
        amtCloseEl.setAttribute("Ccy", "EUR");
        amtCloseEl.setTextContent(money2dp(closeAmt));
        elText(doc, balClose, "CdtDbtInd", closeInd);
        Element dtClose = el(doc, balClose, "Dt");
        elText(doc, dtClose, "Dt", toDate.format(DATE));

        // Entries
        for (Entry e : entries) {
            Element ntry = el(doc, stmt, "Ntry");
            Element ntryAmt = el(doc, ntry, "Amt");
            ntryAmt.setAttribute("Ccy", "EUR");
            ntryAmt.setTextContent(money2dp(e.amt));
            elText(doc, ntry, "CdtDbtInd", e.ind);
            elText(doc, ntry, "Sts", "BOOK");

            Element bookgDt = el(doc, ntry, "BookgDt");
            elText(doc, bookgDt, "Dt", bookDate.format(DATE));

            Element valDt = el(doc, ntry, "ValDt");
            elText(doc, valDt, "Dt", valDate.format(DATE));

            // AcctSvcrRef: prefix + 11 digits + EUR
            String acctSvcrRef = acctSvcrRefPrefix + randomDigits(11) + "EUR";
            elText(doc, ntry, "AcctSvcrRef", acctSvcrRef);

            // BkTxCd (static domain/family/subfamily)
            Element bkTxCd = el(doc, ntry, "BkTxCd");
            Element domn = el(doc, bkTxCd, "Domn");
            elText(doc, domn, "Cd", "ACMT");
            Element fmly = el(doc, domn, "Fmly");
            elText(doc, fmly, "Cd", "OPCL");
            elText(doc, fmly, "SubFmlyCd", "ACCC");

            Element prtry = el(doc, bkTxCd, "Prtry");
            elText(doc, prtry, "Cd", ntryPrtryCd);
            elText(doc, prtry, "Issr", ntryPrtryIssr);

            // Details
            Element ntryDtls = el(doc, ntry, "NtryDtls");
            Element txDtls = el(doc, ntryDtls, "TxDtls");

            Element refs = el(doc, txDtls, "Refs");
            elText(doc, refs, "PmtInfId", String.valueOf(randomInt(100000, 999999)));
            elText(doc, refs, "InstrId", yyyymmdd + String.format("%07d", randomInt(0, 9_999_999)));

            Element txBkTxCd = el(doc, txDtls, "BkTxCd");
            Element txPrtry = el(doc, txBkTxCd, "Prtry");
            elText(doc, txPrtry, "Cd", txPrtryCd);
            elText(doc, txPrtry, "Issr", txPrtryIssr);

            Element rltdPties = el(doc, txDtls, "RltdPties");
            Element dbtr = el(doc, rltdPties, "Dbtr");
            elText(doc, dbtr, "Nm", debtorName);

            Element dbtrAcct = el(doc, rltdPties, "DbtrAcct");
            Element dbtrAcctId = el(doc, dbtrAcct, "Id");
            elText(doc, dbtrAcctId, "IBAN", debtorIban);

            Element rltdAgts = el(doc, txDtls, "RltdAgts");
            Element dbtrAgt = el(doc, rltdAgts, "DbtrAgt");
            Element dbtrFin = el(doc, dbtrAgt, "FinInstnId");
            elText(doc, dbtrFin, "BIC", debtorAgentBic);

            Element rmtInf = el(doc, txDtls, "RmtInf");
            elText(doc, rmtInf, "Ustrd", ustrd);

            elText(doc, ntry, "AddtlNtryInf", addtlNtryInf);
        }

        return doc;
    }

    private static Element el(Document doc, Element parent, String localName) {
        Element e = doc.createElementNS(NS, localName);
        parent.appendChild(e);
        return e;
    }

    private static Element elText(Document doc, Element parent, String localName, String text) {
        Element e = el(doc, parent, localName);
        e.setTextContent(text);
        return e;
    }

    private static void writeXml(Document doc, String filename) throws Exception {
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer t = tf.newTransformer();
        t.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        t.setOutputProperty(OutputKeys.INDENT, "yes");
        t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        // Pretty print (works with common JDKs)
        t.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

        try (FileOutputStream fos = new FileOutputStream(filename)) {
            t.transform(new DOMSource(doc), new StreamResult(fos));
        }
    }

    // -------------------- FORMATTING / IDS --------------------

    private static String fmtDtWithMsAndTz(LocalDate d, LocalTime t, String tz) {
        // e.g. 2025-06-10T06:51:20.0+02:00
        return d.format(DATE) + "T" + t.format(TIME_HMS) + ".0" + tz;
    }

    private static String fmtDtNoMs(LocalDate d, LocalTime t, String tz) {
        // e.g. 2025-06-10T00:00:00+02:00
        return d.format(DATE) + "T" + t.format(TIME_HMS) + tz;
    }

    private static String genMsgId(String yyyymmdd, String acctShort, String ccy, String mid, int seq) {
        return yyyymmdd + "T" + acctShort + ccy + mid + String.format("%07d", seq);
    }

    private static String genStmtId(String yyyymmdd, String acctShort, String ccy, int seq) {
        return yyyymmdd + "R1V" + acctShort + ccy + "S" + seq;
    }

    private static String extractAcctShort(String acctOtherId) {
        Matcher m = ACCT_SHORT_PATTERN.matcher(acctOtherId);
        if (m.find())
            return m.group(1);
        return null;
    }

    private static String randomDigits(int n) {
        StringBuilder sb = new StringBuilder(n);
        for (int i = 0; i < n; i++)
            sb.append(RNG.nextInt(10));
        return sb.toString();
    }

    private static int randomInt(int min, int max) {
        if (min > max) {
            int t = min;
            min = max;
            max = t;
        }
        return min + RNG.nextInt((max - min) + 1);
    }

    private static BigDecimal randomAmount(BigDecimal min, BigDecimal max) {
        if (max.compareTo(min) < 0) {
            BigDecimal t = min;
            min = max;
            max = t;
        }
        double lo = min.doubleValue();
        double hi = max.doubleValue();
        double v = lo + (hi - lo) * RNG.nextDouble();
        return new BigDecimal(v).setScale(2, BigDecimal.ROUND_HALF_UP);
    }

    private static String money2dp(BigDecimal x) {
        return x.setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString();
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    // -------------------- PROMPTS --------------------

    private static String promptString(String label, String defaultValue, String example) throws Exception {
        while (true) {
            String suffix = "";
            if (example != null && !example.isEmpty())
                suffix += " (example: " + example + ")";
            if (defaultValue != null)
                suffix += " [default: " + defaultValue + "]";
            System.out.print(label + suffix + ": ");
            String line = IN.readLine();
            if (line == null)
                throw new RuntimeException("Input closed.");
            line = line.trim();
            if (line.isEmpty()) {
                if (defaultValue != null)
                    return defaultValue;
                System.out.println("This field is required.");
                continue;
            }
            return line;
        }
    }

    private static int promptInt(String label, int defaultValue, int min, int max) throws Exception {
        while (true) {
            System.out.print(label + " [default: " + defaultValue + "]: ");
            String line = IN.readLine();
            if (line == null)
                throw new RuntimeException("Input closed.");
            line = line.trim();
            int v;
            if (line.isEmpty())
                v = defaultValue;
            else {
                try {
                    v = Integer.parseInt(line);
                } catch (NumberFormatException nfe) {
                    System.out.println("Please enter a whole number.");
                    continue;
                }
            }
            if (v < min || v > max) {
                System.out.println("Value must be between " + min + " and " + max + ".");
                continue;
            }
            return v;
        }
    }

    private static LocalDate promptDate(String label, LocalDate defaultValue) throws Exception {
        while (true) {
            System.out.print(label + " [default: " + defaultValue.format(DATE) + "]: ");
            String line = IN.readLine();
            if (line == null)
                throw new RuntimeException("Input closed.");
            line = line.trim();
            if (line.isEmpty())
                return defaultValue;
            try {
                return LocalDate.parse(line, DATE);
            } catch (Exception ex) {
                System.out.println("Invalid date format. Use YYYY-MM-DD.");
            }
        }
    }

    private static BigDecimal promptDecimal(String label, BigDecimal defaultValue) throws Exception {
        while (true) {
            System.out.print(label + " [default: " + defaultValue.toPlainString() + "]: ");
            String line = IN.readLine();
            if (line == null)
                throw new RuntimeException("Input closed.");
            line = line.trim();
            if (line.isEmpty())
                return defaultValue.setScale(2, BigDecimal.ROUND_HALF_UP);
            try {
                BigDecimal v = new BigDecimal(line);
                return v.setScale(2, BigDecimal.ROUND_HALF_UP);
            } catch (Exception ex) {
                System.out.println("Please enter a decimal number, e.g. 1234.56");
            }
        }
    }

    private static String promptChoice(String label, String defaultValue, String[] allowed) throws Exception {
        while (true) {
            System.out.print(label + " [default: " + defaultValue + "]: ");
            String line = IN.readLine();
            if (line == null)
                throw new RuntimeException("Input closed.");
            line = line.trim();
            String v = line.isEmpty() ? defaultValue : line;
            v = v.toUpperCase(Locale.ROOT);

            for (String a : allowed) {
                if (a.equalsIgnoreCase(v))
                    return a.toUpperCase(Locale.ROOT);
            }
            System.out.print("Allowed values: ");
            for (int i = 0; i < allowed.length; i++) {
                System.out.print(allowed[i]);
                if (i < allowed.length - 1)
                    System.out.print(", ");
            }
            System.out.println();
        }
    }
}