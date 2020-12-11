package miniplc0java.analyser;

import miniplc0java.tokenizer.TokenType;
import miniplc0java.util.Pos;

import java.util.ArrayList;
import java.util.List;

public class SymbolEntry {
    boolean isConstant=false;
    boolean isInitialized=false;
    boolean isFunction=false;
    int stackOffset;
    TokenType tokenType;
    Pos pos;
    int parameterCount;
    List<String> parameterList= new ArrayList<>();
    /**
     * @param isConstant
     * @param isDeclared
     * @param stackOffset
     */
    public SymbolEntry(boolean isConstant, boolean isDeclared,boolean isFunction, int stackOffset, TokenType tokenType, Pos pos) {
        this.isConstant = isConstant;
        this.isInitialized = isDeclared;
        this.isFunction =isFunction;
        this.stackOffset = stackOffset;
        this.tokenType = tokenType;
        this.pos = pos;
    }
    public SymbolEntry(boolean isDeclared,boolean isFunction, int stackOffset, TokenType tokenType, Pos pos,int parameterCount,List parameterList) {
        this.isConstant = isConstant;
        this.isInitialized = isDeclared;
        this.isFunction =isFunction;
        this.stackOffset = stackOffset;
        this.tokenType = tokenType;
        this.pos = pos;
        this.parameterCount=parameterCount;
        this.parameterList=parameterList;
    }
    public TokenType getTokenType() {
        return tokenType;
    }

    public void setTokenType(TokenType tokenType) {
        this.tokenType = tokenType;
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
