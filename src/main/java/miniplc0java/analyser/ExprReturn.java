package miniplc0java.analyser;

import miniplc0java.instruction.Instruction;
import miniplc0java.tokenizer.TokenType;

import java.util.ArrayList;
import java.util.List;

public class ExprReturn {
    TokenType type;
    long intValue;
    String stringValue;
    Double doubleValue;
    List<Instruction> instructionList;
    public ExprReturn(){
        this.instructionList=new ArrayList<>();
    }
}
