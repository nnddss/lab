package miniplc0java.analyser;

import miniplc0java.instruction.Instruction;

import java.util.ArrayList;

public class FuncOutput {
    FuncInfo funcInfo;
    ArrayList<Instruction> list = new ArrayList<>();
    public FuncOutput(FuncInfo funcInfo, ArrayList<Instruction> list) {
        this.funcInfo = funcInfo;
        this.list = list;
    }

}
