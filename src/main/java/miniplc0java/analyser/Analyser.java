package miniplc0java.analyser;

import miniplc0java.error.AnalyzeError;
import miniplc0java.error.CompileError;
import miniplc0java.error.ErrorCode;
import miniplc0java.error.ExpectedTokenError;
import miniplc0java.error.TokenizeError;
import miniplc0java.instruction.Instruction;
import miniplc0java.instruction.Operation;
import miniplc0java.tokenizer.Token;
import miniplc0java.tokenizer.TokenType;
import miniplc0java.tokenizer.Tokenizer;
import miniplc0java.util.Pos;

import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.util.*;

import static javax.swing.text.StyleConstants.Size;

public final class Analyser {
    Tokenizer tokenizer;
    ArrayList<Instruction> instructions;
    /**
     * 当前偷看的 token
     */
    Token peekedToken = null;
    boolean inFunction = false;
    //如果是从function进入block会多加一层，
    //所以在进入function时创建局部符号表并且将该值置为true，
    //这样在进入block时不在创建局部符号表，
    //并将其置为false
    /**
     * 符号表
     */
    HashMap<String, SymbolEntry> symbolTable;
    /**
     * 符号表集
     */
    List<HashMap<String, SymbolEntry>> symbolTableList;
    byte b[];
    int start = 0;
    int listLength;

    boolean Neg = false;
    int upperPriority = 0;
    /**
     * 下一个变量的栈偏移
     */
    int nextOffset;
    Integer strNum=0;
    public Analyser(Tokenizer tokenizer) {
        this.tokenizer = tokenizer;
        this.instructions = new ArrayList<>();
    }

    public List<Instruction> analyse() throws CompileError, IOException {
        analyseProgram();
        return instructions;
    }
//    public List<Instruction> analyse() throws CompileError {
//        analyseProgram();
//        return instructions;
//    }

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
     * 如果该标识符未定义，则定义，否则抛出异常
     */
    private void defineIdent(Token token, boolean isConstant, TokenType tokenType) throws CompileError {
        //boolean isConstant, boolean isDeclared, int stackOffset,
        // TokenType tokenType, Pos pos

        HashMap<String, SymbolEntry> symbolTable = symbolTableList.get(listLength);
        if(tokenType==TokenType.String){
            strNum++;
            SymbolEntry symbolEntry = new SymbolEntry(isConstant, true, false,
                    nextOffset, strNum.toString(), tokenType, token.getStartPos());
            //nextOffset= (int) (nextOffset+ token.getValue());
            symbolEntry.length=token.getValueString().length();
            symbolEntry.stringValue=token.getValueString();
            symbolTable.put(token.getValueString(), symbolEntry);
        }
        else if (symbolTable.containsKey(token.getValueString()) == false) {
            SymbolEntry symbolEntry = new SymbolEntry(isConstant, true, false,
                    nextOffset, token.getValueString(), tokenType, token.getStartPos());
            //nextOffset= (int) (nextOffset+ token.getValue());
            symbolTable.put(token.getValueString(), symbolEntry);
        } else {
            throw new AnalyzeError(ErrorCode.DuplicateDeclaration, token.getStartPos());
        }
    }

    private void defineFunction(Token token, TokenType tokenType, int parameterCount, List parameterList) throws CompileError {
        //boolean isConstant, boolean isDeclared, int stackOffset,
        // TokenType tokenType, Pos pos
        HashMap<String, SymbolEntry> symbolTable = symbolTableList.get(0);
        if (symbolTable.containsKey(token.getValueString()) == false) {
            SymbolEntry symbolEntry = new SymbolEntry(true, true,
                    nextOffset, token.getValueString(), tokenType, token.getStartPos(), parameterCount, parameterList);
            //nextOffset= (int) (nextOffset+ instrumentation.getObjectSize(token.getValue()));
            symbolTable.put(token.getValueString(), symbolEntry);
        } else {
            throw new AnalyzeError(ErrorCode.DuplicateDeclaration, token.getStartPos());
        }
    }
    private boolean firstOfStmt() throws TokenizeError {
        peek();
        TokenType tokenType;
        if(peekedToken==null)
           return false;
        else {
            tokenType=peekedToken.getTokenType();
        }
        if(tokenType==TokenType.Minus||tokenType==TokenType.CONTINUE_KW||tokenType==TokenType.BREAK_KW||tokenType==TokenType.L_BRACE||tokenType==TokenType.Semicolon||
                tokenType==TokenType.IF_KW||tokenType==TokenType.WHILE_KW||tokenType==TokenType.RETURN_KW||tokenType==TokenType.CONST_KW||tokenType==TokenType.LET_KW||
                tokenType==TokenType.LParen||tokenType==TokenType.Double||tokenType==TokenType.String||tokenType==TokenType.Uint||tokenType==TokenType.Ident)
            return true;
        else
            return false;

    }

    private TokenType searchFunction(Token token) throws CompileError {
        //boolean isConstant, boolean isDeclared, int stackOffset,
        // TokenType tokenType, Pos pos
        HashMap<String, SymbolEntry> symbolTable = symbolTableList.get(0);
        if (token.getValueString().equals("getint")){
            return TokenType.Uint;
        }
        else if (token.getValueString().equals("getdouble")){
            return TokenType.Double;
        }
        else if (token.getValueString().equals("getchar")){
            return TokenType.Uint;
        }

        else if (token.getValueString().equals("putint")) {
            return TokenType.VOID;
        }
        else if (token.getValueString().equals("putdouble")) {
            return TokenType.VOID;
        }
        else if (token.getValueString().equals("putstr")) {
            return TokenType.VOID;
        }
        else if (token.getValueString().equals("putchar")) {
            return TokenType.VOID;
        }
        else if (token.getValueString().equals("putln")) {
            return TokenType.VOID;
        }
        else if (symbolTable.containsKey(token.getValueString()) == false) {
            throw new AnalyzeError(ErrorCode.NotDeclared, token.getStartPos());
        }
            return symbolTable.get(token.getValueString()).getTokenType();
    }

    private TokenType searchGlobalNotConst(Token token) throws CompileError {
        //boolean isConstant, boolean isDeclared, int stackOffset,
        // TokenType tokenType, Pos pos
        for (int i = listLength; i >= 0; i--) {
            HashMap<String, SymbolEntry> symbolTable = symbolTableList.get(i);
            if (symbolTable.containsKey(token.getValueString()) && !symbolTable.get(token.getValueString()).isConstant) {
                return symbolTable.get(token.getValueString()).getTokenType();
            } else if (symbolTable.containsKey(token.getValueString()) && symbolTable.get(token.getValueString()).isConstant) {
                throw new AnalyzeError(ErrorCode.AssignToConstant, token.getStartPos());
            }
        }
        throw new AnalyzeError(ErrorCode.NotDeclared, token.getStartPos());
    }

    private TokenType searchGlobal(Token token) throws CompileError {
        //boolean isConstant, boolean isDeclared, int stackOffset,
        // TokenType tokenType, Pos pos
        for (int i = listLength; i >= 0; i--) {
            HashMap<String, SymbolEntry> symbolTable = symbolTableList.get(i);
            if (symbolTable.containsKey(token.getValueString())) {
                return symbolTable.get(token.getValueString()).getTokenType();
            }
        }
        return null ;
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

    /**
     * 获取下一个变量的栈偏移
     *
     * @return
     */
    private int getNextVariableOffset() {
        return this.nextOffset++;
    }

    /**
     * 添加一个符号
     *
     * @param name          名字
     * @param isInitialized 是否已赋值
     * @param isConstant    是否是常量
     * @param curPos        当前 token 的位置（报错用）
     * @throws AnalyzeError 如果重复定义了则抛异常
     */
//    private void addSymbol(String name, boolean isInitialized, boolean isConstant, Pos curPos,TokenType tokenType) throws AnalyzeError {
//        if (this.symbolTable.get(name) != null) {
//            throw new AnalyzeError(ErrorCode.DuplicateDeclaration, curPos);
//        } else {
//            this.symbolTable.put(name, new SymbolEntry(isConstant, isInitialized, getNextVariableOffset(),tokenType,curPos));
//        }
//    }

    /**
     * 设置符号为已赋值
     *
     * @param name   符号名称
     * @param curPos 当前位置（报错用）
     * @throws AnalyzeError 如果未定义则抛异常
     */
    private void initializeSymbol(String name, Pos curPos) throws AnalyzeError {
        var entry = this.symbolTable.get(name);
        if (entry == null) {
            throw new AnalyzeError(ErrorCode.NotDeclared, curPos);
        } else {
            entry.setInitialized(true);
        }
    }

    /**
     * 获取变量在栈上的偏移
     *
     * @param name   符号名
     * @param curPos 当前位置（报错用）
     * @return 栈偏移
     * @throws AnalyzeError
     */
    private int getOffset(String name, Pos curPos) throws AnalyzeError {
        var entry = this.symbolTable.get(name);
        if (entry == null) {
            throw new AnalyzeError(ErrorCode.NotDeclared, curPos);
        } else {
            return entry.getStackOffset();
        }
    }

    /**
     * 获取变量是否是常量
     *
     * @param name   符号名
     * @param curPos 当前位置（报错用）
     * @return 是否为常量
     * @throws AnalyzeError
     */
    private boolean isConstant(String name, Pos curPos) throws AnalyzeError {
        var entry = this.symbolTable.get(name);
        if (entry == null) {
            throw new AnalyzeError(ErrorCode.NotDeclared, curPos);
        } else {
            return entry.isConstant();
        }
    }

    //
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
//        //throw new Error("Not implemented");
//        analyseConstantDeclaration();
//        analyseVariableDeclaration();
//        analyseStatementSequence();
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
//                // 分析初始化的表达式
//                analyseExpression();
//                initialized = true;
//            }
//
//            // 分号
//            expect(TokenType.Semicolon);
//
//            // 加入符号表，请填写名字和当前位置（报错用）
//            String name = /* 名字 */ (String) nameToken.getValue();;
//            addSymbol(name, initialized, false, /* 当前位置 */  nameToken.getStartPos());
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
//            if (peeked.getTokenType() == TokenType.Ident) {
//                // 调用相应的分析函数
//                analyseAssignmentStatement();
//                // 如果遇到其他非终结符的 FIRST 集呢？
//
//            }
//            else if(peeked.getTokenType() == TokenType.Print){
//                analyseOutputStatement();
//            }
//            else if(peeked.getTokenType() == TokenType.Semicolon){
//                expect(TokenType.Semicolon);
//            }
//            else{
//                // 都不是，摸了
//                break;
//            }
//        }
//        //throw new Error("Not implemented");
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
//        var nameToken = expect(TokenType.Ident);
//        expect(TokenType.Equal);
//        analyseExpression();
//        expect(TokenType.Semicolon);
//        // 标识符是什么？
//
//        String name = nameToken.getValueString();
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
//            var op = peek();
//            if(op.getTokenType()!=TokenType.Mult&&op.getTokenType()!=TokenType.Div){
//                break;
//            }
//            // 运算符
//            next();
//            // 因子
//            analyseFactor();
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
//            var nameToken = expect(TokenType.Ident);
//            // 加载标识符的值
//            String name = /* 快填 */ nameToken.getValueString();
//            var symbol = symbolTable.get(name);
//            if (symbol == null) {
//                // 没有这个标识符
//                throw new AnalyzeError(ErrorCode.NotDeclared, /* 当前位置 */ nameToken.getStartPos());
//            } else if (!symbol.isInitialized) {
//                // 标识符没初始化
//                throw new AnalyzeError(ErrorCode.NotInitialized, /* 当前位置 */ nameToken.getStartPos());
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
//            next();
//            analyseExpression();
//            expect(TokenType.RParen);
//        } else {
//            // 都不是，摸了
//            throw new ExpectedTokenError(List.of(TokenType.Ident, TokenType.Uint, TokenType.LParen), next());
//        }
//
//        if (negate) {
//            instructions.add(new Instruction(Operation.SUB));
//        }
//        //throw new Error("Not implemented");
//    }
    private TokenType analyseExpr() throws CompileError {
        // 表达式 -> 运算符表达式|取反|赋值|类型转换|call|字面量|标识符|括号
        TokenType tokenType;
        peekedToken = peek();
        if (peekedToken.getTokenType() == TokenType.Minus) {
            expect(TokenType.Minus);
            boolean k1=Neg;
            int k=upperPriority;
            Neg=true;
            tokenType = analyseExpr();
            Neg=k1;
            upperPriority=k;
            if(tokenType==TokenType.Uint)
                instructions.add(new Instruction(Operation.neg_i));
            else if(tokenType==TokenType.Double)
                instructions.add(new Instruction(Operation.neg_f));
            else
                throw new AnalyzeError(ErrorCode.NeedUintOrDouble, /* 当前位置 */ peekedToken.getStartPos());
        }
        else if (peekedToken.getTokenType() == TokenType.LParen) {
            int k=upperPriority;
            upperPriority=0;
            expect(TokenType.LParen);
            tokenType = analyseExpr();
            upperPriority=k;
            expect(TokenType.RParen);
        }
        else if (peekedToken.getTokenType() == TokenType.Ident) {
            tokenType = analyseAssign_ExprOrIdent_ExprOrCall_Expr();
        }
        else if (peekedToken.getTokenType() == TokenType.Uint ||
                peekedToken.getTokenType() == TokenType.Double ||
                peekedToken.getTokenType() == TokenType.String
        ) {
            tokenType = analyseLiteral_Expr();
        } else {
            throw new AnalyzeError(ErrorCode.InvalidExpr, /* 当前位置 */ peekedToken.getStartPos());
        }
        while (!Neg) {
            if (peek().getTokenType()==TokenType.AS_KW){
                Token token = expect(TokenType.AS_KW);
                token = expect(TokenType.Ident);
                if (tokenType!=TokenType.VOID){
                    tokenType = getTokenTypeOfUintOrDouble(token);
                }
                else throw new AnalyzeError(ErrorCode.NeedUintOrDouble,token.getStartPos());
            }
            else if(check(TokenType.Plus) ||
                    check(TokenType.Minus)||
                    check(TokenType.Div)||
                    check(TokenType.Mult)||
                    check(TokenType.Equal)||
                    check(TokenType.NEQ)||
                    check(TokenType.LE)|
                    check(TokenType.LT)||
                    check(TokenType.GE)||
                    check(TokenType.GT)){
                Token token = peek();
                int tmp = upperPriority;
                if (upperPriority>=token.getTokenType().getPriority())
                    break;
                token = next();
                upperPriority = token.getTokenType().getPriority();
                TokenType tokenType1 = analyseExpr();
                switch (token.getTokenType()){
                    case Plus:{
                        if (tokenType1 ==TokenType.Uint&&tokenType ==TokenType.Uint)
                            instructions.add(new Instruction(Operation.add_i));

                        else if (tokenType1 ==TokenType.Double&&tokenType ==TokenType.Double)
                            instructions.add(new Instruction(Operation.add_f));
                        break;
                    }
                    case Minus:{
                        if (tokenType1 ==TokenType.Uint&&tokenType ==TokenType.Uint)
                            instructions.add(new Instruction(Operation.sub_i));

                        else if (tokenType1 ==TokenType.Double&&tokenType ==TokenType.Double)
                            instructions.add(new Instruction(Operation.sub_f));
                        break;
                    }
                    case Mult:{
                        if (tokenType1 ==TokenType.Uint&&tokenType ==TokenType.Uint)
                            instructions.add(new Instruction(Operation.mul_i));

                        else if (tokenType1 ==TokenType.Double&&tokenType ==TokenType.Double)
                            instructions.add(new Instruction(Operation.mul_f));
                        break;
                    }
                    case Div:{
                        if (tokenType1 ==TokenType.Uint&&tokenType ==TokenType.Uint)
                            instructions.add(new Instruction(Operation.div_i));

                        else if (tokenType1 ==TokenType.Double&&tokenType ==TokenType.Double)
                            instructions.add(new Instruction(Operation.div_f));
                        break;
                    }
                    case Equal : {
                        instructions.add(new Instruction(Operation.xor));
                        instructions.add(new Instruction(Operation.not));
                        //tokenType=TokenType.Uint;
                        break;
                    }
                    case NEQ : {
                        instructions.add(new Instruction(Operation.xor));
                        //tokenType=TokenType.Uint;
                        break;
                    }
                    case LT :{
                        if (tokenType1 ==TokenType.Uint&&tokenType ==TokenType.Uint){
                            instructions.add(new Instruction(Operation.cmp_i));
                            instructions.add(new Instruction(Operation.set_lt));
                        }
                        else if (tokenType1 ==TokenType.Double&&tokenType ==TokenType.Double){
                            instructions.add(new Instruction(Operation.cmp_f));
                            instructions.add(new Instruction(Operation.set_lt));
                        }
                        break;
                    }
                    case GT :{
                        if (tokenType1 ==TokenType.Uint&&tokenType ==TokenType.Uint){
                            instructions.add(new Instruction(Operation.cmp_i));
                            instructions.add(new Instruction(Operation.set_gt));
                        }
                        else if (tokenType1 ==TokenType.Double&&tokenType ==TokenType.Double){
                            instructions.add(new Instruction(Operation.cmp_f));
                            instructions.add(new Instruction(Operation.set_gt));
                        }
                        break;
                    }
                    case GE : {
                        if (tokenType1 ==TokenType.Uint&&tokenType ==TokenType.Uint){
                            instructions.add(new Instruction(Operation.cmp_i));
                            instructions.add(new Instruction(Operation.set_lt));
                            instructions.add(new Instruction(Operation.not));
                        }
                        else if (tokenType1 ==TokenType.Double&&tokenType ==TokenType.Double){
                            instructions.add(new Instruction(Operation.cmp_f));
                            instructions.add(new Instruction(Operation.set_lt));
                            instructions.add(new Instruction(Operation.not));
                        }
                        break;
                    }
                    case LE : {
                        if (tokenType1 ==TokenType.Uint&&tokenType ==TokenType.Uint){
                            instructions.add(new Instruction(Operation.cmp_i));
                            instructions.add(new Instruction(Operation.set_gt));
                            instructions.add(new Instruction(Operation.not));
                        }
                        else if (tokenType1 ==TokenType.Double&&tokenType ==TokenType.Double){
                            instructions.add(new Instruction(Operation.cmp_f));
                            instructions.add(new Instruction(Operation.set_gt));
                            instructions.add(new Instruction(Operation.not));
                        }
                        break;
                    }

                }
                upperPriority = tmp;
            }
            else break;
        }
        return tokenType;
    }

    private TokenType analyseNegate_Expr() throws CompileError {
        TokenType tokenType;
        expect(TokenType.Minus);
        tokenType = analyseExpr();
        if (tokenType != TokenType.Uint || tokenType != TokenType.Double)
            throw new AnalyzeError(ErrorCode.NeedUintOrDouble, /* 当前位置 */ peekedToken.getStartPos());
        return tokenType;
    }

    private TokenType analyseAssign_ExprOrIdent_ExprOrCall_Expr() throws CompileError {
        Token Ident = expect(TokenType.Ident);
        if (nextIf(TokenType.ASSIGN) != null) {//赋值表达式
            searchGlobalNotConst(Ident);
            if(Ident.getTokenType()!=analyseExpr()||Ident.getTokenType()==TokenType.VOID){
                throw new AnalyzeError(ErrorCode.TypeDifferent, /* 当前位置 */ peekedToken.getStartPos());
            }
            instructions.add(new Instruction(Operation.store64));
        } else if (nextIf(TokenType.LParen) != null) {//函数调用
            TokenType tokenType = searchFunction(Ident);

            int k=upperPriority;
            upperPriority=0;
            analyseCall_Expr(Ident);
            upperPriority=k;
            return tokenType;
        }
        TokenType tokenType=searchGlobal(Ident);
        if(tokenType==null){

        }
        instructions.add(new Instruction(Operation.load64));
        return tokenType;
    }

    private void analyseCall_Expr(Token token) throws CompileError {
        if (token.getValueString().equals("getint")){
            expect(TokenType.RParen);
            instructions.add(new Instruction(Operation.scan_i));
            return;
        }
        else if (token.getValueString().equals("getdouble")){
            expect(TokenType.RParen);
            instructions.add(new Instruction(Operation.scan_f));
            return;
        }
        else if (token.getValueString().equals("getchar")){
            expect(TokenType.RParen);
            instructions.add(new Instruction(Operation.scan_c));
            return;
        }

        else if (token.getValueString().equals("putint")) {

            if (analyseExpr()!=TokenType.Uint)
                throw new AnalyzeError(ErrorCode.TypeDifferent,token.getStartPos());
            instructions.add(new Instruction(Operation.print_i));
            expect(TokenType.RParen);
            return;
        }
        else if (token.getValueString().equals("putdouble")) {
            if (analyseExpr()!=TokenType.Double)
                throw new AnalyzeError(ErrorCode.TypeDifferent,token.getStartPos());
            instructions.add(new Instruction(Operation.print_f));
            expect(TokenType.RParen);
            return;
        }
        else if (token.getValueString().equals("putstr")) {
            if (analyseExpr()!=TokenType.Uint)
                throw new AnalyzeError(ErrorCode.TypeDifferent,token.getStartPos());
            instructions.add(new Instruction(Operation.print_s));
            expect(TokenType.RParen);
            return;
        }
        else if (token.getValueString().equals("putchar")) {
            if (analyseExpr()!=TokenType.Uint)
                throw new AnalyzeError(ErrorCode.TypeDifferent,token.getStartPos());
            instructions.add(new Instruction(Operation.print_c));
            expect(TokenType.RParen);
            return;
        }
        else if (token.getValueString().equals("putln")) {
            instructions.add(new Instruction(Operation.println));
            expect(TokenType.RParen);
            return;
        }
        else {
            instructions.add(new Instruction(Operation.stackAlloc,searchFunction(token)==TokenType.VOID?0:1));
            SymbolEntry symbolEntry=symbolTableList.get(0).get(token.getValueString());
            for (int i=0;i<symbolEntry.parameterCount;i++){
                if (i!=0)
                    expect(TokenType.COMMA);
                analyseExpr();
            }
            instructions.add(new Instruction(Operation.call,symbolEntry.number));
            expect(TokenType.RParen);
            return;
        }

    }


    private void analyseCallParamList() throws CompileError {
        analyseExpr();
        while (nextIf(TokenType.COMMA) != null) {
            analyseExpr();
        }
    }

    private TokenType analyseLiteral_Expr() throws CompileError {
        peekedToken = peek();
        Token token;
        if (peekedToken.getTokenType() == TokenType.Uint) {
            token=expect(TokenType.Uint);
            if (token.getValue() instanceof Long){
                instructions.add(new Instruction(Operation.push,(long)token.getValue()));
            }
            else
                instructions.add(new Instruction(Operation.push,(int)token.getValue()));
            return TokenType.Uint;
        } else if (peekedToken.getTokenType() == TokenType.String) {
            token=next();//strNum在定义时增加了
            defineIdent(token,true,TokenType.String);
            instructions.add(new Instruction(Operation.push,strNum-1));
            return TokenType.Uint;
        } else if (peekedToken.getTokenType() == TokenType.Double) {
            token=expect(TokenType.Double);
            instructions.add(new Instruction(Operation.push,(Double)token.getValue()));// TODO: 2020-12-25 ?
            return TokenType.Double;
        } else
            throw new AnalyzeError(ErrorCode.ExpectedLiteral_expr, /* 当前位置 */ peekedToken.getStartPos());
    }


    private TokenType analyseIdent_Expr() throws CompileError {
        Token token;
        token = expect(TokenType.Ident);
        return searchGlobal(token);
    }

    private TokenType analyseGroup_Expr() throws CompileError {
        TokenType tokenType;
        expect(TokenType.LParen);
        tokenType = analyseExpr();
        expect(TokenType.RParen);
        return tokenType;
    }

    private void analyseStmt() throws CompileError {
        // 表达式 -> 运算符表达式|取反|赋值|类型转换|call|字面量|标识符|括号
        peekedToken = peek();
        if (peekedToken.getTokenType() == TokenType.Ident ||
                peekedToken.getTokenType() == TokenType.Minus ||
                peekedToken.getTokenType() == TokenType.LParen ||
                peekedToken.getTokenType() == TokenType.Uint ||
                peekedToken.getTokenType() == TokenType.String ||
                peekedToken.getTokenType() == TokenType.Double) {
            analyseExpr_stmt();
        } else if (peekedToken.getTokenType() == TokenType.LET_KW ||
                peekedToken.getTokenType() == TokenType.CONST_KW) {
            analyseDecl_stmt();
        } else if (peekedToken.getTokenType() == TokenType.IF_KW) {
            analyseIf_stmt();
        } else if (peekedToken.getTokenType() == TokenType.WHILE_KW) {
            analyseWhile_stmt();
        } else if (peekedToken.getTokenType() == TokenType.RETURN_KW) {
            analyseReturn_stmt();
        } else if (peekedToken.getTokenType() == TokenType.L_BRACE) {
            analyseBlock_stmt();
        } else if (peekedToken.getTokenType() == TokenType.Semicolon) {
            expect(TokenType.Semicolon);
        }
    }

    private void analyseExpr_stmt() throws CompileError {
        analyseExpr();
        expect(TokenType.Semicolon);
    }

    private void analyseDecl_stmt() throws CompileError {
        Token token, type;
        TokenType tokenType;
        if (nextIf(TokenType.LET_KW) != null) {
            token = expect(TokenType.Ident);
            expect(TokenType.COLON);
            type = expect(TokenType.TYPE);
            tokenType = getTokenTypeOfUintOrDouble(type);
            if(tokenType==TokenType.VOID)
                throw new AnalyzeError(ErrorCode.NeedUintOrDouble, type.getStartPos());
            if (nextIf(TokenType.ASSIGN) != null) {
                if (tokenType != analyseExpr())
                    throw new AnalyzeError(ErrorCode.TypeDifferent, /* 当前位置 */ peekedToken.getStartPos());
            }
            expect(TokenType.Semicolon);
            defineIdent(token, false, tokenType);
        } else if (nextIf(TokenType.CONST_KW) != null) {
            expect(TokenType.CONST_KW);
            token = expect(TokenType.Ident);
            expect(TokenType.COLON);
            type = expect(TokenType.TYPE);
            tokenType = getTokenTypeOfUintOrDouble(type);
            expect(TokenType.ASSIGN);
            if (tokenType != analyseExpr())
                throw new AnalyzeError(ErrorCode.TypeDifferent, /* 当前位置 */ peekedToken.getStartPos());
            expect(TokenType.Semicolon);
            instructions.add(new Instruction(Operation.store64));
            defineIdent(token, true, tokenType);
        }
    }

    private TokenType getTokenTypeOfUintOrDouble(Token type) throws AnalyzeError {
        TokenType tokenType;
        if (type.getValueString().compareTo("int") == 0)
            tokenType = TokenType.Uint;
        else if (type.getValueString().compareTo("double") == 0)
            tokenType = TokenType.Double;
        else if (type.getValueString().compareTo("void") == 0)
            tokenType = TokenType.VOID;
        else
            throw new AnalyzeError(ErrorCode.NeedUintOrDouble, /* 当前位置 */ peekedToken.getStartPos());
        return tokenType;
    }

    private void analyseIf_stmt() throws CompileError {
        expect(TokenType.IF_KW);
        analyseExpr();
        analyseBlock_stmt();
        if (nextIf(TokenType.ELSE_KW) != null) {
            peekedToken = peek();
            if (peekedToken.getTokenType() == TokenType.IF_KW) {
                analyseIf_stmt();
            } else
                analyseBlock_stmt();
        }
    }

    private void analyseWhile_stmt() throws CompileError {
        expect(TokenType.WHILE_KW);
        analyseExpr();
        analyseBlock_stmt();

    }

    private TokenType analyseReturn_stmt() throws CompileError {
        TokenType tokenType = TokenType.VOID;
        expect(TokenType.RETURN_KW);
        if (nextIf(TokenType.Semicolon) == null){
            tokenType = analyseExpr();
            expect(TokenType.Semicolon);
        }
        return tokenType;
    }

    private TokenType analyseBlock_stmt() throws CompileError {
        TokenType tokenType = TokenType.VOID;
        if (inFunction == false) {
            HashMap<String, SymbolEntry> symbolTable = new HashMap<>();
            symbolTableList.add(symbolTable);
            listLength++;
        } else {
            inFunction = false;
        }
        expect(TokenType.L_BRACE);
        peek();
        while (firstOfStmt()) {
//            if (peekedToken.getTokenType() == TokenType.EOF) {
//                throw new AnalyzeError(ErrorCode.InvalidExpr, peek().getStartPos());
//            }
//            if (peek().getTokenType() == TokenType.RETURN_KW) {
//                returnType = analyseReturn_stmt();
//                if (tokenType == null)
//                    tokenType = returnType;
//                else if (tokenType != returnType) {
//                    throw new AnalyzeError(ErrorCode.TypeDifferent, peek().getStartPos());
//                }
//
//            }
            analyseStmt();
        }
        expect(TokenType.R_BRACE);
        symbolTableList.remove(listLength);
        listLength--;

        return tokenType;
    }

    private void analyseEmpty_stmt() throws CompileError {
        expect(TokenType.Semicolon);
    }

    private TokenType analyseFunction() throws CompileError {
        instructions.clear();
        inFunction = true;
        Token token, type;
        TokenType tokenType;
        HashMap<String, SymbolEntry> symbolTable = new HashMap<>();
        symbolTableList.add(symbolTable);
        listLength++;
        expect(TokenType.FN_KW);
        token = expect(TokenType.Ident);
        expect(TokenType.LParen);
        List<TokenType> parameterList = new ArrayList<>();

        if (peek().getTokenType() == TokenType.Ident)
            parameterList = analyseFunctionParamList();
        expect(TokenType.RParen);
        expect(TokenType.ARROW);
        type = expect(TokenType.TYPE);
        tokenType = analyseBlock_stmt();
        if (tokenType != getTokenTypeOfUintOrDouble(type))
            throw new AnalyzeError(ErrorCode.TypeDifferent, peek().getStartPos());

        defineFunction(token, tokenType, parameterList.size(), parameterList);
        SymbolEntry symbolEntry=symbolTableList.get(0).get(token.getValueString());
        symbolEntry.instructionList.addAll(instructions);
        if(type.getValueString()=="void")
            instructions.add(new Instruction(Operation.ret));
        instructions.clear();
        return tokenType;
    }

    private List<TokenType> analyseFunctionParamList() throws CompileError {
        List<TokenType> parameterList = new ArrayList<>();
        parameterList.add(analyseFunctionParam());
        while (nextIf(TokenType.COMMA) != null) {
            parameterList.add(analyseFunctionParam());
        }
        return parameterList;
    }

    private TokenType analyseFunctionParam() throws CompileError {
        Token token, type;
        TokenType tokenType;
        nextIf(TokenType.CONST_KW);
        token = expect(TokenType.Ident);

        expect(TokenType.COLON);
        type = expect(TokenType.TYPE);
        tokenType = getTokenTypeOfUintOrDouble(type);
        defineIdent(token, false, tokenType);
        return tokenType;
    }

    private void analyseProgram() throws CompileError, IOException {
        //program -> decl_stmt* function*
        /** 符号表 */
        symbolTable = new HashMap<>();
        /** 符号表集 */
        symbolTableList = new ArrayList<>();
        symbolTableList.add(symbolTable);
        listLength = 0;
        b = new byte[100000];
        /** 下一个变量的栈偏移 */
        nextOffset = 0;
        instructions = new ArrayList<>();
        peekedToken = peek();
        while (peekedToken.getTokenType() == TokenType.LET_KW ||
                peekedToken.getTokenType() == TokenType.FN_KW) {
            if (peekedToken.getTokenType() == TokenType.LET_KW)
                analyseDecl_stmt();
            else
                analyseFunction();
            peekedToken = peek();
        }
        Token token = new Token(TokenType.Ident, "main", null, null);
        searchFunction(token);
        DataOutputStream out = new DataOutputStream(new FileOutputStream("output.txt"));
        output(out);
        out.close();
    }

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

    private void output(DataOutputStream out) throws CompileError, IOException {
        List<Instruction> instructionList=new ArrayList<>();
        b=new byte[10000];
        symbolTable=symbolTableList.get(0);
        int32ToByte(0x72303b3e);
        int32ToByte(0x00000001);
        int IdentNum=0,FunctionNum=0;
        for (Map.Entry<String, SymbolEntry> entry : symbolTable.entrySet()) {
            if(!entry.getValue().isFunction)
                IdentNum++;
            else{
                FunctionNum++;
                entry.getValue().number=IdentNum+FunctionNum;
            }

        }
        int32ToByte(IdentNum);
        for (Map.Entry<String, SymbolEntry> entry : symbolTable.entrySet()) {
            if(!entry.getValue().isFunction){
                boolToByte(entry.getValue().isConstant);
                if(entry.getValue().tokenType==TokenType.Uint){
                    int32ToByte(8);
                    int64ToByte(entry.getValue().uintValue);
                    instructionList.addAll(entry.getValue().instructionList);
                }
                else if(entry.getValue().tokenType==TokenType.Double){
                    int32ToByte(8);
                    doubleToByte(entry.getValue().doubleValue);
                    instructionList.addAll(entry.getValue().instructionList);
                }
                else if(entry.getValue().tokenType==TokenType.String){
                    int32ToByte(entry.getValue().stringValue.length());
                    stringToByte(entry.getValue().stringValue);
                    instructionList.addAll(entry.getValue().instructionList);
                }
            }
            else {
                int32ToByte(entry.getValue().name.length());
                stringToByte(entry.getValue().name);
                instructionList.addAll(entry.getValue().instructionList);
            }
        }
        int32ToByte(FunctionNum+1);
        // TODO: 2020-12-13 start函数

        int32ToByte(IdentNum);
        int32ToByte(0);
        int32ToByte(0);
        int32ToByte(0);
        int l=instructions.size();
        int32ToByte(l);
        for(int i=0;i<l;i++){
            instructionToByte(instructions.get(i));
        }

        for (Map.Entry<String, SymbolEntry> entry : symbolTable.entrySet()) {
            if(entry.getValue().isFunction){
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
                int32ToByte(entry.getValue().number);
                if(entry.getValue().tokenType==TokenType.VOID)
                    int32ToByte(0);
                else
                    int32ToByte(8);
                int32ToByte(entry.getValue().parameterCount);
                int32ToByte(entry.getValue().localParameterNum);
                int32ToByte(entry.getValue().instructionNum);
                l=entry.getValue().instructionNum;
                for(int i=0;i<l;i++){
                    instructionToByte(entry.getValue().instructionList.get(i));
                }
            }
        }
        out.write(b);
    }
}
