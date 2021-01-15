package miniplc0java.instruction;

import miniplc0java.error.AnalyzeError;
import miniplc0java.error.ErrorCode;
import miniplc0java.util.Pos;

public enum Operation {
    nop1, nop2, push, pop, loca, arga, globa,load_64,store_64,stackalloc,add_i,sub_i,mul_i,div_i,add_f,sub_f,mul_f,div_f,div_u,xor,not,cmp_i,cmp_u,cmp_f,neg_i,neg_f,itof,ftoi,set_lt,set_gt,br,br_false,br_true,call,ret,scan_i,scan_c,scan_f,print_i,print_c,print_f,print_s,print_ln;
    public int getOperationParamLength(){
        switch (this) {
            case push:
                return  8 ;
            case pop:
                return  0x02 ;
            case loca:
            case arga:
            case globa:
            case stackalloc:
            case br:
            case br_false:
            case br_true:
            case call:
                return  4;
            default:
                return 0;
        }
    }public int toInstruction() throws AnalyzeError {
        switch (this) {
            case nop1:
                return  0x00 ;
            case push:
                return  0x01 ;
            case pop:
                return  0x02 ;
            case loca:
                return  0x0a ;
            case arga:
                return  0x0b ;
            case globa:
                return  0x0c ;
            case load_64:
                return  0x13 ;
            case store_64:
                return  0x17 ;
            case stackalloc:
                return  0x1a ;
            case call:
                return  0x48 ;
            case add_i:
                return  0x20 ;
            case add_f:
                return  0x24 ;
            case sub_i:
                return  0x21 ;
            case sub_f:
                return  0x25 ;
            case mul_i:
                return  0x22 ;
            case mul_f:
                return  0x26 ;
            case div_i:
                return  0x23 ;
            case div_f:
                return  0x27 ;
            case div_u:
                return  0x28 ;
            case xor:
                return  0x2d ;
            case not:
                return  0x2e ;
            case cmp_i:
                return  0x30 ;
            case cmp_u:
                return  0x31 ;
            case cmp_f:
                return  0x32 ;
            case neg_i:
                return  0x34 ;
            case neg_f:
                return  0x35 ;
            case set_lt:
                return  0x39 ;
            case set_gt:
                return  0x3a ;
            case br:
                return  0x41 ;
            case br_false:
                return  0x42 ;
            case br_true:
                return  0x43 ;
            case itof:
                return  0x36 ;
            case ftoi:
                return  0x37 ;
            case ret:
                return  0x49 ;
            case scan_i:
                return  0x50 ;
            case scan_c:
                return  0x51 ;
            case scan_f:
                return  0x52 ;
            case print_i:
                return  0x54 ;
            case print_c:
                return  0x55 ;
            case print_f:
                return  0x56 ;
            case print_s:
                return  0x57 ;
            case print_ln:
                return  0x58 ;
            default:
                throw new AnalyzeError(ErrorCode.InvalidType, new Pos(0, 0));
        }
}
}
