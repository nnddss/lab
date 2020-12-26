package miniplc0java.tokenizer;

import miniplc0java.error.ErrorCode;
import miniplc0java.error.TokenizeError;
import miniplc0java.util.Pos;

import static miniplc0java.error.ErrorCode.ExpectedApostrophe;
import static miniplc0java.error.ErrorCode.IllegalEscapeCharacter;

public class Tokenizer {

    private StringIter it;

    public Tokenizer(StringIter it) {
        this.it = it;
    }

    // 这里本来是想实现 Iterator<Token> 的，但是 Iterator 不允许抛异常，于是就这样了
    /**
     * 获取下一个 Token
     * 
     * @return
     * @throws TokenizeError 如果解析有异常则抛出
     */
    public Token nextToken() throws TokenizeError {
        it.readAll();

        // 跳过之前的所有空白字符
        skipSpaceCharacters();

        if (it.isEOF()) {
            return new Token(TokenType.EOF, "", it.currentPos(), it.currentPos());
        }

        char peek = it.peekChar();
        if (Character.isDigit(peek)) {
            return lexUIntOrDouble();
        }else if (peek=='\'') {
            return lexChar();
        }else if (peek=='\"') {
            return lexString();
        }else if (Character.isAlphabetic(peek)) {
            return lexIdentOrKeyword();
        }else {
            return lexOperatorOrUnknown();
        }
    }

    private Token lexUIntOrDouble() throws TokenizeError {
        // 请填空：
        // 直到查看下一个字符不是数字为止:
        // -- 前进一个字符，并存储这个字符
        //
        // 解析存储的字符串为无符号整数
        // 解析成功则返回无符号整数类型的token，否则返回编译错误
        //
        // Token 的 Value 应填写数字的值
        //throw new Error("Not implemented");
        Pos startPos = new Pos(it.currentPos().row,it.currentPos().col);
        StringBuffer number=new StringBuffer();
        char peek=it.peekChar();
        boolean isDouble=false;
        while(true){
            number.append(it.nextChar());
            peek=it.peekChar();
            if(Character.isDigit(peek)||peek=='e'||peek=='E'||peek=='+'||peek=='-'){
                continue;
            }
            else if(peek=='.'&&!isDouble){
                isDouble=true;
            }
            else {
                break;
            }
        }
        try {
            if(!isDouble){
                Integer num = Integer.parseInt(number.toString());
                Pos endPos = new Pos(it.currentPos().row,it.currentPos().col);
                Token token=new Token(TokenType.Uint, num, startPos, endPos);
                return token;
            }
            else{
                Double num =Double.parseDouble(number.toString());
                Pos endPos = new Pos(it.currentPos().row,it.currentPos().col);
                Token token=new Token(TokenType.Double, num, startPos, endPos);
                return token;
            }
        }
        catch (NumberFormatException numberFormatException){
            throw numberFormatException;
        }



    }
    private Token lexString() throws TokenizeError {
        // 请填空：
        // 直到查看下一个字符不是数字为止:
        // -- 前进一个字符，并存储这个字符
        //
        // 解析存储的字符串为无符号整数
        // 解析成功则返回无符号整数类型的token，否则返回编译错误
        //
        // Token 的 Value 应填写数字的值
        //throw new Error("Not implemented");
        StringBuilder stringBuilder = new StringBuilder();
        Pos start = it.currentPos();
        it.nextChar();
        while(!it.isEOF()) {
            char peek = it.peekChar();
            if (peek == '\\') {
//                stringBuilder.append(it.nextChar());
                it.nextChar();
                peek = it.peekChar();
                switch (peek) {
                    case '\'':
                        stringBuilder.append('\'');
                        break;
                    case '\"':
                        stringBuilder.append('\"');
                        break;
                    case '\\':
                        stringBuilder.append('\\');
                        break;
                    case 'n':
                        stringBuilder.append('\n');
                        break;
                    case 't':
                        stringBuilder.append('\t');
                        break;
                    case 'r':
                        stringBuilder.append('\r');
                        break;
                    default:
                        throw new TokenizeError(ErrorCode.InvalidInput,it.currentPos());
                }
                it.nextChar();
            } else if (peek == '\"') {
                it.nextChar();
                break;
            } else {
                stringBuilder.append(it.nextChar());
            }
        }
        String result = stringBuilder.toString();
        Pos end = it.currentPos();
        return new Token(TokenType.String, result, start, end);
    }
    private Token lexChar() throws TokenizeError {
        // 请填空：
        // 直到查看下一个字符不是数字为止:
        // -- 前进一个字符，并存储这个字符
        //
        // 解析存储的字符串为无符号整数
        // 解析成功则返回无符号整数类型的token，否则返回编译错误
        //
        // Token 的 Value 应填写数字的值
        //throw new Error("Not implemented");
        Pos startPos = new Pos(it.currentPos().row,it.currentPos().col);
        char c;
        char now;
        boolean isEscape=false;
        now=it.nextChar();
        if(now=='\\'){
            now=it.nextChar();
            switch (now){
                case 'n': c='\n';break;
                case '\'': c='\'';break;
                case '\"': c='\"';break;
                case '\\': c='\\';break;
                case 'r': c='\r';break;
                case 't': c='\t';break;
                default:throw new TokenizeError(IllegalEscapeCharacter, it.currentPos());
            }
        }
        else if(now!='\''&&now!='\"')
            c=now;
        else {
           throw new TokenizeError(IllegalEscapeCharacter, it.currentPos());
        }
        now=it.nextChar();
        if(now!='\'')
            throw new TokenizeError(ExpectedApostrophe, it.currentPos());
        Pos endPos = new Pos(it.currentPos().row,it.currentPos().col);
        Token token=new Token(TokenType.Char,c, startPos, endPos);
        return token;
    }

    private Token lexIdentOrKeyword() throws TokenizeError {
        // 请填空：
        // 直到查看下一个字符不是数字或字母为止:
        // -- 前进一个字符，并存储这个字符
        //
        // 尝试将存储的字符串解释为关键字
        // -- 如果是关键字，则返回关键字类型的 token
        // -- 否则，返回标识符
        //
        // Token 的 Value 应填写标识符或关键字的字符串
        //throw new Error("Not implemented");
        Pos startPos = new Pos(it.currentPos().row,it.currentPos().col);
        String IdentOrKeyword=new String();
        while(true){
            IdentOrKeyword+=it.nextChar();
            char peek=it.peekChar();
            if(Character.isAlphabetic(peek)||Character.isDigit(peek)||peek=='_'){
                continue;
            }
            else{
                break;
            }
        }
        Pos endPos = new Pos(it.currentPos().row,it.currentPos().col);
        if(IdentOrKeyword.compareTo("fn")==0){
            Token token=new Token(TokenType.FN_KW, "fn", startPos, endPos);
            return token;
        }
        else if(IdentOrKeyword.compareTo("const")==0){
            Token token=new Token(TokenType.CONST_KW, "const", startPos, endPos);
            return token;
        }
        else if(IdentOrKeyword.compareTo("let")==0){
            Token token=new Token(TokenType.LET_KW, "let", startPos, endPos);
            return token;
        }
        else if(IdentOrKeyword.compareTo("as")==0){
            Token token=new Token(TokenType.AS_KW, "as", startPos, endPos);
            return token;
        }
        else if(IdentOrKeyword.compareTo("while")==0){
            Token token=new Token(TokenType.WHILE_KW, "while", startPos, endPos);
            return token;
        }
        else if(IdentOrKeyword.compareTo("if")==0){
            Token token=new Token(TokenType.IF_KW, "if", startPos, endPos);
            return token;
        }
        else if(IdentOrKeyword.compareTo("else")==0){
            Token token=new Token(TokenType.ELSE_KW, "else", startPos, endPos);
            return token;
        }
        else if(IdentOrKeyword.compareTo("return")==0){
            Token token=new Token(TokenType.RETURN_KW, "return", startPos, endPos);
            return token;
        }
        else if(IdentOrKeyword.compareTo("break")==0){
            Token token=new Token(TokenType.BREAK_KW, "break", startPos, endPos);
            return token;
        }
        else if(IdentOrKeyword.compareTo("continue")==0){
            Token token=new Token(TokenType.CONTINUE_KW, "continue", startPos, endPos);
            return token;
        }
        else if(IdentOrKeyword.compareTo("int")==0){
            Token token=new Token(TokenType.TYPE, "int", startPos, endPos);
            return token;
        }
        else if(IdentOrKeyword.compareTo("double")==0){
            Token token=new Token(TokenType.TYPE, "double", startPos, endPos);
            return token;
        }
        else if(IdentOrKeyword.compareTo("void")==0){
            Token token=new Token(TokenType.TYPE, "void", startPos, endPos);
            return token;
        }
        else {
            Token token=new Token(TokenType.Ident,IdentOrKeyword, startPos, endPos);
            return token;
        }
    }

    private Token lexOperatorOrUnknown() throws TokenizeError {
        switch (it.nextChar()) {
            case '+':
                return new Token(TokenType.Plus, '+', it.previousPos(), it.currentPos());

            case '-':
                // 填入返回语句
                //throw new Error("Not implemented");
                if(it.peekChar()=='>'){
                    it.nextChar();
                    return new Token(TokenType.ARROW, "->", it.previousPos(), it.currentPos());
                }
                return new Token(TokenType.Minus, '-', it.previousPos(), it.currentPos());

            case '*':
                // 填入返回语句
                //throw new Error("Not implemented");
                return new Token(TokenType.Mult, '*', it.previousPos(), it.currentPos());


            case '/':
                // 填入返回语句
                //throw new Error("Not implemented");
                return new Token(TokenType.Div, '/', it.previousPos(), it.currentPos());

            case '=':
                // 填入返回语句
                //throw new Error("Not implemented");
                if(it.peekChar()=='='){
                    it.nextChar();
                    return new Token(TokenType.Equal, "==", it.previousPos(), it.currentPos());
                }
                return new Token(TokenType.ASSIGN, '=', it.previousPos(), it.currentPos());

            case '(':
                // 填入返回语句
                //throw new Error("Not implemented");
                return new Token(TokenType.LParen, '(', it.previousPos(), it.currentPos());

            case ')':
                // 填入返回语句
                //throw new Error("Not implemented");
                return new Token(TokenType.RParen, ')', it.previousPos(), it.currentPos());
            case '>':
                // 填入返回语句
                //throw new Error("Not implemented");
                if(it.peekChar()=='='){
                    it.nextChar();
                    return new Token(TokenType.GE, ">=", it.previousPos(), it.currentPos());
                }
                return new Token(TokenType.GT, '>', it.previousPos(), it.currentPos());
            case '<':
                // 填入返回语句
                //throw new Error("Not implemented");
                if(it.peekChar()=='='){
                    it.nextChar();
                    return new Token(TokenType.LE, "<=", it.previousPos(), it.currentPos());
                }
                return new Token(TokenType.LT, '<', it.previousPos(), it.currentPos());
            case '{':
                // 填入返回语句
                //throw new Error("Not implemented");
                return new Token(TokenType.L_BRACE, '{', it.previousPos(), it.currentPos());
            case '}':
                // 填入返回语句
                //throw new Error("Not implemented");
                return new Token(TokenType.R_BRACE, '}', it.previousPos(), it.currentPos());
            case ',':
                // 填入返回语句
                //throw new Error("Not implemented");
                return new Token(TokenType.COMMA, ',', it.previousPos(), it.currentPos());
            case ':':
                // 填入返回语句
                //throw new Error("Not implemented");
                return new Token(TokenType.COLON, ':', it.previousPos(), it.currentPos());
            case ';':
                // 填入返回语句
                //throw new Error("Not implemented");
                return new Token(TokenType.Semicolon, ';', it.previousPos(), it.currentPos());
            case '!':
                // 填入返回语句
                //throw new Error("Not implemented");
                if(it.peekChar()=='='){
                    it.nextChar();
                    return new Token(TokenType.NEQ, "!=", it.previousPos(), it.currentPos());
                }
                // 填入更多状态和返回语句

            default:
                // 不认识这个输入，摸了
                throw new TokenizeError(ErrorCode.InvalidInput, it.previousPos());
        }
    }

    private void skipSpaceCharacters() {
        while (!it.isEOF() && Character.isWhitespace(it.peekChar())) {
            it.nextChar();
        }
    }
}
