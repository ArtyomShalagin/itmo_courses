package predicate_calculus;

import predicate_calculus.expression.AbstractExpression;
import common.Util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static common.Util.cleanLine;
import static common.Util.logNoNewline;

public class ProofInfo {
    public final List<AbstractExpression> assumptions;
    public final List<AbstractExpression> proof;
    public final AbstractExpression result;

    private ProofInfo(List<AbstractExpression> assumptions,
                      List<AbstractExpression> proof,
                      AbstractExpression result) {
        this.assumptions = Collections.unmodifiableList(assumptions);
        this.proof = Collections.unmodifiableList(proof);
        this.result = result;
    }

    public static ProofInfo parseProofInfo(List<String> data) throws ParsingException {
        String header = data.get(0);
        int separatorIndex = header.indexOf("|-");
        if (separatorIndex == -1) {
            throw new IllegalArgumentException("Unable to find header in the file");
        }
        String assumptionsString = header.substring(0, separatorIndex);
        assumptionsString = Util.removeSpaces(assumptionsString);
        List<String> assumptionStrings = new ArrayList<>();
        int balance = 0;
        int last = 0;
        for (int i = 0; i < assumptionsString.length(); i++) {
            if (assumptionsString.charAt(i) == '(') {
                balance++;
            } else if (assumptionsString.charAt(i) == ')') {
                balance--;
                if (balance == 0 && i != assumptionsString.length() - 1 && assumptionsString.charAt(i + 1) == ',') {
                    assumptionStrings.add(assumptionsString.substring(last, i + 1));
                    last = i + 2;
                    i++;
                }
            } else if (assumptionsString.charAt(i) == ',' && balance == 0) {
                assumptionStrings.add(assumptionsString.substring(last, i));
                last = i + 1;
            }
        }
        if (last != assumptionsString.length()) {
            assumptionStrings.add(assumptionsString.substring(last));
        }
        List<AbstractExpression> assumptions = new ArrayList<>();
        for (String assumptionString : assumptionStrings) {
            assumptions.add(Parser.parse(assumptionString));
        }
        AbstractExpression result = Parser.parse(header.substring(header.indexOf("|-") + 2));

        List<AbstractExpression> proof = new ArrayList<>();
        for (int i = 1; i < data.size(); i++) {
            proof.add(Parser.parse(data.get(i)));
            logNoNewline("\rParsed " + i + "/" + (data.size() - 1));
        }
        cleanLine(("Parsed /" + String.valueOf(data.size()).length() * 2).length());

        return new ProofInfo(assumptions, proof, result);
    }
}
