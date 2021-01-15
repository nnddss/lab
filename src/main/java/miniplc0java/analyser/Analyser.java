package miniplc0java.analyser;

import miniplc0java.App;
import miniplc0java.error.*;
import miniplc0java.instruction.Instruction;
import miniplc0java.instruction.Operation;
import miniplc0java.tokenizer.Token;
import miniplc0java.tokenizer.TokenType;
import miniplc0java.tokenizer.Tokenizer;
import miniplc0java.util.Pos;
import org.checkerframework.checker.units.qual.A;

import javax.lang.model.type.ErrorType;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public final class Analyser {
    public static int flag = 0;
    Tokenizer tokenizer;
    ArrayList<Instruction> instructions;
    static ArrayList<BlockSymbol> symbolTable = new ArrayList<>();
    int top = -1;
    static HashMap<String, FuncInfo> funList = new HashMap<>();
    int funID = 0;
    int localParaCnt;
    public static BlockSymbol globalSymbol = new BlockSymbol();
    public static HashMap<Integer,Object> globalValue= new HashMap<>();
    public static ArrayList<Instruction> startFuncInstructions = new ArrayList<>();
    String curFunc;
    int strID;

    public static ArrayList<FuncOutput> funcOutputs = new ArrayList<>();
    public static String printFuncOutputs(){
        String result = "";
        result = result+String.format("%08x", funcOutputs.size());
        for (FuncOutput funcOutput:funcOutputs) {
            result = result+"00000000";
            result = result+String.format("%08x", funcOutput.funcInfo.returnType==Type.VOID?0:1);
            result = result+String.format("%08x", funcOutput.funcInfo.paraCnt);
            result = result+String.format("%08x", funcOutput.funcInfo.localParaCnt);
            result = result+String.format("%08x", funcOutput.funcInfo.bodyCnt);
            for (Instruction i:funcOutput.list) {
                result = result+i.toString();
                System.out.println(i.toString());
            }

        }
        return result;
    }

    /** 当前偷看的 token */
    Token peekedToken = null;
    public Analyser(Tokenizer tokenizer) {
        this.tokenizer = tokenizer;
        this.instructions = new ArrayList<>();
    }

    public List<Instruction> analyse() throws CompileError {
        analyseProgram();
        return instructions;
    }

    /**
     * 查看下一个 Token
     * 
     * @return
     * @throws TokenizeError
     */
    private Token peek() throws TokenizeError {
        if (peekedToken == null) {
            peekedToken = tokenizer.nextToken();
        }
        return peekedToken;
    }

    /**
     * 获取下一个 Token
     * 
     * @return
     * @throws TokenizeError
     */
    private Token next() throws TokenizeError {
        if (peekedToken != null) {
            var token = peekedToken;
            peekedToken = null;
            return token;
        } else {
            return tokenizer.nextToken();
        }
    }

    /**
     * 如果下一个 token 的类型是 tt，则返回 true
     * 
     * @param tt
     * @return
     * @throws TokenizeError
     */
    private boolean check(TokenType tt) throws TokenizeError {
        var token = peek();
        return token.getTokenType() == tt;
    }

    /**
     * 如果下一个 token 的类型是 tt，则前进一个 token 并返回这个 token
     * 
     * @param tt 类型
     * @return 如果匹配则返回这个 token，否则返回 null
     * @throws TokenizeError
     */
    private Token nextIf(TokenType tt) throws TokenizeError {
        var token = peek();
        if (token.getTokenType() == tt) {
            return next();
        } else {
            return null;
        }
    }

    /**
     * 如果下一个 token 的类型是 tt，则前进一个 token 并返回，否则抛出异常
     * 
     * @param tt 类型
     * @return 这个 token
     * @throws CompileError 如果类型不匹配
     */
    private Token expect(TokenType tt) throws CompileError {
        var token = peek();
        if (token.getTokenType() == tt) {
            return next();
        } else {
            throw new ExpectedTokenError(tt, token);
        }
    }

    private boolean isFirst_vt_stmt() throws CompileError{
        return check(TokenType.MINUS)||check(TokenType.IDENT)||check(TokenType.UINT_LITERAL)
                ||check(TokenType.STRING_LITERAL)||check(TokenType.DOUBLE_LITERAL)
                ||check(TokenType.L_PAREN)||check(TokenType.LET_KW)||check(TokenType.CONST_KW)
                ||check(TokenType.IF_KW)||check(TokenType.WHILE_KW)||check(TokenType.RETURN_KW)
                ||check(TokenType.SEMICOLON)||check(TokenType.L_BRACE)||check(TokenType.BREAK_KW)||check(TokenType.CONTINUE_KW);
    }

    private Type findIdent(Token token) throws CompileError{
        String name = token.getValueString();
//        if (funList)

        for(int i=symbolTable.size()-1;i>=0;i--){
            if (symbolTable.get(i).getIdent(name)!=-1){
                if (i==0)
                    instructions.add(new Instruction(Operation.arga,symbolTable.get(i).getIdent(name)));
                else
                    instructions.add(new Instruction(Operation.loca,symbolTable.get(i).getIdent(name)));
                return symbolTable.get(i).getType(name);
            }
        }
        if(globalSymbol.getIdent(name)!=-1){
            instructions.add(new Instruction(Operation.globa,globalSymbol.getIdent(name)));
            return globalSymbol.getType(name);
        }
        throw new AnalyzeError(ErrorCode.NotDeclared,token.getStartPos());
    }

    private boolean isConstant(Token token)throws CompileError{
        String name = token.getValueString();
        if (globalSymbol.getIdent(name)!=-1){
            return globalSymbol.isConstant(name,token.getStartPos());
        }
        for(int i=0;i<symbolTable.size();i++) {
            if (symbolTable.get(i).getIdent(name) != -1) {
                return symbolTable.get(i).isConstant(name,token.getStartPos());
            }
        }
        throw new AnalyzeError(ErrorCode.NotDeclared,token.getStartPos());
    }



    private Type analyseTy() throws CompileError{
        Token token = expect(TokenType.IDENT);
        if (token.getValue().equals("void")){
            return Type.VOID;
        }
        else if(token.getValue().equals("int")){
            return Type.INT;
        }
        else if (token.getValue().equals("double")){
            return Type.DOUBLE;
        }
        else throw new AnalyzeError(ErrorCode.InvalidType,peek().getStartPos());
    }

    private void analyseStmt() throws CompileError{
        if (check(TokenType.MINUS)||check(TokenType.IDENT)||check(TokenType.UINT_LITERAL)
                ||check(TokenType.STRING_LITERAL)||check(TokenType.DOUBLE_LITERAL)
                ||check(TokenType.L_PAREN)){
            analyseExpr();
            expect(TokenType.SEMICOLON);
        }
        else if (check(TokenType.LET_KW)||check(TokenType.CONST_KW)){
            localParaCnt++;
            analyseDecl_stmt(true);
        }
        else if (check(TokenType.IF_KW)){
            analyseIf_stmt();
        }
        else if (check(TokenType.WHILE_KW)){
            analyseWhile_stmt();
        }
        else if (check(TokenType.RETURN_KW)){
            analyseReturn_stmt();
        }
        else if (check(TokenType.L_BRACE)){
            analyseBlock_stmt();
        }
        else if (check(TokenType.BREAK_KW)){
            analyseBreak_stmt();
        }
        else if (check(TokenType.CONTINUE_KW)){
            analyseContinue_stmt();
        }
        else{
            expect(TokenType.SEMICOLON);
        }
    }

    private void analyseDecl_stmt(boolean isLocal) throws CompileError{
        if (check(TokenType.LET_KW)) analyseLet_decl_stmt(isLocal);
        else analyseConst_decl_stmt(isLocal);
    }
    private void analyseLet_decl_stmt(boolean isLocal) throws CompileError{
        expect(TokenType.LET_KW);
        Token token = expect(TokenType.IDENT);
        String name = (String)token.getValue();
        expect(TokenType.COLON);
        Type type = analyseTy();
        if (type==Type.VOID)
            throw new AnalyzeError(ErrorCode.InvalidType,new Pos(-1,-1));
        if (check(TokenType.ASSIGN)){
            expect(TokenType.ASSIGN);
            if (isLocal){
                BlockSymbol blockSymbol = symbolTable.get(top);
                blockSymbol.addSymbol(name,true,false,type,token.getStartPos());
                instructions.add(new Instruction(Operation.loca, blockSymbol.getOffset(name,token.getStartPos())));
            }
            else{
                globalSymbol.addSymbol(name,true,false,type,token.getStartPos());
                instructions.add(new Instruction(Operation.globa, globalSymbol.getOffset(name,token.getStartPos())));
            }
            Type type1 = analyseExpr();
            if (type!=type1) throw new AnalyzeError(ErrorCode.InvalidType,new Pos(-1,-2));
            instructions.add(new Instruction(Operation.store_64));
        }
        else {
            if (isLocal){
                symbolTable.get(top).addSymbol(name,false,false,type,token.getStartPos());
            }
            else
                globalSymbol.addSymbol(name,false,false,type,token.getStartPos());


        }
        expect(TokenType.SEMICOLON);
    }

    private void analyseConst_decl_stmt(boolean isLocal) throws CompileError{  //初步完成
        expect(TokenType.CONST_KW);
        Token token = expect(TokenType.IDENT);
        String name = (String)token.getValue();
        expect(TokenType.COLON);
        Type type = analyseTy();
        expect(TokenType.ASSIGN);
        if (type==Type.VOID)
            throw new AnalyzeError(ErrorCode.InvalidType,new Pos(-1,-1));


        if (isLocal){
            BlockSymbol blockSymbol = symbolTable.get(top);
            blockSymbol.addSymbol(name,true,true,type, token.getStartPos());
            instructions.add(new Instruction(Operation.loca, blockSymbol.getOffset(name,token.getStartPos())));
        }
        else{
            globalSymbol.addSymbol(name,true,true,type, token.getStartPos());
            instructions.add(new Instruction(Operation.globa, globalSymbol.getOffset(name,token.getStartPos())));
        }
        Type type1 = analyseExpr();
        if (type!=type1) throw new AnalyzeError(ErrorCode.InvalidType,new Pos(-1,-2));
        expect(TokenType.SEMICOLON);

        instructions.add(new Instruction(Operation.store_64));
    }

    private void analyseIf_stmt() throws CompileError{
        expect(TokenType.IF_KW);
        analyseExpr();
        int pointer = instructions.size();
        analyseBlock_stmt();

        instructions.add(pointer, new Instruction(Operation.br, instructions.size()-pointer+1));
        int pointer2 = instructions.size();
        if (check(TokenType.ELSE_KW)){
            expect(TokenType.ELSE_KW);
            if (check(TokenType.IF_KW)){
                analyseIf_stmt();
            }
            else analyseBlock_stmt();
        }

        instructions.add(pointer, new Instruction(Operation.br_true, 1));
        instructions.add(pointer2+1, new Instruction(Operation.br, instructions.size()-pointer2-1));
    }
    int continue_cnt = 0;
    private void analyseContinue_stmt() throws CompileError{
        if (!isWhile){
            throw new AnalyzeError(ErrorCode.InvalidType,new Pos(-1,-1));
        }
        expect(TokenType.CONTINUE_KW);
        expect(TokenType.SEMICOLON);
        instructions.add(new Instruction(Operation.nop2));
        continue_cnt++;
    }
    int break_cnt=0;
    private void analyseBreak_stmt() throws CompileError{
        if (!isWhile){
            throw new AnalyzeError(ErrorCode.InvalidType,new Pos(-1,-1));
        }
        expect(TokenType.BREAK_KW);
        expect(TokenType.SEMICOLON);
        instructions.add(new Instruction(Operation.nop1));
        break_cnt++;
    }
    boolean isWhile = false;
    private void analyseWhile_stmt() throws CompileError{

        //while_stmt -> 'while' expr block_stmt
        boolean p_flag = isWhile;
        isWhile = true;
        expect(TokenType.WHILE_KW);
        int pointer1 = instructions.size();
        analyseExpr();
        int pointer2 = instructions.size();

        int p = break_cnt;
        break_cnt = 0;
        int p1 = continue_cnt;
        continue_cnt = 0;
        analyseBlock_stmt();

        instructions.add(new Instruction(Operation.br, pointer1-instructions.size()-3));
        instructions.add(pointer2, new Instruction(Operation.br, instructions.size()-pointer2));
        instructions.add(pointer2, new Instruction(Operation.br_true, 1));
        for (int i=instructions.size()-1;break_cnt!=0;i--){
            if (instructions.get(i).alterBreak()){
                instructions.remove(i);
                instructions.add(i,new Instruction(Operation.br,instructions.size()-i));
                break_cnt--;
            }
        }
        for (int i = instructions.size()-1;continue_cnt!=0;i--){
            if (instructions.get(i).alterContinue()){
                instructions.remove(i);
                instructions.add(i,new Instruction(Operation.br,pointer1-i-1));
                continue_cnt--;
            }
        }
        continue_cnt = p1;
        break_cnt = p;
        isWhile = p_flag;
    }
    private void analyseReturn_stmt() throws CompileError{
        Token token = expect(TokenType.RETURN_KW);
        if (funList.get(curFunc).returnType==Type.VOID){
            instructions.add(new Instruction(Operation.ret));
            expect(TokenType.SEMICOLON);
            return;
        }
        instructions.add(new Instruction(Operation.arga,0));
        Type type = analyseExpr();
        if (type!=funList.get(curFunc).returnType)
            throw new AnalyzeError(ErrorCode.InvalidReturn,token.getStartPos());

        if (funList.get(curFunc).returnType!=Type.VOID)
            instructions.add(new Instruction(Operation.store_64));
        expect(TokenType.SEMICOLON);
        instructions.add(new Instruction(Operation.ret));
    }

    private void analyseBlock_stmt() throws CompileError{
        expect(TokenType.L_BRACE);
        BlockSymbol blockSymbol = new BlockSymbol();
        symbolTable.add(blockSymbol);
        top++;
        while (check(TokenType.MINUS)||check(TokenType.IDENT)||check(TokenType.UINT_LITERAL)
                ||check(TokenType.L_PAREN)||check(TokenType.LET_KW)||check(TokenType.CONST_KW)
                ||check(TokenType.STRING_LITERAL)||check(TokenType.DOUBLE_LITERAL)
                ||check(TokenType.SEMICOLON)||check(TokenType.L_BRACE)
                ||check(TokenType.IF_KW)||check(TokenType.WHILE_KW)||check(TokenType.RETURN_KW)
                ||check(TokenType.BREAK_KW)||check(TokenType.CONTINUE_KW) ){
            analyseStmt();
        }
        expect(TokenType.R_BRACE);
        symbolTable.remove(top);
        top--;
    }

    private void analyseFunc() throws CompileError{
        localParaCnt = 0;
        int paraCnt=0;
        instructions = new ArrayList<>();
        expect(TokenType.FN_KW);
        Token token = expect(TokenType.IDENT);
        expect(TokenType.L_PAREN);

        if (funList.get(token.getValueString())!=null)
            throw new AnalyzeError(ErrorCode.DuplicateDeclaration, token.getStartPos());

        symbolTable = new ArrayList<>();
        symbolTable.add(new BlockSymbol());
        top=0;
        BlockSymbol.nextOffset = 0;
        curFunc =token.getValueString();

        if (check(TokenType.CONST_KW)||check(TokenType.IDENT)){
            paraCnt = analyseFuncParaList();
        }
        expect(TokenType.R_PAREN);
        expect(TokenType.ARROW);
        Type type = analyseTy();
        if (type!=Type.VOID){
            symbolTable.get(0).addAllOffset();
        }

        funList.put(token.getValueString(),new FuncInfo(funID,paraCnt,type));//添加函数到函数表
        funID++;

        analyseBlock_stmt();

        if (funList.get(token.getValueString()).returnType==Type.VOID){
            instructions.add(new Instruction(Operation.ret));
        }
        funList.get(token.getValueString()).localParaCnt=localParaCnt;
        funList.get(token.getValueString()).bodyCnt=instructions.size();



        FuncOutput funcOutput = new FuncOutput(funList.get(token.getValueString()),instructions);
        funcOutputs.add(funcOutput);

    }

    private int analyseFuncParaList() throws CompileError{
        int cnt=1;
        BlockSymbol.nextOffset=0;
        analyseFuncPara();
        while (nextIf(TokenType.COMMA)!=null){
            analyseFuncPara();
            cnt++;
        }
        BlockSymbol.nextOffset = 0;
        return cnt;
    }

    private void analyseFuncPara() throws CompileError{
        boolean isConstant = nextIf(TokenType.CONST_KW) != null;
        Token token = expect(TokenType.IDENT);
        String name = (String)token.getValue();
        expect(TokenType.COLON);
        Type type = analyseTy();
        symbolTable.get(top).addSymbol(name,true,isConstant,type,token.getStartPos());
    }
    public static int strOffset;
    private void analyseProgram() throws CompileError {
        FuncInfo funcInfo = new FuncInfo(funID,0,Type.VOID);
        funList.put("_start",funcInfo);
        funID++;
        globalSymbol.addSymbol("0",true, true,Type.INT,new Pos(-1,-1));
        while (check(TokenType.LET_KW) || check(TokenType.CONST_KW)) {
            analyseGloDecl_stmt();
        }
        startFuncInstructions = instructions;
        strOffset = BlockSymbol.nextOffset;
        while (check(TokenType.FN_KW)){
            analyseFunc();
        }
        if (funList.get("main")==null)
            throw new AnalyzeError(ErrorCode.NoMainFunction,new Pos(0,0));
        instructions = startFuncInstructions;
        instructions.add(new Instruction(Operation.call,funList.get("main").funID));
        funcInfo.bodyCnt = instructions.size();
        FuncOutput funcOutput = new FuncOutput(funcInfo,startFuncInstructions);
        funcOutputs.add(0,funcOutput);

    }
    public boolean isOperation() throws CompileError{
        return check(TokenType.PLUS)||check(TokenType.MINUS)||check(TokenType.MUL)||
                check(TokenType.DIV) ||check(TokenType.EQ)||check(TokenType.ASSIGN)||check(TokenType.NEQ)
                ||check(TokenType.LT)||check(TokenType.GT)||check(TokenType.LE)||check(TokenType.GE);
    }




    static HashMap<TokenType,Integer> priorityMap = new HashMap<>();
    static {
        priorityMap.put(TokenType.MUL,5);
        priorityMap.put(TokenType.DIV,5);
        priorityMap.put(TokenType.PLUS,4);
        priorityMap.put(TokenType.MINUS,4);
        priorityMap.put(TokenType.LE,3);
        priorityMap.put(TokenType.LT,3);
        priorityMap.put(TokenType.GE,3);
        priorityMap.put(TokenType.GT,3);
        priorityMap.put(TokenType.EQ,3);
        priorityMap.put(TokenType.NEQ,3);
        priorityMap.put(TokenType.ASSIGN,2);
    }


    boolean isNEG = false;
    int upperPriority = 0;
    private Type analyseExpr() throws CompileError{
        Type returnType;
        if (check(TokenType.IDENT)){
            Token token = expect(TokenType.IDENT);
            if (check(TokenType.L_PAREN)){
                int p1 = upperPriority;
                upperPriority = 0;
                returnType = analyseCall_expr(token);
                upperPriority=p1;
            }
            else if (check(TokenType.ASSIGN)){
                if (isConstant(token)){
                    throw new AnalyzeError(ErrorCode.InvalidAssignment,token.getStartPos());
                }
                analyseAssign_expr(token);
                returnType = Type.VOID;
            }
            else {
                returnType = findIdent(token);
                instructions.add(new Instruction(Operation.load_64));

            }
        }
        else if (check(TokenType.L_PAREN)){
            int p1 = upperPriority;
            upperPriority = 0;
            expect(TokenType.L_PAREN);
            returnType = analyseExpr();
            expect(TokenType.R_PAREN);
            upperPriority = p1;

        }
        else if (check(TokenType.MINUS)){
            int p1 = upperPriority;
            upperPriority = 0;
            Token token = expect(TokenType.MINUS);
            boolean p = isNEG;
            isNEG = true;
            returnType = analyseExpr();
            isNEG = p;
            upperPriority=p1;
            if (returnType == Type.DOUBLE)
                instructions.add(new Instruction(Operation.neg_f));
            else if (returnType == Type.INT)
                instructions.add(new Instruction(Operation.neg_i));
            else
                throw new AnalyzeError(ErrorCode.InvalidAssignment,token.getStartPos());
        }
        else if (check(TokenType.UINT_LITERAL)){
            Token token = expect(TokenType.UINT_LITERAL);
            if (token.getValue() instanceof Long){
                instructions.add(new Instruction(Operation.push,(long)token.getValue()));
            }
            else
                instructions.add(new Instruction(Operation.push,(int)token.getValue()));
            returnType = Type.INT;
        }
        else if (check(TokenType.DOUBLE_LITERAL)){
            Token token = expect(TokenType.DOUBLE_LITERAL);
            instructions.add(new Instruction(Operation.push,(Double)token.getValue(),true));
            returnType = Type.DOUBLE;
        }
        else if (check(TokenType.STRING_LITERAL)){
            Token token = next();
            String str = token.getValueString();
            strID++;
            globalSymbol.addSymbol(Integer.toString(strID),true,true,Type.VOID);
            globalSymbol.setLength(Integer.toString(strID),str.length());
            globalSymbol.setStr(Integer.toString(strID),str);


            instructions.add(new Instruction(Operation.push,strID));
            returnType = Type.INT;
        }
        else {
            expect(TokenType.nop);
            return Type.VOID;
        }
        while (!isNEG){
            if (check(TokenType.AS_KW)){
                Token token = expect(TokenType.AS_KW);
                Type type = analyseTy();
                if (returnType!=Type.VOID){
                    returnType = type;
                }
                else throw new AnalyzeError(ErrorCode.InvalidAsStmt,token.getStartPos());
            }
            else if (isOperation()){

                int p = upperPriority;
                Token token = peek();
                if (upperPriority>=priorityMap.get(token.getTokenType()))
                    break;
                token = next();
                upperPriority = priorityMap.get(token.getTokenType());
                Type newType = analyseExpr();
                switch (token.getTokenType()){
                    case PLUS : {
                        if (returnType ==Type.INT&&newType ==Type.INT){
                            instructions.add(new Instruction(Operation.add_i));
                            returnType=Type.INT;
                        }

                        else if (returnType ==Type.DOUBLE&&newType ==Type.DOUBLE){
                            instructions.add(new Instruction(Operation.add_f));
                            returnType=Type.DOUBLE;
                        }
                        break;
                    }
                    case MINUS : {
                        if (returnType ==Type.INT&&newType ==Type.INT) {
                            instructions.add(new Instruction(Operation.sub_i));
                            returnType=Type.INT;
                        }
                        else if (returnType ==Type.DOUBLE&&newType ==Type.DOUBLE){
                            instructions.add(new Instruction(Operation.sub_f));
                            returnType=Type.DOUBLE;
                        }
                        break;
                    }
                    case MUL : {
                        if (returnType ==Type.INT&&newType ==Type.INT) {
                            instructions.add(new Instruction(Operation.mul_i));
                            returnType=Type.INT;
                        }
                        else if (returnType ==Type.DOUBLE&&newType ==Type.DOUBLE){
                            instructions.add(new Instruction(Operation.mul_f));
                            returnType=Type.DOUBLE;
                        }
                        break;
                    }
                    case DIV : {
                        if (returnType ==Type.INT&&newType ==Type.INT) {
                            instructions.add(new Instruction(Operation.div_i));
                            returnType=Type.INT;
                        }
                        else if (returnType ==Type.DOUBLE&&newType ==Type.DOUBLE){
                            instructions.add(new Instruction(Operation.div_f));
                            returnType=Type.DOUBLE;
                        }
                        break;
                    }
                    case EQ : {
                        instructions.add(new Instruction(Operation.xor));
                        instructions.add(new Instruction(Operation.not));
                        returnType=Type.INT;
                        break;
                    }
                    case NEQ : {
                        instructions.add(new Instruction(Operation.xor));
                        returnType=Type.INT;
                        break;
                    }
                    case LT :{
                        if (newType==Type.INT&&returnType==Type.INT){
                            instructions.add(new Instruction(Operation.cmp_i));
                            instructions.add(new Instruction(Operation.set_lt));
                        }
                        else if (newType==Type.DOUBLE&&returnType==Type.DOUBLE){
                            instructions.add(new Instruction(Operation.cmp_f));
                            instructions.add(new Instruction(Operation.set_lt));
                        }
                        returnType=Type.INT;
                        break;
                    }
                    case GT :{
                        if (newType==Type.INT&&returnType==Type.INT){
                            instructions.add(new Instruction(Operation.cmp_i));
                            instructions.add(new Instruction(Operation.set_gt));
                        }
                        else if (newType==Type.DOUBLE&&returnType==Type.DOUBLE){
                            instructions.add(new Instruction(Operation.cmp_f));
                            instructions.add(new Instruction(Operation.set_gt));
                        }
                        returnType=Type.INT;
                        break;
                    }
                    case GE : {
                        if (newType==Type.INT&&returnType==Type.INT){
                            instructions.add(new Instruction(Operation.cmp_i));
                            instructions.add(new Instruction(Operation.set_lt));
                            instructions.add(new Instruction(Operation.not));
                        }
                        else if (newType==Type.DOUBLE&&returnType==Type.DOUBLE){
                            instructions.add(new Instruction(Operation.cmp_f));
                            instructions.add(new Instruction(Operation.set_lt));
                            instructions.add(new Instruction(Operation.not));
                        }
                        returnType=Type.INT;
                        break;
                    }
                    case LE : {
                        if (newType==Type.INT&&returnType==Type.INT){
                            instructions.add(new Instruction(Operation.cmp_i));
                            instructions.add(new Instruction(Operation.set_gt));
                            instructions.add(new Instruction(Operation.not));
                        }
                        else if (newType==Type.DOUBLE&&returnType==Type.DOUBLE){
                            instructions.add(new Instruction(Operation.cmp_f));
                            instructions.add(new Instruction(Operation.set_gt));
                            instructions.add(new Instruction(Operation.not));
                        }
                        returnType=Type.INT;
                        break;
                    }

                }
                upperPriority = p;
            }
            else break;
        }
        return returnType;
    }
    private Type analyseCall_expr(Token token) throws CompileError{

        expect(TokenType.L_PAREN);

        FuncInfo funcInfo = funList.get(token.getValueString());
        if (funcInfo==null) {
            if (token.getValueString().equals("getint")){
                expect(TokenType.R_PAREN);
                instructions.add(new Instruction(Operation.scan_i));
                return Type.INT;
            }
            else if (token.getValueString().equals("getdouble")){
                expect(TokenType.R_PAREN);
                instructions.add(new Instruction(Operation.scan_f));
                return Type.DOUBLE;
            }
            else if (token.getValueString().equals("getchar")){
                expect(TokenType.R_PAREN);
                instructions.add(new Instruction(Operation.scan_c));
                return Type.INT;
            }

            else if (token.getValueString().equals("putint")) {
                Type type = analyseExpr();
                if (type!=Type.INT) throw new AnalyzeError(ErrorCode.InvalidInput,token.getStartPos());
                instructions.add(new Instruction(Operation.print_i));
                expect(TokenType.R_PAREN);
            }
            else if (token.getValueString().equals("putdouble")) {
                Type type = analyseExpr();
                if (type!=Type.DOUBLE) throw new AnalyzeError(ErrorCode.InvalidInput,token.getStartPos());
                instructions.add(new Instruction(Operation.print_f));
                expect(TokenType.R_PAREN);
            }
            else if (token.getValueString().equals("putln")) {
                instructions.add(new Instruction(Operation.print_ln));
                expect(TokenType.R_PAREN);
            }
            else if (token.getValueString().equals("putstr")) {
                Type type = analyseExpr();
                if (type!=Type.INT) throw new AnalyzeError(ErrorCode.InvalidInput,token.getStartPos());
                instructions.add(new Instruction(Operation.print_s));
                expect(TokenType.R_PAREN);
            }
            else if (token.getValueString().equals("putchar")) {
                Type type = analyseExpr();
                if (type!=Type.INT) throw new AnalyzeError(ErrorCode.InvalidInput,token.getStartPos());
                instructions.add(new Instruction(Operation.print_c));
                expect(TokenType.R_PAREN);
            }

            else
                throw new NotDeclaredError(ErrorCode.NotDeclared,token.getStartPos());
            return Type.VOID;
        }
        else{
            instructions.add(new Instruction(Operation.stackalloc,funcInfo.returnType==Type.VOID?0:1));
            for (int i=0;i<funcInfo.paraCnt;i++){
                if (i!=0)
                    expect(TokenType.COMMA);
                analyseExpr();
            }
            instructions.add(new Instruction(Operation.call,funcInfo.funID));
            expect(TokenType.R_PAREN);
            return funcInfo.returnType;
        }

    }
    private void analyseAssign_expr(Token token) throws CompileError{
        expect(TokenType.ASSIGN);
        Type type1 = findIdent(token);
        Type type2 = analyseExpr();
        if (type1==Type.VOID||type1!=type2){
            throw new AnalyzeError(ErrorCode.InvalidAssignment,token.getStartPos());
        }
        instructions.add(new Instruction(Operation.store_64));
    }

    private void analyseGloDecl_stmt() throws CompileError{
        strID++;
        if (check(TokenType.LET_KW)) analyseGloLet_decl_stmt();
        else analyseGloConst_decl_stmt();
    }
    private void analyseGloLet_decl_stmt() throws CompileError{
        expect(TokenType.LET_KW);
        Token token = expect(TokenType.IDENT);
        String name = token.getValueString();
        expect(TokenType.COLON);
        Type type = analyseTy();
        if (type==Type.VOID)
            throw new AnalyzeError(ErrorCode.InvalidType,new Pos(-1,-1));
        if (check(TokenType.ASSIGN)){
            expect(TokenType.ASSIGN);

            globalSymbol.addSymbol(name,true,false,type,token.getStartPos());
            instructions.add(new Instruction(Operation.globa, globalSymbol.getOffset(name,token.getStartPos())));//获取该变量的栈偏移

            Type type1 = analyseExpr();
            if (type!=type1) throw new AnalyzeError(ErrorCode.InvalidType,new Pos(-1,-2));
            instructions.add(new Instruction(Operation.store_64));
        }
        else {
            globalSymbol.addSymbol(name,false,false,type,token.getStartPos());
        }
        expect(TokenType.SEMICOLON);
    }

    private void analyseGloConst_decl_stmt() throws CompileError{
        expect(TokenType.CONST_KW);
        Token token = expect(TokenType.IDENT);
        String name = (String)token.getValue();
        expect(TokenType.COLON);
        Type type = analyseTy();
        expect(TokenType.ASSIGN);
        if (type==Type.VOID)
            throw new AnalyzeError(ErrorCode.InvalidType,new Pos(-1,-1));
        globalSymbol.addSymbol(name,true,true,type,token.getStartPos());
        instructions.add(new Instruction(Operation.globa, globalSymbol.getOffset(name,token.getStartPos())));//获取该变量的栈偏移

        Type type1 = analyseExpr();
        if (type!=type1) throw new AnalyzeError(ErrorCode.InvalidType,new Pos(-1,-2));
        instructions.add(new Instruction(Operation.store_64));

        expect(TokenType.SEMICOLON);
    }

}



















//    private void analyseProgram() throws CompileError {
//        // 程序 -> 'begin' 主过程 'end'
//        // 示例函数，示例如何调用子程序
//        // 'begin'
//        expect(TokenType.Begin);
//
//        analyseMain();
//
//        // 'end'
//        expect(TokenType.End);
//        expect(TokenType.EOF);
//    }
//
//    private void analyseMain() throws CompileError {
//        // 主过程 -> 常量声明 变量声明 语句序列
//        analyseConstantDeclaration();
//        analyseVariableDeclaration();
//        analyseStatementSequence();
//
//    }
//
//    private void analyseConstantDeclaration() throws CompileError {
//        // 示例函数，示例如何解析常量声明
//        // 常量声明 -> 常量声明语句*
//
//        // 如果下一个 token 是 const 就继续
//        while (nextIf(TokenType.Const) != null) {
//            // 常量声明语句 -> 'const' 变量名 '=' 常表达式 ';'
//
//            // 变量名
//            var nameToken = expect(TokenType.Ident);
//
//            // 加入符号表
//            String name = (String) nameToken.getValue();
//            addSymbol(name, true, true, nameToken.getStartPos());
//
//            // 等于号
//            expect(TokenType.Equal);
//
//            // 常表达式
//            var value = analyseConstantExpression();
//
//            // 分号
//            expect(TokenType.Semicolon);
//
//            // 这里把常量值直接放进栈里，位置和符号表记录的一样。
//            // 更高级的程序还可以把常量的值记录下来，遇到相应的变量直接替换成这个常数值，
//            // 我们这里就先不这么干了。
//            instructions.add(new Instruction(Operation.LIT, value));
//        }
//    }
//
//    private void analyseVariableDeclaration() throws CompileError {
//        // 变量声明 -> 变量声明语句*
//
//        // 如果下一个 token 是 var 就继续
//        while (nextIf(TokenType.Var) != null) {
//            // 变量声明语句 -> 'var' 变量名 ('=' 表达式)? ';'
//
//            // 变量名
//            var nameToken = expect(TokenType.Ident);
//            // 变量初始化了吗
//            boolean initialized = false;
//
//            // 下个 token 是等于号吗？如果是的话分析初始化
//            if(check(TokenType.Equal)){
//                expect(TokenType.Equal);
//                initialized=true;
//                analyseExpression();
//            }
//            // 分析初始化的表达式
//
//            // 分号
//            expect(TokenType.Semicolon);
//
//            // 加入符号表，请填写名字和当前位置（报错用）
//            String name = (String) nameToken.getValue();
//            addSymbol(name, initialized, false, nameToken.getStartPos());
//
//            // 如果没有初始化的话在栈里推入一个初始值
//            if (!initialized) {
//                instructions.add(new Instruction(Operation.LIT, 0));
//            }
//        }
//    }
//
//    private void analyseStatementSequence() throws CompileError {
//        // 语句序列 -> 语句*
//        // 语句 -> 赋值语句 | 输出语句 | 空语句
//
//        while (true) {
//            // 如果下一个 token 是……
//            var peeked = peek();
//
//            if (peeked.getTokenType() == TokenType.Ident) {
//                // 调用相应的分析函数
//                // 如果遇到其他非终结符的 FIRST 集呢？
//                analyseAssignmentStatement();
//            }
//            else if (peeked.getTokenType()== TokenType.Print){
//                analyseOutputStatement();
//            }
//            else if (peeked.getTokenType() == TokenType.Semicolon){
//                next();
//            }
//            else {
//                // 都不是，摸了
//                break;
//            }
//        }
////        throw new Error("Not implemented");
//    }
//
//    private int analyseConstantExpression() throws CompileError {
//        // 常表达式 -> 符号? 无符号整数
//        boolean negative = false;
//        if (nextIf(TokenType.Plus) != null) {
//            negative = false;
//        } else if (nextIf(TokenType.Minus) != null) {
//            negative = true;
//        }
//
//        var token = expect(TokenType.Uint);
//
//        int value = (int) token.getValue();
//        if (negative) {
//            value = -value;
//        }
//
//        return value;
//    }
//
//    private void analyseExpression() throws CompileError {
//        // 表达式 -> 项 (加法运算符 项)*
//        // 项
//        analyseItem();
//
//        while (true) {
//            // 预读可能是运算符的 token
//            var op = peek();
//            if (op.getTokenType() != TokenType.Plus && op.getTokenType() != TokenType.Minus) {
//                break;
//            }
//
//            // 运算符
//            next();
//
//            // 项
//            analyseItem();
//
//            // 生成代码
//            if (op.getTokenType() == TokenType.Plus) {
//                instructions.add(new Instruction(Operation.ADD));
//            } else if (op.getTokenType() == TokenType.Minus) {
//                instructions.add(new Instruction(Operation.SUB));
//            }
//        }
//    }
//
//    private void analyseAssignmentStatement() throws CompileError {
//        // 赋值语句 -> 标识符 '=' 表达式 ';'
//
//        // 分析这个语句
//        Token nameToken = expect(TokenType.Ident);
//        expect(TokenType.Equal);
//        analyseExpression();
//        expect(TokenType.Semicolon);
//        // 标识符是什么？
//        String name = (String) nameToken.getValue();
//        var symbol = symbolTable.get(name);
//        if (symbol == null) {
//            // 没有这个标识符
//            throw new AnalyzeError(ErrorCode.NotDeclared, /* 当前位置 */ nameToken.getStartPos());
//        } else if (symbol.isConstant) {
//            // 标识符是常量
//            throw new AnalyzeError(ErrorCode.AssignToConstant, /* 当前位置 */ nameToken.getStartPos());
//        }
//        // 设置符号已初始化
//        initializeSymbol(name, nameToken.getStartPos());
//
//        // 把结果保存
//        var offset = getOffset(name, nameToken.getStartPos());
//        instructions.add(new Instruction(Operation.STO, offset));
//    }
//
//    private void analyseOutputStatement() throws CompileError {
//        // 输出语句 -> 'print' '(' 表达式 ')' ';'
//
//        expect(TokenType.Print);
//        expect(TokenType.LParen);
//
//        analyseExpression();
//
//        expect(TokenType.RParen);
//        expect(TokenType.Semicolon);
//
//        instructions.add(new Instruction(Operation.WRT));
//    }
//
//    private void analyseItem() throws CompileError {
//        // 项 -> 因子 (乘法运算符 因子)*
//
//        // 因子
//        analyseFactor();
//        while (true) {
//            // 预读可能是运算符的 token
//            Token op = peek();
//            if (op.getTokenType() != TokenType.Mult && op.getTokenType() != TokenType.Div) {
//                break;
//            }
//
//            // 运算符
//            next();
//
//            // 因子
//            analyseFactor();
//
//            // 生成代码
//            if (op.getTokenType() == TokenType.Mult) {
//                instructions.add(new Instruction(Operation.MUL));
//            } else if (op.getTokenType() == TokenType.Div) {
//                instructions.add(new Instruction(Operation.DIV));
//            }
//        }
//    }
//
//    private void analyseFactor() throws CompileError {
//        // 因子 -> 符号? (标识符 | 无符号整数 | '(' 表达式 ')')
//
//        boolean negate;
//        if (nextIf(TokenType.Minus) != null) {
//            negate = true;
//            // 计算结果需要被 0 减
//            instructions.add(new Instruction(Operation.LIT, 0));
//        } else {
//            nextIf(TokenType.Plus);
//            negate = false;
//        }
//
//        if (check(TokenType.Ident)) {
//            // 是标识符
//
//            // 加载标识符的值
//            Token nameToken = expect(TokenType.Ident);
//            String name = (String) nameToken.getValue();
//            var symbol = symbolTable.get(name);
//            if (symbol == null) {
//                // 没有这个标识符
//                throw new AnalyzeError(ErrorCode.NotDeclared, nameToken.getStartPos());
//            } else if (!symbol.isInitialized) {
//                // 标识符没初始化
//                throw new AnalyzeError(ErrorCode.NotInitialized, nameToken.getStartPos());
//            }
//            var offset = getOffset(name, nameToken.getStartPos());
//            instructions.add(new Instruction(Operation.LOD, offset));
//        } else if (check(TokenType.Uint)) {
//            // 是整数
//            // 加载整数值
//            int value = (int)expect(TokenType.Uint).getValue();
//            instructions.add(new Instruction(Operation.LIT, value));
//        } else if (check(TokenType.LParen)) {
//            // 是表达式
//            // 调用相应的处理函数
//            expect(TokenType.LParen);
//            analyseExpression();
//            expect(TokenType.RParen);
//
//        } else {
//            // 都不是，摸了
//            throw new ExpectedTokenError(List.of(TokenType.Ident, TokenType.Uint, TokenType.LParen), next());
//        }
//
//        if (negate) {
//            instructions.add(new Instruction(Operation.SUB));
//        }
//
//    }
//}
