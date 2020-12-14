//package miniplc0java.instruction;
//
//import java.util.Objects;
//
//public class Instruction {
//    private Operation opt;
//    Integer x;
//
//    public Instruction(Operation opt) {
//        this.opt = opt;
//        this.x = 0;
//    }
//
//    public Instruction(Operation opt, Integer x) {
//        this.opt = opt;
//        this.x = x;
//    }
//
//    public Instruction() {
//        this.opt = Operation.LIT;
//        this.x = 0;
//    }
//
//    @Override
//    public boolean equals(Object o) {
//        if (this == o)
//            return true;
//        if (o == null || getClass() != o.getClass())
//            return false;
//        Instruction that = (Instruction) o;
//        return opt == that.opt && Objects.equals(x, that.x);
//    }
//
//    @Override
//    public int hashCode() {
//        return Objects.hash(opt, x);
//    }
//
//    public Operation getOpt() {
//        return opt;
//    }
//
//    public void setOpt(Operation opt) {
//        this.opt = opt;
//    }
//
//    public Integer getX() {
//        return x;
//    }
//
//    public void setX(Integer x) {
//        this.x = x;
//    }
//
//    @Override
//    public String toString() {
//        switch (this.opt) {
//            case ADD:
//            case DIV:
//            case ILL:
//            case MUL:
//            case SUB:
//            case WRT:
//                return String.format("%s", this.opt);
//            case LIT:
//            case LOD:
//            case STO:
//                return String.format("%s %s", this.opt, this.x);
//            default:
//                return "ILL";
//        }
//    }
//}
package miniplc0java.instruction;

import java.util.Objects;

public class Instruction {
    public int opt;
    public Integer x;
    public int parameterLength;
    public Instruction(int opt) {
        this.opt = opt;
        this.x = 0;
    }

    public Instruction(int opt, Integer x) {
        this.opt = opt;
        this.x = x;
        this.parameterLength=getParameterLength(opt);
    }
    public int getParameterLength(int opt){
        switch (opt){
            case 0x01:return 8;
            case 0x03:
            case 0x0a:
            case 0x0b:
            case 0x0c:
            case 0x1a:
            case 0x41:
            case 0x42:
            case 0x43:
            case 0x48:
            case 0x4a:
                return 4;
            default:
                return 0;
        }
    }


    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Instruction that = (Instruction) o;
        return opt == that.opt && Objects.equals(x, that.x);
    }


}
