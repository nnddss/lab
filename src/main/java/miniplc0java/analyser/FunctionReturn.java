package miniplc0java.analyser;

import miniplc0java.instruction.Instruction;
import miniplc0java.tokenizer.TokenType;
import org.checkerframework.checker.units.qual.A;

import java.util.ArrayList;
import java.util.List;

public class FunctionReturn {
    public TokenType tokenType;
    public int localParameterNum;
    public List<Instruction> instructionList;
    public FunctionReturn(){
        this.instructionList=new ArrayList<>();
    }
}

