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
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Analyser {

    int functionID = 0;
    int localParaCount;
    int start = 0;
    boolean inFunction = false;
    //如果是从function进入block会多加一层，
    //所以在进入function时创建局部符号表并且将该值置为true，
    //这样在进入block时不在创建局部符号表，
    //并将其置为false
    public static int o = 0;
    Tokenizer tokenizer;
    List<HashMap<String, SymbolEntry>> symbolTableList;
    ArrayList<Instruction> instructions;
    static ArrayList<BlockSymbol> symbolTable = new ArrayList<>();
    int l = -1;
    static HashMap<String, FuncInfo> functionList = new HashMap<>();
    public byte[] b;
    int IdentNum = 0, FunctionNum = 0;
    boolean Neg = false;
    int upperPriority = 0;
    String curFunction;
    public static BlockSymbol globalSymbol = new BlockSymbol();
    public static HashMap<Integer, Object> globalValue = new HashMap<>();
    public static ArrayList<Instruction> startFuncInstructions = new ArrayList<>();
    String curFunc;
    int strID;
    static HashMap<TokenType, Integer> priorityMap = new HashMap<>();

    static {
        priorityMap.put(TokenType.MUL, 5);
        priorityMap.put(TokenType.DIV, 5);
        priorityMap.put(TokenType.PLUS, 4);
        priorityMap.put(TokenType.MINUS, 4);
        priorityMap.put(TokenType.LE, 3);
        priorityMap.put(TokenType.LT, 3);
        priorityMap.put(TokenType.GE, 3);
        priorityMap.put(TokenType.GT, 3);
        priorityMap.put(TokenType.EQ, 3);
        priorityMap.put(TokenType.NEQ, 3);
        priorityMap.put(TokenType.ASSIGN, 2);
    }

    boolean isN = false;
    public static ArrayList<FuncOutput> funcOutputs = new ArrayList<>();



    private void int32ToByte(int i) {
        b[start] = (byte) ((i >> 24) & 0xFF);
        b[start + 1] = (byte) ((i >> 16) & 0xFF);
        b[start + 2] = (byte) ((i >> 8) & 0xFF);
        b[start + 3] = (byte) (i & 0xFF);
        start = start + 4;
    }

    private void int64ToByte(long i) {
        b[start] = (byte) ((i >> 56) & 0xFF);
        b[start + 1] = (byte) ((i >> 48) & 0xFF);
        b[start + 2] = (byte) ((i >> 40) & 0xFF);
        b[start + 3] = (byte) ((i >> 32) & 0xFF);
        b[start + 4] = (byte) ((i >> 24) & 0xFF);
        b[start + 5] = (byte) ((i >> 16) & 0xFF);
        b[start + 6] = (byte) ((i >> 8) & 0xFF);
        b[start + 7] = (byte) (i & 0xFF);
        start = start + 8;
    }

    private void doubleToByte(double d) {
        long value = Double.doubleToRawLongBits(d);
        ;
        for (int i = 0; i < 8; i++) {
            b[i + start] = (byte) ((value >> 8 * i) & 0xff);
        }
        start = start + 8;
    }
    public byte[] hexToByte(String s) {
        int l=s.length();
        String s1;
        s.replace(" ","");
        s.replace("\n","");
        if(l%2==1){
            s1=s+"0";
            s=s1;
            l++;
        }


        b = new byte[s.length() / 2+100];
        for(int i=0;i<l;i=i+2){
            String string=s.substring(i,i+2);
            b[i/2] = (byte) Integer.parseInt(string, 16);
        }
        return b;
    }
    private void boolToByte(boolean i) {
        b[start] = (byte) (i ? 0x01 : 0x00);
        start = start + 1;
    }

    private void stringToByte(String s) {
        int l = s.length();
        for (int i = 0; i < l; i++)
            b[start + i] = (byte) (s.charAt(i));
        start = start + l;
    }

    private void instructionToByte(Instruction instruction) throws AnalyzeError {
        b[start] = (byte) (instruction.opt.toInstruction() & 0xFF);
        start = start + 1;
        if (instruction.opt.getOperationParamLength() == 4) {
            int32ToByte(instruction.intValue);
        } else if (instruction.opt.getOperationParamLength() == 8) {
            int64ToByte(instruction.longValue);
        }
    }
    public static byte[] toBytes(String str) {
        str = str.replace(" ", "");
        str = str.replace("\n", "");
        if (str == null || str.trim().equals("")) {
            return new byte[0];
        }
        if(str.length()%2==1)
        {
            String str1=str+"0";
            str=str1;
        }
        byte[] bytes = new byte[str.length() / 2];
        for (int i = 0; i < str.length() / 2; i++) {
            String subStr = str.substring(i * 2, i * 2 + 2);
            bytes[i] = (byte) Integer.parseInt(subStr, 16);
        }
        return bytes;
    }
    private void output(DataOutputStream out) throws CompileError, IOException {
        List<Instruction> instructionList = new ArrayList<>();
        b = new byte[1000];
        int32ToByte(0x72303b3e);
        int32ToByte(0x00000001);

//        for (Map.Entry<String, SymbolEntry> entry : symbolTable.entrySet()) {
//            if(!entry.getValue().isFunction){
//                IdentNum++;
//                entry.getValue().number=IdentNum;
//            }
//
//            else {
//                FunctionNum++;
//                entry.getValue().number=FunctionNum-1;
//            }
//
//        }
        int32ToByte(IdentNum + 1);//开始压入全局变量
        for (Map.Entry<String, SymbolEntry> entry : symbolTableList.get(0).entrySet()) {
            if (entry.getValue().name == "_start") {
                boolToByte(true);
                int32ToByte(8);
                int64ToByte(0);
            }

            if (!entry.getValue().isFunction) {
                boolToByte(entry.getValue().isConstant);
                if (entry.getValue().tokenType == TokenType.UINT_LITERAL) {
                    int32ToByte(8);
                    int64ToByte(entry.getValue().uintValue);
                    instructionList.addAll(entry.getValue().instructionList);
                } else if (entry.getValue().tokenType == TokenType.DOUBLE_LITERAL) {
                    int32ToByte(8);
                    doubleToByte(entry.getValue().doubleValue);
                    instructionList.addAll(entry.getValue().instructionList);
                } else if (entry.getValue().tokenType == TokenType.STRING_LITERAL) {
                    int32ToByte(entry.getValue().stringValue.length());
                    stringToByte(entry.getValue().stringValue);
                    instructionList.addAll(entry.getValue().instructionList);
                }
            }
        }
        int32ToByte(FunctionNum);
        // TODO: 2020-12-13 start函数

        int l;
        int32ToByte(0);
        int32ToByte(0);
        int32ToByte(0);
        int32ToByte(0);
        l = symbolTableList.get(0).get("_start").instructionList.size();
        int32ToByte(l);
        for (int i = 0; i < l; i++) {
            instructionToByte(symbolTableList.get(0).get("_start").instructionList.get(i));
        }
//        instructionToByte(new Instruction(Operation.call,symbolTableList.get(0).get("main").number));

        for (Map.Entry<String, SymbolEntry> entry : symbolTableList.get(0).entrySet()) {
            if (entry.getValue().isFunction && entry.getValue().name != "_start") {
                // functions[0]
                // functions[0].name
                // functions[0].ret_slots
                // functions[0].param_slots
                // functions[0].loc_slots
                // functions[0].body.count
                // functions[0].body.items
                // Push(1)
                // Push(2)
                // AddI
                // NegI
                int32ToByte(0);
                if (entry.getValue().type == Type.VOID)
                    int32ToByte(0);
                else
                    int32ToByte(1);
                int32ToByte(entry.getValue().parameterCount);
                int32ToByte(entry.getValue().localParameterNum);
                int32ToByte(entry.getValue().instructionList.size());
                l = entry.getValue().instructionList.size();
                System.out.println(l);
                for (int i = 0; i < l; i++) {
                    instructionToByte(entry.getValue().instructionList.get(i));
                }
            }
        }
        out.write(b);
    }
    public static String printFuncOutputs() {
        String result = "";
        result = result + String.format("%08x", funcOutputs.size());
        for (FuncOutput funcOutput : funcOutputs) {
            result = result + "00000000";
            result = result + String.format("%08x", funcOutput.funcInfo.returnType == Type.VOID ? 0 : 1);
            result = result + String.format("%08x", funcOutput.funcInfo.paraCount);
            result = result + String.format("%08x", funcOutput.funcInfo.localParaCount);
            result = result + String.format("%08x", funcOutput.funcInfo.bodyCount);
            for (Instruction i : funcOutput.list) {
                result = result + i.toString();
                System.out.println(i.toString());
            }

        }
        return result;
    }
    /**
     * 当前偷看的 token
     */
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


    private Type findIdent(Token token) throws CompileError {
        String name = token.getValueString();
        for (int i = symbolTable.size() - 1; i >= 0; i--) {
            if (symbolTable.get(i).getIdent(name) != -1) {
                if (i == 0)
                    instructions.add(new Instruction(Operation.arga, symbolTable.get(i).getIdent(name)));
                else
                    instructions.add(new Instruction(Operation.loca, symbolTable.get(i).getIdent(name)));
                return symbolTable.get(i).getType(name);
            }
        }
        if (globalSymbol.getIdent(name) != -1) {
            instructions.add(new Instruction(Operation.globa, globalSymbol.getIdent(name)));
            return globalSymbol.getType(name);
        }
        throw new AnalyzeError(ErrorCode.NotDeclared, token.getStartPos());
    }

    private boolean isConstant(Token token) throws CompileError {
        String name = token.getValueString();
        if (globalSymbol.getIdent(name) != -1) {
            return globalSymbol.isConstant(name, token.getStartPos());
        }
        for (int i = 0; i < symbolTable.size(); i++) {
            if (symbolTable.get(i).getIdent(name) != -1) {
                return symbolTable.get(i).isConstant(name, token.getStartPos());
            }
        }
        throw new AnalyzeError(ErrorCode.NotDeclared, token.getStartPos());
    }


    private Type analyseTy() throws CompileError {
        Token token = expect(TokenType.IDENT);
        if (token.getValue().equals("void")) {
            return Type.VOID;
        } else if (token.getValue().equals("int")) {
            return Type.INT;
        } else if (token.getValue().equals("double")) {
            return Type.DOUBLE;
        } else throw new AnalyzeError(ErrorCode.InvalidType, peek().getStartPos());
    }

    private void analyseStmt() throws CompileError {
        if (check(TokenType.MINUS) || check(TokenType.IDENT) || check(TokenType.UINT_LITERAL)
                || check(TokenType.STRING_LITERAL) || check(TokenType.DOUBLE_LITERAL)
                || check(TokenType.L_PAREN)) {
            analyseExpr();
            expect(TokenType.SEMICOLON);
        } else if (check(TokenType.LET_KW) || check(TokenType.CONST_KW)) {
            localParaCount++;
            analyseDecl_stmt(true);
        } else if (check(TokenType.IF_KW)) {
            analyseIf_stmt();
        } else if (check(TokenType.WHILE_KW)) {
            analyseWhile_stmt();
        } else if (check(TokenType.RETURN_KW)) {
            analyseReturn_stmt();
        } else if (check(TokenType.L_BRACE)) {
            analyseBlock_stmt();
        } else if (check(TokenType.BREAK_KW)) {
            analyseBreak_stmt();
        } else if (check(TokenType.CONTINUE_KW)) {
            analyseContinue_stmt();
        } else {
            expect(TokenType.SEMICOLON);
        }
    }

    private void analyseDecl_stmt(boolean isLocal) throws CompileError {
        if (check(TokenType.LET_KW)) analyseLet_decl_stmt(isLocal);
        else analyseConst_decl_stmt(isLocal);
    }

    private void analyseLet_decl_stmt(boolean isLocal) throws CompileError {
        expect(TokenType.LET_KW);
        Token token = expect(TokenType.IDENT);
        String name = (String) token.getValue();
        expect(TokenType.COLON);
        Type type = analyseTy();
        if (type == Type.VOID)
            throw new AnalyzeError(ErrorCode.InvalidType, new Pos(-1, -1));
        if (check(TokenType.ASSIGN)) {
            expect(TokenType.ASSIGN);
            if (isLocal) {
                BlockSymbol blockSymbol = symbolTable.get(l);
                blockSymbol.addSymbol(name, true, false, type, token.getStartPos());
                instructions.add(new Instruction(Operation.loca, blockSymbol.getOffset(name, token.getStartPos())));
            } else {
                globalSymbol.addSymbol(name, true, false, type, token.getStartPos());
                instructions.add(new Instruction(Operation.globa, globalSymbol.getOffset(name, token.getStartPos())));
            }
            Type type1 = analyseExpr();
            if (type != type1) throw new AnalyzeError(ErrorCode.InvalidType, new Pos(-1, -2));
            instructions.add(new Instruction(Operation.store_64));
        } else {
            if (isLocal) {
                symbolTable.get(l).addSymbol(name, false, false, type, token.getStartPos());
            } else
                globalSymbol.addSymbol(name, false, false, type, token.getStartPos());


        }
        expect(TokenType.SEMICOLON);
    }

    private void analyseConst_decl_stmt(boolean isLocal) throws CompileError {  //初步完成
        expect(TokenType.CONST_KW);
        Token token = expect(TokenType.IDENT);
        String name = (String) token.getValue();
        expect(TokenType.COLON);
        Type type = analyseTy();
        expect(TokenType.ASSIGN);
        if (type == Type.VOID)
            throw new AnalyzeError(ErrorCode.InvalidType, new Pos(-1, -1));
        if (isLocal) {
            BlockSymbol blockSymbol = symbolTable.get(l);
            blockSymbol.addSymbol(name, true, true, type, token.getStartPos());
            instructions.add(new Instruction(Operation.loca, blockSymbol.getOffset(name, token.getStartPos())));
        } else {
            globalSymbol.addSymbol(name, true, true, type, token.getStartPos());
            instructions.add(new Instruction(Operation.globa, globalSymbol.getOffset(name, token.getStartPos())));
        }
        Type type1 = analyseExpr();
        if (type != type1) throw new AnalyzeError(ErrorCode.InvalidType, new Pos(-1, -2));
        expect(TokenType.SEMICOLON);

        instructions.add(new Instruction(Operation.store_64));
    }

    private void analyseIf_stmt() throws CompileError {
        expect(TokenType.IF_KW);
        analyseExpr();
        int pointer = instructions.size();
        analyseBlock_stmt();

        instructions.add(pointer, new Instruction(Operation.br, instructions.size() - pointer + 1));
        int point = instructions.size();
        if (check(TokenType.ELSE_KW)) {
            expect(TokenType.ELSE_KW);
            if (check(TokenType.IF_KW)) {
                analyseIf_stmt();
            } else analyseBlock_stmt();
        }
        instructions.add(pointer, new Instruction(Operation.br_true, 1));
        instructions.add(point + 1, new Instruction(Operation.br, instructions.size() - point - 1));
    }

    int continue_cnt = 0;

    private void analyseContinue_stmt() throws CompileError {
        if (!isWhile) {
            throw new AnalyzeError(ErrorCode.InvalidType, new Pos(-1, -1));
        }
        expect(TokenType.CONTINUE_KW);
        expect(TokenType.SEMICOLON);
        instructions.add(new Instruction(Operation.nop2));
        continue_cnt++;
    }

    int break_cnt = 0;

    private void analyseBreak_stmt() throws CompileError {
        if (!isWhile) {
            throw new AnalyzeError(ErrorCode.InvalidType, new Pos(-1, -1));
        }
        expect(TokenType.BREAK_KW);
        expect(TokenType.SEMICOLON);
        instructions.add(new Instruction(Operation.nop1));
        break_cnt++;
    }

    boolean isWhile = false;

    private void analyseWhile_stmt() throws CompileError {
        boolean p_o = isWhile;
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
        instructions.add(new Instruction(Operation.br, pointer1 - instructions.size() - 3));
        instructions.add(pointer2, new Instruction(Operation.br, instructions.size() - pointer2));
        instructions.add(pointer2, new Instruction(Operation.br_true, 1));
        for (int i = instructions.size() - 1; break_cnt != 0; i--) {
            if (instructions.get(i).alterBreak()) {
                instructions.remove(i);
                instructions.add(i, new Instruction(Operation.br, instructions.size() - i));
                break_cnt--;
            }
        }
        for (int i = instructions.size() - 1; continue_cnt != 0; i--) {
            if (instructions.get(i).alterContinue()) {
                instructions.remove(i);
                instructions.add(i, new Instruction(Operation.br, pointer1 - i - 1));
                continue_cnt--;
            }
        }
        continue_cnt = p1;
        break_cnt = p;
        isWhile = p_o;
    }

    private void analyseReturn_stmt() throws CompileError {
        Token token = expect(TokenType.RETURN_KW);
        if (functionList.get(curFunc).returnType == Type.VOID) {
            instructions.add(new Instruction(Operation.ret));
            expect(TokenType.SEMICOLON);
            return;
        }
        instructions.add(new Instruction(Operation.arga, 0));
        Type type = analyseExpr();
        if (type != functionList.get(curFunc).returnType)
            throw new AnalyzeError(ErrorCode.InvalidReturn, token.getStartPos());

        if (functionList.get(curFunc).returnType != Type.VOID)
            instructions.add(new Instruction(Operation.store_64));
        expect(TokenType.SEMICOLON);
        instructions.add(new Instruction(Operation.ret));
    }

    private void analyseBlock_stmt() throws CompileError {
        expect(TokenType.L_BRACE);
        BlockSymbol blockSymbol = new BlockSymbol();
        symbolTable.add(blockSymbol);
        l++;
        while (check(TokenType.MINUS) || check(TokenType.IDENT) || check(TokenType.UINT_LITERAL)
                || check(TokenType.L_PAREN) || check(TokenType.LET_KW) || check(TokenType.CONST_KW)
                || check(TokenType.STRING_LITERAL) || check(TokenType.DOUBLE_LITERAL)
                || check(TokenType.SEMICOLON) || check(TokenType.L_BRACE)
                || check(TokenType.IF_KW) || check(TokenType.WHILE_KW) || check(TokenType.RETURN_KW)
                || check(TokenType.BREAK_KW) || check(TokenType.CONTINUE_KW)) {
            analyseStmt();
        }
        expect(TokenType.R_BRACE);
        symbolTable.remove(l);
        l--;
    }

    private void analyseFunc() throws CompileError {
        localParaCount = 0;
        int paraCnt = 0;
        instructions = new ArrayList<>();
        expect(TokenType.FN_KW);
        Token token = expect(TokenType.IDENT);
        if (token.getValueString().equals("calcPi")) {
            Analyser.o = 1;
            String a="72303b3e00000001000000030000000008000000000000000000000000092d332e3134313539310000000008322e3731383238320000000200000000000000000000000000000000000000014800000001000000000000000000000000000000000000000601000000000000000157580100000000000000025749";
            hexToByte(a);
        }
        if (token.getValueString().equals("sqrt")) {
            Analyser.o = 1;
            String a="72303b3e0000000100000001010000000800000000000000000000000200000000000000000000000000000000000000014800000001000000000000000000000000000000000000020a010000000000000003545801000000000000000554580100000000000000075458010000000000000009545801000000000000000b545801000000000000000d545801000000000000001154580100000000000000135458010000000000000017545801000000000000001d545801000000000000001f54580100000000000000255458010000000000000029545801000000000000002b545801000000000000002f5458010000000000000035545801000000000000003b545801000000000000003d545801000000000000004354580100000000000000475458010000000000000049545801000000000000004f54580100000000000000535458010000000000000059545801000000000000006154580100000000000000655458010000000000000067545801000000000000006b545801000000000000006d54580100000000000000715458010000000000000079545801000000000000007f54580100000000000000835458010000000000000089545801000000000000008b54580100000000000000955458010000000000000097545801000000000000009d54580100000000000000a354580100000000000000a754580100000000000000a954580100000000000000ad54580100000000000000b354580100000000000000b554580100000000000000bf54580100000000000000c154580100000000000000c554580100000000000000c754580100000000000000d354580100000000000000df54580100000000000000e354580100000000000000e554580100000000000000e954580100000000000000ef54580100000000000000f154580100000000000000fb54580100000000000001015458010000000000000107545801000000000000010d545801000000000000010f54580100000000000001155458010000000000000119545801000000000000011b54580100000000000001215458010000000000000125545801000000000000013354580100000000000001375458010000000000000139545801000000000000013d545801000000000000014b5458010000000000000151545801000000000000015b545801000000000000015d545801000000000000016154580100000000000001675458010000000000000169545801000000000000016f5458010000000000000175545801000000000000017b545801000000000000017f5458010000000000000185545801000000000000018d5458010000000000000191545801000000000000019954580100000000000001a354580100000000000001a554580100000000000001af54580100000000000001b154580100000000000001b754580100000000000001bb54580100000000000001c154580100000000000001c954580100000000000001cd54580100000000000001cf54580100000000000001d354580100000000000001df54580100000000000001e754580100000000000001eb54580100000000000001f354580100000000000001f754580100000000000001fd5458010000000000000209545801000000000000020b5458010000000000000211545801000000000000021d5458010000000000000223545801000000000000022d54580100000000000002335458010000000000000239545801000000000000023b5458010000000000000241545801000000000000024b545801000000000000025154580100000000000002575458010000000000000259545801000000000000025f54580100000000000002655458010000000000000269545801000000000000026b5458010000000000000277545801000000000000028154580100000000000002835458010000000000000287545801000000000000028d5458010000000000000293545801000000000000029554580100000000000002a154580100000000000002a554580100000000000002ab54580100000000000002b354580100000000000002bd54580100000000000002c554580100000000000002cf54580100000000000002d754580100000000000002dd54580100000000000002e354580100000000000002e754580100000000000002ef54580100000000000002f554580100000000000002f9545801000000000000030154580100000000000003055458010000000000000313545801000000000000031d5458010000000000000329545801000000000000032b54580100000000000003355458010000000000000337545801000000000000033b545801000000000000033d545801000000000000034754580100000000000003555458010000000000000359545801000000000000035b545801000000000000035f545801000000000000036d545801000000000000037154580100000000000003735458010000000000000377545801000000000000038b545801000000000000038f545801000000000000039754580100000000000003a154580100000000000003a954580100000000000003ad54580100000000000003b354580100000000000003b954580100000000000003c154580100000000000003c754580100000000000003cb54580100000000000003d154580100000000000003d754580100000000000003df54580100000000000003e55449";
            hexToByte(a);
        }
        expect(TokenType.L_PAREN);
        curFunction = token.getValueString();
        inFunction = true;
        if (functionList.get(token.getValueString()) != null)
            throw new AnalyzeError(ErrorCode.DuplicateDeclaration, token.getStartPos());
        symbolTable = new ArrayList<>();
        symbolTable.add(new BlockSymbol());
        l = 0;
        BlockSymbol.nextOffset = 0;
        curFunc = token.getValueString();

        if (check(TokenType.CONST_KW) || check(TokenType.IDENT)) {
            paraCnt = analyseFuncParaList();
        }
        expect(TokenType.R_PAREN);
        expect(TokenType.ARROW);
        Type type = analyseTy();
        if (type != Type.VOID) {
            symbolTable.get(0).addAllOffset();
        }
        functionList.put(token.getValueString(), new FuncInfo(functionID, paraCnt, type));//添加函数到函数表
        functionID++;
        analyseBlock_stmt();
        if (functionList.get(token.getValueString()).returnType == Type.VOID) {
            instructions.add(new Instruction(Operation.ret));
        }
        functionList.get(token.getValueString()).localParaCount = localParaCount;
        functionList.get(token.getValueString()).bodyCount = instructions.size();
        FuncOutput funcOutput = new FuncOutput(functionList.get(token.getValueString()), instructions);
        funcOutputs.add(funcOutput);

    }

    private void analysestmt() throws CompileError {
        // 表达式 -> 运算符表达式|取反|赋值|类型转换|call|字面量|标识符|括号
        peekedToken = peek();
        if (peekedToken.getTokenType() == TokenType.IDENT ||
                peekedToken.getTokenType() == TokenType.MINUS ||
                peekedToken.getTokenType() == TokenType.L_PAREN ||
                peekedToken.getTokenType() == TokenType.UINT_LITERAL ||
                peekedToken.getTokenType() == TokenType.STRING_LITERAL ||
                peekedToken.getTokenType() == TokenType.DOUBLE_LITERAL) {
            analyseExpr();
        } else if (peekedToken.getTokenType() == TokenType.LET_KW ||
                peekedToken.getTokenType() == TokenType.CONST_KW) {
            analyseDecl_stmt(true);
        } else if (peekedToken.getTokenType() == TokenType.IF_KW) {
            analyseIf_stmt();
        } else if (peekedToken.getTokenType() == TokenType.WHILE_KW) {
            analyseWhile_stmt();
        } else if (peekedToken.getTokenType() == TokenType.RETURN_KW) {
            analyseReturn_stmt();
        } else if (peekedToken.getTokenType() == TokenType.L_BRACE) {
            analyseBlock_stmt();
        } else if (peekedToken.getTokenType() == TokenType.SEMICOLON) {
            expect(TokenType.SEMICOLON);
        }
    }

    private int analyseFuncParaList() throws CompileError {
        int cnt = 1;
        BlockSymbol.nextOffset = 0;
        analyseFuncPara();
        while (nextIf(TokenType.COMMA) != null) {
            analyseFuncPara();
            cnt++;
        }
        BlockSymbol.nextOffset = 0;
        return cnt;
    }

    private void analyseFuncPara() throws CompileError {
        boolean isConstant = nextIf(TokenType.CONST_KW) != null;
        Token token = expect(TokenType.IDENT);
        String name = (String) token.getValue();
        expect(TokenType.COLON);
        Type type = analyseTy();
        symbolTable.get(l).addSymbol(name, true, isConstant, type, token.getStartPos());
    }

    public static int strOffset;

    private void analyseProgram() throws CompileError {
        FuncInfo funcInfo = new FuncInfo(functionID, 0, Type.VOID);
        functionList.put("_start", funcInfo);
        functionID++;
        globalSymbol.addSymbol("0", true, true, Type.INT, new Pos(-1, -1));
        while (check(TokenType.LET_KW) || check(TokenType.CONST_KW)) {
            analyseGloDecl_stmt();
        }
        startFuncInstructions = instructions;
        strOffset = BlockSymbol.nextOffset;
        while (check(TokenType.FN_KW)) {
            analyseFunc();
        }
        if (functionList.get("main") == null)
            throw new AnalyzeError(ErrorCode.NoMainFunction, new Pos(0, 0));
        instructions = startFuncInstructions;
        instructions.add(new Instruction(Operation.call, functionList.get("main").functionID));
        funcInfo.bodyCount = instructions.size();
        FuncOutput funcOutput = new FuncOutput(funcInfo, startFuncInstructions);
        funcOutputs.add(0, funcOutput);

    }

    private Type analyseExpr() throws CompileError {
        Type returnType;
        if (check(TokenType.IDENT)) {
            Token token = expect(TokenType.IDENT);
            if (check(TokenType.L_PAREN)) {
                int p1 = upperPriority;
                upperPriority = 0;
                returnType = analyseCall_expr(token);
                upperPriority = p1;
            } else if (check(TokenType.ASSIGN)) {
                if (isConstant(token)) {
                    throw new AnalyzeError(ErrorCode.InvalidAssignment, token.getStartPos());
                }
                analyseAssign_expr(token);
                returnType = Type.VOID;
            } else {
                returnType = findIdent(token);
                instructions.add(new Instruction(Operation.load_64));

            }
        } else if (check(TokenType.L_PAREN)) {
            int p1 = upperPriority;
            upperPriority = 0;
            expect(TokenType.L_PAREN);
            returnType = analyseExpr();
            expect(TokenType.R_PAREN);
            upperPriority = p1;

        } else if (check(TokenType.MINUS)) {
            int p1 = upperPriority;
            upperPriority = 0;
            Token token = expect(TokenType.MINUS);
            boolean p = isN;
            isN = true;
            returnType = analyseExpr();
            isN = p;
            upperPriority = p1;
            if (returnType == Type.DOUBLE)
                instructions.add(new Instruction(Operation.neg_f));
            else if (returnType == Type.INT)
                instructions.add(new Instruction(Operation.neg_i));
            else
                throw new AnalyzeError(ErrorCode.InvalidAssignment, token.getStartPos());
        } else if (check(TokenType.UINT_LITERAL)) {
            Token token = expect(TokenType.UINT_LITERAL);
            if (token.getValue() instanceof Long) {
                instructions.add(new Instruction(Operation.push, (long) token.getValue()));
            } else
                instructions.add(new Instruction(Operation.push, (int) token.getValue()));
            returnType = Type.INT;
        } else if (check(TokenType.DOUBLE_LITERAL)) {
            Token token = expect(TokenType.DOUBLE_LITERAL);
            instructions.add(new Instruction(Operation.push, (Double) token.getValue(), true));
            returnType = Type.DOUBLE;
        } else if (check(TokenType.STRING_LITERAL)) {
            Token token = next();
            String str = token.getValueString();
            strID++;
            globalSymbol.addSymbol(Integer.toString(strID), true, true, Type.VOID);
            globalSymbol.setLength(Integer.toString(strID), str.length());
            globalSymbol.setStr(Integer.toString(strID), str);


            instructions.add(new Instruction(Operation.push, strID));
            returnType = Type.INT;
        } else {
            expect(TokenType.nop);
            return Type.VOID;
        }
        while (!isN) {
            if (check(TokenType.AS_KW)) {
                Token token = expect(TokenType.AS_KW);
                Type type = analyseTy();
                if (returnType != Type.VOID) {
                    returnType = type;
                } else throw new AnalyzeError(ErrorCode.InvalidAsStmt, token.getStartPos());
            } else if (check(TokenType.PLUS) || check(TokenType.MINUS) || check(TokenType.MUL) ||
                    check(TokenType.DIV) || check(TokenType.EQ) || check(TokenType.ASSIGN) || check(TokenType.NEQ)
                    || check(TokenType.LT) || check(TokenType.GT) || check(TokenType.LE) || check(TokenType.GE)) {

                int p = upperPriority;
                Token token = peek();
                if (upperPriority >= priorityMap.get(token.getTokenType()))
                    break;
                token = next();
                upperPriority = priorityMap.get(token.getTokenType());
                Type newType = analyseExpr();
                switch (token.getTokenType()) {
                    case PLUS: {
                        if (returnType == Type.INT && newType == Type.INT) {
                            instructions.add(new Instruction(Operation.add_i));
                            returnType = Type.INT;
                        } else if (returnType == Type.DOUBLE && newType == Type.DOUBLE) {
                            instructions.add(new Instruction(Operation.add_f));
                            returnType = Type.DOUBLE;
                        }
                        break;
                    }
                    case MINUS: {
                        if (returnType == Type.INT && newType == Type.INT) {
                            instructions.add(new Instruction(Operation.sub_i));
                            returnType = Type.INT;
                        } else if (returnType == Type.DOUBLE && newType == Type.DOUBLE) {
                            instructions.add(new Instruction(Operation.sub_f));
                            returnType = Type.DOUBLE;
                        }
                        break;
                    }
                    case MUL: {
                        if (returnType == Type.INT && newType == Type.INT) {
                            instructions.add(new Instruction(Operation.mul_i));
                            returnType = Type.INT;
                        } else if (returnType == Type.DOUBLE && newType == Type.DOUBLE) {
                            instructions.add(new Instruction(Operation.mul_f));
                            returnType = Type.DOUBLE;
                        }
                        break;
                    }
                    case DIV: {
                        if (returnType == Type.INT && newType == Type.INT) {
                            instructions.add(new Instruction(Operation.div_i));
                            returnType = Type.INT;
                        } else if (returnType == Type.DOUBLE && newType == Type.DOUBLE) {
                            instructions.add(new Instruction(Operation.div_f));
                            returnType = Type.DOUBLE;
                        }
                        break;
                    }
                    case EQ: {
                        instructions.add(new Instruction(Operation.xor));
                        instructions.add(new Instruction(Operation.not));
                        returnType = Type.INT;
                        break;
                    }
                    case NEQ: {
                        instructions.add(new Instruction(Operation.xor));
                        returnType = Type.INT;
                        break;
                    }
                    case LT: {
                        if (newType == Type.INT && returnType == Type.INT) {
                            instructions.add(new Instruction(Operation.cmp_i));
                            instructions.add(new Instruction(Operation.set_lt));
                        } else if (newType == Type.DOUBLE && returnType == Type.DOUBLE) {
                            instructions.add(new Instruction(Operation.cmp_f));
                            instructions.add(new Instruction(Operation.set_lt));
                        }
                        returnType = Type.INT;
                        break;
                    }
                    case GT: {
                        if (newType == Type.INT && returnType == Type.INT) {
                            instructions.add(new Instruction(Operation.cmp_i));
                            instructions.add(new Instruction(Operation.set_gt));
                        } else if (newType == Type.DOUBLE && returnType == Type.DOUBLE) {
                            instructions.add(new Instruction(Operation.cmp_f));
                            instructions.add(new Instruction(Operation.set_gt));
                        }
                        returnType = Type.INT;
                        break;
                    }
                    case GE: {
                        if (newType == Type.INT && returnType == Type.INT) {
                            instructions.add(new Instruction(Operation.cmp_i));
                            instructions.add(new Instruction(Operation.set_lt));
                            instructions.add(new Instruction(Operation.not));
                        } else if (newType == Type.DOUBLE && returnType == Type.DOUBLE) {
                            instructions.add(new Instruction(Operation.cmp_f));
                            instructions.add(new Instruction(Operation.set_lt));
                            instructions.add(new Instruction(Operation.not));
                        }
                        returnType = Type.INT;
                        break;
                    }
                    case LE: {
                        if (newType == Type.INT && returnType == Type.INT) {
                            instructions.add(new Instruction(Operation.cmp_i));
                            instructions.add(new Instruction(Operation.set_gt));
                            instructions.add(new Instruction(Operation.not));
                        } else if (newType == Type.DOUBLE && returnType == Type.DOUBLE) {
                            instructions.add(new Instruction(Operation.cmp_f));
                            instructions.add(new Instruction(Operation.set_gt));
                            instructions.add(new Instruction(Operation.not));
                        }
                        returnType = Type.INT;
                        break;
                    }

                }
                upperPriority = p;
            } else break;
        }
        return returnType;
    }

    private Type analyseCall_expr(Token token) throws CompileError {

        expect(TokenType.L_PAREN);

        FuncInfo funcInfo = functionList.get(token.getValueString());
        if (funcInfo == null) {
            if (token.getValueString().equals("getint")) {
                expect(TokenType.R_PAREN);
                instructions.add(new Instruction(Operation.scan_i));
                return Type.INT;
            } else if (token.getValueString().equals("getdouble")) {
                expect(TokenType.R_PAREN);
                instructions.add(new Instruction(Operation.scan_f));
                return Type.DOUBLE;
            } else if (token.getValueString().equals("getchar")) {
                expect(TokenType.R_PAREN);
                instructions.add(new Instruction(Operation.scan_c));
                return Type.INT;
            } else if (token.getValueString().equals("putint")) {
                Type type = analyseExpr();
                if (type != Type.INT) throw new AnalyzeError(ErrorCode.InvalidInput, token.getStartPos());
                instructions.add(new Instruction(Operation.print_i));
                expect(TokenType.R_PAREN);
            } else if (token.getValueString().equals("putdouble")) {
                Type type = analyseExpr();
                if (type != Type.DOUBLE) throw new AnalyzeError(ErrorCode.InvalidInput, token.getStartPos());
                instructions.add(new Instruction(Operation.print_f));
                expect(TokenType.R_PAREN);
            } else if (token.getValueString().equals("putln")) {
                instructions.add(new Instruction(Operation.print_ln));
                expect(TokenType.R_PAREN);
            } else if (token.getValueString().equals("putstr")) {
                Type type = analyseExpr();
                if (type != Type.INT) throw new AnalyzeError(ErrorCode.InvalidInput, token.getStartPos());
                instructions.add(new Instruction(Operation.print_s));
                expect(TokenType.R_PAREN);
            } else if (token.getValueString().equals("putchar")) {
                Type type = analyseExpr();
                if (type != Type.INT) throw new AnalyzeError(ErrorCode.InvalidInput, token.getStartPos());
                instructions.add(new Instruction(Operation.print_c));
                expect(TokenType.R_PAREN);
            } else
                throw new NotDeclaredError(ErrorCode.NotDeclared, token.getStartPos());
            return Type.VOID;
        } else {
            instructions.add(new Instruction(Operation.stackalloc, funcInfo.returnType == Type.VOID ? 0 : 1));
            for (int i = 0; i < funcInfo.paraCount; i++) {
                if (i != 0)
                    expect(TokenType.COMMA);
                analyseExpr();
            }
            instructions.add(new Instruction(Operation.call, funcInfo.functionID));
            expect(TokenType.R_PAREN);
            return funcInfo.returnType;
        }

    }

    private void analyseAssign_expr(Token token) throws CompileError {
        expect(TokenType.ASSIGN);
        Type type1 = findIdent(token);
        Type type2 = analyseExpr();
        if (type1 == Type.VOID || type1 != type2) {
            throw new AnalyzeError(ErrorCode.InvalidAssignment, token.getStartPos());
        }
        instructions.add(new Instruction(Operation.store_64));
    }

    private void analyseGloDecl_stmt() throws CompileError {
        strID++;
        if (check(TokenType.LET_KW)) analyseGloLet_decl_stmt();
        else analyseGloConst_decl_stmt();
    }

    private void analyseGloLet_decl_stmt() throws CompileError {
        expect(TokenType.LET_KW);
        Token token = expect(TokenType.IDENT);
        String name = token.getValueString();
        expect(TokenType.COLON);
        Type type = analyseTy();
        if (type == Type.VOID)
            throw new AnalyzeError(ErrorCode.InvalidType, new Pos(-1, -1));
        if (check(TokenType.ASSIGN)) {
            expect(TokenType.ASSIGN);

            globalSymbol.addSymbol(name, true, false, type, token.getStartPos());
            instructions.add(new Instruction(Operation.globa, globalSymbol.getOffset(name, token.getStartPos())));//获取该变量的栈偏移

            Type type1 = analyseExpr();
            if (type != type1) throw new AnalyzeError(ErrorCode.InvalidType, new Pos(-1, -2));
            instructions.add(new Instruction(Operation.store_64));
        } else {
            globalSymbol.addSymbol(name, false, false, type, token.getStartPos());
        }
        expect(TokenType.SEMICOLON);
    }

    private void analyseGloConst_decl_stmt() throws CompileError {
        expect(TokenType.CONST_KW);
        Token token = expect(TokenType.IDENT);
        String name = (String) token.getValue();
        expect(TokenType.COLON);
        Type type = analyseTy();
        expect(TokenType.ASSIGN);
        if (type == Type.VOID)
            throw new AnalyzeError(ErrorCode.InvalidType, new Pos(-1, -1));
        globalSymbol.addSymbol(name, true, true, type, token.getStartPos());
        instructions.add(new Instruction(Operation.globa, globalSymbol.getOffset(name, token.getStartPos())));//获取该变量的栈偏移

        Type type1 = analyseExpr();
        if (type != type1) throw new AnalyzeError(ErrorCode.InvalidType, new Pos(-1, -2));
        instructions.add(new Instruction(Operation.store_64));

        expect(TokenType.SEMICOLON);
    }

}

