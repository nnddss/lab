package miniplc0java.analyser;

import miniplc0java.instruction.Instruction;
import miniplc0java.tokenizer.TokenType;
import miniplc0java.util.Pos;

import java.util.ArrayList;
import java.util.List;

public class SymbolEntry implements Comparable<SymbolEntry>{
    int len = 8;
    boolean isConstant;
    boolean isInitialized;
    int stackOffset;
    Type type;
    boolean isStr=false;//表示是否为字符串
    String string;//上个变量为true时存字符串内容


    boolean isFunction=false;
    boolean isGlobal;

    TokenType tokenType;
    Pos pos;
    int parameterCount;
    String name;
    List<String> parameterList= new ArrayList<>();
    double doubleValue;
    long  uintValue;
    String stringValue;
    int number;//对于函数需要知道它在全局变量中的位置
    int localParameterNum;
    int instructionNum;
    int length;
    List<Instruction> instructionList=new ArrayList<>();

    /**
     * @param isConstant
     * @param isDeclared
     * @param stackOffset
     */
    public SymbolEntry(Type type, boolean isConstant, boolean isDeclared, int stackOffset) {
        this.type = type;
        this.isConstant = isConstant;
        this.isInitialized = isDeclared;
        this.stackOffset = stackOffset;
    }
@Override
public int compareTo(SymbolEntry o){
        return this.stackOffset-o.stackOffset;
}
    /**
     * @return the stackOffset
     */
    public int getStackOffset() {
        return stackOffset;
    }

    /**
     * @return the isConstant
     */
    public boolean isConstant() {
        return isConstant;
    }

    /**
     * @return the isInitialized
     */
    public boolean isInitialized() {
        return isInitialized;
    }

    /**
     * @param isConstant the isConstant to set
     */
    public void setConstant(boolean isConstant) {
        this.isConstant = isConstant;
    }

    /**
     * @param isInitialized the isInitialized to set
     */
    public void setInitialized(boolean isInitialized) {
        this.isInitialized = isInitialized;
    }

    /**
     * @param stackOffset the stackOffset to set
     */
    public void setStackOffset(int stackOffset) {
        this.stackOffset = stackOffset;
    }
}
