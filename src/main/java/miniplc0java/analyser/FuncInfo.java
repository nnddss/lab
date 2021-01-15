package miniplc0java.analyser;

public class FuncInfo {
    public int funID;
    public int paraCnt;
    public int localParaCnt=0;
    public int bodyCnt;
    public Type returnType;

    public FuncInfo(int funID, int paraCnt,Type returnType){
        this.returnType = returnType;
        this.funID = funID;
        this.paraCnt = paraCnt;
    }
}
