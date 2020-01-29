package nl.ou.testar.temporal.util;

import nl.ou.testar.temporal.structure.StateEncoding;
import nl.ou.testar.temporal.structure.TemporalModel;
import nl.ou.testar.temporal.structure.TemporalOracle;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

public class ITSctl_CheckerResultsParser {


    private TemporalModel tmodel;
    private String rawInput;
    private List<TemporalOracle> oracleColl;


    private File log;


    public ITSctl_CheckerResultsParser(TemporalModel tmodel, List<TemporalOracle> oracleColl, File log) {
        this.tmodel = tmodel;
        setOracleColl(oracleColl);
        this.log = log;
    }

    public ITSctl_CheckerResultsParser(TemporalModel tmodel, List<TemporalOracle> oracleColl) {
        this.tmodel = tmodel;
        setOracleColl(oracleColl);
    }

    public File getLog() {
        return log;
    }

    public void setLog(File log) {
        this.log = log;
    }


    public void setTmodel(TemporalModel tmodel) {
        this.tmodel = tmodel;
    }


    public void setOracleColl(List<TemporalOracle> oracleColl) {

        this.oracleColl = new ArrayList<>();
        for (TemporalOracle tOracle : oracleColl
        ) {
            try {
                this.oracleColl.add(tOracle.clone());
            } catch (CloneNotSupportedException e) {
                e.printStackTrace();
            }
        }
    }


    public List<TemporalOracle> parse(String rawInput) {
        this.rawInput = rawInput;
        List<StateEncoding> stateEncodings = tmodel.getStateEncodings();

        Scanner scanner = new Scanner(rawInput);
//        scanner.useDelimiter("\\s*Error\\s*");
//        if (scanner.hasNext()) {
//            return null;  // there is a line with ** error ** somewhere in the results file
//        }


        scanner = new Scanner(rawInput);
        scanner.useDelimiter("\\s*original formula:\\s*");
        if (scanner.hasNext()) scanner.next(); // throw away the content before the first formula result

        List<String> formularesults = new ArrayList<>();
        while (scanner.hasNext()) formularesults.add(scanner.next());
        scanner.useDelimiter("\\s*(\\*\\*\\*)+\\s*");
        if (scanner.hasNext()) {
            formularesults.add(scanner.next());// read last formula result
        }

        if ((formularesults.size() != oracleColl.size())) {
            return null;
        }
        int i = 0;
        boolean toggle = false;
        for (String fResult : formularesults
        ) {
            TemporalOracle Oracle = oracleColl.get(i);
            i++;
            // get result status
            String formulaStatus = "ERROR";
            String encodedFormula = "";
            Scanner forumlascanner = new Scanner(fResult);
            forumlascanner.useDelimiter("Formula is TRUE");
            if (forumlascanner.hasNext()) {
                formulaStatus = "PASS";
                encodedFormula = forumlascanner.nextLine(); //firstline contains the formula

            } else {
                forumlascanner.useDelimiter("Formula is FALSE");
                if (forumlascanner.hasNext()) {
                    formulaStatus = "FAIL";
                    encodedFormula = forumlascanner.nextLine();
                } else {
                    //in case there is a change in the future how LTSMIN provide log details
                    System.out.println("Error parsing results from model checker");
                }
            }
            List<String> emptyList = Collections.emptyList();
            Oracle.setExampleRun_Prefix_States(emptyList);
            Oracle.setExampleRun_Prefix_Transitions(emptyList); //test only
            Oracle.set_comments(new ArrayList<>(Collections.singletonList("Encoded Formula: " + encodedFormula)));
            Oracle.setExampleRun_Cycle_States(emptyList);
            Oracle.setExampleRun_Cycle_Transitions(emptyList);
            if (formulaStatus.equals(Verdict.FAIL.toString())) Oracle.setOracle_verdict(Verdict.FAIL);
            if (formulaStatus.equals(Verdict.PASS.toString())) Oracle.setOracle_verdict(Verdict.PASS);
            Oracle.setLog_RunDate(LocalDateTime.now().toString());


        }

        return this.oracleColl;
    }

    public List<TemporalOracle> parse(File rawInput) {
        StringBuilder contentBuilder = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(rawInput))) {
            String sCurrentLine;
            while ((sCurrentLine = br.readLine()) != null) {
                contentBuilder.append(sCurrentLine).append("\n");
            }
        } catch (IOException f) {
            f.printStackTrace();
        }
        return parse(contentBuilder.toString());

    }
}

