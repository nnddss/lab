package miniplc0java.error;

public enum ErrorCode {
    NoError, // Should be only used internally.
    InvalidAsStmt,InvalidType,Pos1,InvalidStringLiteral,DoubleOverflow,InvalidDouble,InvalidChar,
    StreamError, EOF, InvalidInput, InvalidIdentifier, IntegerOverflow, // int32_t overflow.
    NoBegin, NoEnd, NeedIdentifier, ConstantNeedValue, NoSemicolon, InvalidVariableDeclaration, IncompleteExpression,
    NotDeclared, AssignToConstant, DuplicateDeclaration, NotInitialized, InvalidAssignment, InvalidPrint, ExpectedToken, InvalidReturn,NoMainFunction
}
