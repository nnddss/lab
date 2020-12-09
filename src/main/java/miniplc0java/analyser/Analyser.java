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

import java.lang.instrument.Instrumentation;
import java.util.*;

public final class Analyser {

    Tokenizer tokenizer;
    ArrayList<Instruction> instructions;
    Instrumentation instrumentation;
    /** 当前偷看的 token */
    Token peekedToken = null;
    boolean inFunction=false;
    //如果是从function进入block会多加一层，
    //所以在进入function时创建局部符号表并且将该值置为true，
    //这样在进入block时不在创建局部符号表，
    //并将其置为false
    /** 符号表 */
    HashMap<String, SymbolEntry> symbolTable = new HashMap<>();
    /** 符号表集 */
    List<HashMap<String,SymbolEntry>> symbolTableList = new ArrayList<>();
    int listLength=0;
    /** 下一个变量的栈偏移 */
    int nextOffset = 0;

    public Analyser(Tokenizer tokenizer) {
        this.tokenizer = tokenizer;
        this.instructions = new ArrayList<>();
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
    private void defineIdent(Token token,boolean isConstant) throws CompileError {
        //boolean isConstant, boolean isDeclared, int stackOffset,
        // TokenType tokenType, Pos pos
        HashMap<String, SymbolEntry> symbolTable = symbolTableList.get(listLength);
        if(symbolTable.containsKey(token.getValueString())==false){
            SymbolEntry symbolEntry=new SymbolEntry(isConstant,true,false,
                    nextOffset,token.getTokenType().toString(),token.getStartPos());
            nextOffset= (int) (nextOffset+ instrumentation.getObjectSize(token.getValue()));
            symbolTable.put(token.getValueString(),symbolEntry);
        }
        else {
            throw new AnalyzeError(ErrorCode.DuplicateDeclaration, token.getStartPos());
        }
    }
    private void defineFunction(Token token,Token type,int parameterCount,List parameterList) throws CompileError {
        //boolean isConstant, boolean isDeclared, int stackOffset,
        // TokenType tokenType, Pos pos
        HashMap<String, SymbolEntry> symbolTable = symbolTableList.get(0);
        if(symbolTable.containsKey(token.getValueString())==false){
            SymbolEntry symbolEntry=new SymbolEntry(true,true,
                    nextOffset,type.getTokenType().toString(),token.getStartPos(),parameterCount,parameterList);
            nextOffset= (int) (nextOffset+ instrumentation.getObjectSize(token.getValue()));
            symbolTable.put(token.getValueString(),symbolEntry);
        }
        else {
            throw new AnalyzeError(ErrorCode.DuplicateDeclaration, token.getStartPos());
        }
    }

    private void searchLocal(Token token) throws CompileError {
        //boolean isConstant, boolean isDeclared, int stackOffset,
        // TokenType tokenType, Pos pos
        HashMap<String, SymbolEntry> symbolTable = symbolTableList.get(listLength);
        if(symbolTable.containsKey(token.getValueString())==false){
            throw new AnalyzeError(ErrorCode.NotDeclared, token.getStartPos());
        }
    }
    private void searchFunction(Token token) throws CompileError {
        //boolean isConstant, boolean isDeclared, int stackOffset,
        // TokenType tokenType, Pos pos
        HashMap<String, SymbolEntry> symbolTable = symbolTableList.get(0);
        if(symbolTable.containsKey(token.getValueString())==false){
            throw new AnalyzeError(ErrorCode.NotDeclared, token.getStartPos());
        }
    }
    private void searchGlobal(Token token) throws CompileError {
        //boolean isConstant, boolean isDeclared, int stackOffset,
        // TokenType tokenType, Pos pos
        for(int i=listLength;i>=0;i--){
            HashMap<String, SymbolEntry> symbolTable = symbolTableList.get(i);
            if(symbolTable.containsKey(token.getValueString())){
                return;
            }
        }
        throw new AnalyzeError(ErrorCode.NotDeclared, token.getStartPos());
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
    private void analyseExpr() throws CompileError {
        // 表达式 -> 运算符表达式|取反|赋值|类型转换|call|字面量|标识符|括号
        peekedToken=peek();
        if (peekedToken.getTokenType() == TokenType.Minus) {
            analyseNegate_Expr();
        }
        else if(peekedToken.getTokenType() == TokenType.LParen){
            analyseGroup_Expr();
        }
        else if(peekedToken.getTokenType() == TokenType.Ident){
            analyseAssign_ExprOrIdent_ExprOrCall_Expr();
        }
        else if(peekedToken.getTokenType() == TokenType.Uint||
                peekedToken.getTokenType() == TokenType.Double||
                peekedToken.getTokenType() == TokenType.String
        ){
            analyseLiteral_Expr();
        }
        while(
              nextIf(TokenType.Plus)!=null||
              nextIf(TokenType.Minus)!=null||
              nextIf(TokenType.Div)!=null||
              nextIf(TokenType.Mult)!=null||
              nextIf(TokenType.Equal)!=null||
              nextIf(TokenType.NEQ)!=null||
              nextIf(TokenType.LE)!=null||
              nextIf(TokenType.LT)!=null||
              nextIf(TokenType.GE)!=null||
              nextIf(TokenType.GT)!=null
        ){
            analyseExpr();
        }
        while(nextIf(TokenType.ASSIGN)!=null){
            expect(TokenType.TYPE);
        }
    }

    private void analyseNegate_Expr() throws CompileError {
        expect(TokenType.Minus);
        analyseExpr();
    }
    private void analyseAssign_ExprOrIdent_ExprOrCall_Expr() throws CompileError {

        peekedToken=peek();
        if (nextIf(TokenType.Equal) != null) {
            analyseAssign_Expr();
        }
        else if(nextIf(TokenType.LParen) != null) {
            analyseCall_Expr();
        }
        else {
            analyseIdent_Expr();
        }

    }
    private void analyseAssign_Expr() throws CompileError {
        searchGlobal(expect(TokenType.Ident));
        expect(TokenType.ASSIGN);
        analyseExpr();
    }
    private void analyseCall_Expr() throws CompileError {

        searchFunction(expect(TokenType.Ident));
        expect(TokenType.LParen);
        analyseCallParamList();
        expect(TokenType.RParen);

    }
    private void analyseCallParamList() throws CompileError {
        analyseExpr();
        while (nextIf(TokenType.COMMA)!=null){
            analyseExpr();
        }
    }
    private void analyseLiteral_Expr() throws CompileError {
        peekedToken=peek();
        if ( peekedToken.getTokenType() == TokenType.Uint) {
            expect(TokenType.Uint);
        }
        else if ( peekedToken.getTokenType() == TokenType.String) {
            expect(TokenType.String);
        }
        else if ( peekedToken.getTokenType() == TokenType.Double) {
            expect(TokenType.Double);
        }
        else
            throw new AnalyzeError(ErrorCode.ExpectedLiteral_expr, /* 当前位置 */ peekedToken.getStartPos());
    }


    private void analyseIdent_Expr() throws CompileError {
        Token token;
        token=expect(TokenType.Ident);
        searchGlobal(token);
    }
    private void analyseGroup_Expr() throws CompileError {
        expect(TokenType.LParen);
        analyseExpr();
        expect(TokenType.RParen);
    }

    private void analyseStmt() throws CompileError {
        // 表达式 -> 运算符表达式|取反|赋值|类型转换|call|字面量|标识符|括号
        peekedToken=peek();
        if(peekedToken.getTokenType()==TokenType.Ident||
           peekedToken.getTokenType()==TokenType.Minus||
           peekedToken.getTokenType()==TokenType.LParen||
           peekedToken.getTokenType()==TokenType.Uint||
           peekedToken.getTokenType()==TokenType.String||
           peekedToken.getTokenType()==TokenType.Double){
            analyseExpr_stmt();
        }
        else if(peekedToken.getTokenType()==TokenType.LET_KW||
                peekedToken.getTokenType()==TokenType.CONST_KW){
            analyseDecl_stmt();
        }
        else if(peekedToken.getTokenType()==TokenType.IF_KW){
            analyseIf_stmt();
        }
        else if(peekedToken.getTokenType()==TokenType.WHILE_KW){
            analyseWhile_stmt();
        }
        else if(peekedToken.getTokenType()==TokenType.RETURN_KW){
            analyseReturn_stmt();
        }
        else if(peekedToken.getTokenType()==TokenType.L_BRACE){
            analyseBlock_stmt();
        }
        else if(peekedToken.getTokenType()==TokenType.Semicolon){
            expect(TokenType.Semicolon);
        }
    }
    private void analyseExpr_stmt() throws CompileError {
        analyseExpr();
        expect(TokenType.Semicolon);
    }
    private void analyseDecl_stmt() throws CompileError {
        Token token;
        if(nextIf(TokenType.LET_KW)!=null){
            token=expect(TokenType.Ident);
            defineIdent(token,false);
            expect(TokenType.COLON);
            expect(TokenType.TYPE);
            if(nextIf(TokenType.ASSIGN)!=null){
                analyseExpr();
            }
            expect(TokenType.Semicolon);
        }
        if(nextIf(TokenType.CONST_KW)!=null){
            expect(TokenType.CONST_KW);
            token=expect(TokenType.Ident);
            defineIdent(token,true);
            expect(TokenType.COLON);
            expect(TokenType.TYPE);
            expect(TokenType.ASSIGN);
            analyseExpr();
            expect(TokenType.Semicolon);
        }
    }
    private void analyseIf_stmt() throws CompileError {
        expect(TokenType.IF_KW);
        analyseExpr();
        analyseBlock_stmt();
        if(nextIf(TokenType.ELSE_KW)!=null){
            peekedToken=peek();
            if(peekedToken.getTokenType()==TokenType.IF_KW){
                analyseIf_stmt();
            }
            else
                analyseBlock_stmt();
        }
    }
    private void analyseWhile_stmt() throws CompileError {
        expect(TokenType.WHILE_KW);
        analyseExpr();
        analyseBlock_stmt();

    }
    private void analyseReturn_stmt() throws CompileError {
        expect(TokenType.RETURN_KW);
        if(nextIf(TokenType.Semicolon)==null)
            analyseExpr();
        expect(TokenType.Semicolon);
    }
    private void analyseBlock_stmt() throws CompileError {
        if(inFunction==false){
            HashMap<String, SymbolEntry> symbolTable = new HashMap<>();
            symbolTableList.add(symbolTable);
            listLength++;
        }
        else {
            inFunction = false;
        }
        expect(TokenType.L_BRACE);
        while (nextIf(TokenType.R_BRACE)==null){
            analyseStmt();
        }
        symbolTableList.remove(listLength);
        listLength--;
    }
    private void analyseEmpty_stmt() throws CompileError {
       expect(TokenType.Semicolon);
    }

    private void analyseFunction() throws CompileError {
        inFunction=true;
        Token token,type;
        HashMap<String, SymbolEntry> symbolTable = new HashMap<>();
        symbolTableList.add(symbolTable);
        listLength++;
        expect(TokenType.FN_KW);
        token=expect(TokenType.Ident);

        expect(TokenType.LParen);
        List<String> parameterList=analyseFunctionParamList();
        expect(TokenType.RParen);
        expect(TokenType.ARROW);
        type=expect(TokenType.TYPE);
        analyseBlock_stmt();
        defineFunction(token,type,parameterList.size(),parameterList);
    }
    private List<String> analyseFunctionParamList() throws CompileError {
        List<String> parameterList=new ArrayList<>();
        parameterList.add(analyseFunctionParam());
        while (nextIf(TokenType.COMMA)!=null){
            parameterList.add(analyseFunctionParam());
        }
        return  parameterList;
    }
    private String analyseFunctionParam() throws CompileError {
        Token token;
        nextIf(TokenType.CONST_KW);
        token=expect(TokenType.Ident);
        defineIdent(token,false);
        expect(TokenType.COLON);
        expect(TokenType.TYPE);
        return token.getValueString();
    }
    private void analyseProgram() throws CompileError {
        //program -> decl_stmt* function*
        peekedToken=peek();
        while (peekedToken.getTokenType()==TokenType.LET_KW||
                peekedToken.getTokenType()==TokenType.FN_KW){
            if(peekedToken.getTokenType()==TokenType.LET_KW)
                analyseDecl_stmt();
            else
                analyseFunction();
            peekedToken=peek();
        }
    }
}
