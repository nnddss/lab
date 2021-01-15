package miniplc0java.analyser;

public class FuncInfo {
    public int functionID;
    public int paraCount;
    public int localParaCount=0;
    public int bodyCount;
    public Type returnType;

    public FuncInfo(int funID, int paraCnt,Type returnType){
        this.returnType = returnType;
        this.functionID = funID;
        this.paraCount = paraCnt;
    }
}
