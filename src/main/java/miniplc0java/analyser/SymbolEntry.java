package miniplc0java.analyser;

public class SymbolEntry implements Comparable<SymbolEntry>{
    int len = 8;
    boolean isConstant;
    boolean isInitialized;
    int stackOffset;
    Type type;
    boolean isStr=false;//表示是否为字符串
    String string;//上个变量为true时存字符串内容

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
