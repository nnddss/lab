package miniplc0java.analyser;

import miniplc0java.instruction.Instruction;
import miniplc0java.tokenizer.TokenType;

import java.util.ArrayList;
import java.util.List;

public class StmtReturn {
    TokenType tokenType;
    List<Instruction> instructionList;
    public StmtReturn(){
        this.instructionList=new ArrayList<>();
    }
}
