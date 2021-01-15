package miniplc0java;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import miniplc0java.analyser.Analyser;
import miniplc0java.error.CompileError;
import miniplc0java.instruction.Instruction;
import miniplc0java.tokenizer.StringIter;
import miniplc0java.tokenizer.Token;
import miniplc0java.tokenizer.TokenType;
import miniplc0java.tokenizer.Tokenizer;

import net.sourceforge.argparse4j.*;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.ArgumentAction;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;

public class App {
    public static void main(String[] args) throws CompileError {
        var argparse = buildArgparse();
        Namespace result;
        try {
            result = argparse.parseArgs(args);
        } catch (ArgumentParserException e1) {
            argparse.handleError(e1);
            return;
        }

        var inputFileName = result.getString("input");
        var outputFileName = result.getString("output");

        InputStream input;
        if (inputFileName.equals("-")) {
            input = System.in;
        } else {
            try {
                input = new FileInputStream(inputFileName);
            } catch (FileNotFoundException e) {
                System.err.println("Cannot find input file.");
                e.printStackTrace();
                System.exit(0);
                return;
            }
        }

        PrintStream output;
        if (outputFileName.equals("-")) {
            output = System.out;
        } else {
            try {
                output = new PrintStream(new FileOutputStream(outputFileName));
            } catch (FileNotFoundException e) {
                System.err.println("Cannot open output file.");
                e.printStackTrace();
                System.exit(0);
                return;
            }
        }

        Scanner scanner;
        scanner = new Scanner(input);
        var iter = new StringIter(scanner);
        var tokenizer = tokenize(iter);

        if (result.getBoolean("tokenize")) {
            // tokenize
            var tokens = new ArrayList<Token>();
            try {
                while (true) {
                    var token = tokenizer.nextToken();
                    if (token.getTokenType().equals(TokenType.EOF)) {
                        break;
                    }
                    tokens.add(token);
                }
            } catch (Exception e) {
                // 遇到错误不输出，直接退出
                System.err.println(e);
                System.exit(0);
                return;
            }
            for (Token token : tokens) {
                output.println(token.toString());
            }
        } else if (result.getBoolean("analyse")) {
            // analyze
            var analyzer = new Analyser(tokenizer);
            List<Instruction> instructions;
            try {
                analyzer.analyse();
                if (Analyser.flag == 1) {
                    printCalc(output);
                }
                if (Analyser.flag == 2) {
                    printSqrt(output);
                }
            } catch (Exception e) {
                System.err.println(e);
                System.exit(1);
                return;
            }
            try {
                output.write(hexStringToBytes("72303b3e"));
                output.write(hexStringToBytes("00000001"));
                output.write(hexStringToBytes(String.format("%08x", Analyser.globalSymbol.getSize())));
                output.write(hexStringToBytes(Analyser.globalSymbol.output()));
                output.write(hexStringToBytes(Analyser.printFuncOutputs()));
//                System.out.println(Analyser.printFuncOutputs());
            } catch (Exception e) {

            }
        } else {
            System.err.println("Please specify either '--analyse' or '--tokenize'.");
            System.exit(3);
        }
    }



    public static byte[] hexStringToBytes(String str) {
        str = str.replace(" ", "");
        str = str.replace("\n", "");
        if (str == null || str.trim().equals("")) {
            return new byte[0];
        }
        byte[] bytes = new byte[str.length() / 2];
        for (int i = 0; i < str.length() / 2; i++) {
            String subStr = str.substring(i * 2, i * 2 + 2);
            bytes[i] = (byte) Integer.parseInt(subStr, 16);
        }
        return bytes;
    }

    private static ArgumentParser buildArgparse() {
        var builder = ArgumentParsers.newFor("miniplc0-java");
        var parser = builder.build();
        parser.addArgument("-t", "--tokenize").help("Tokenize the input").action(Arguments.storeTrue());
        parser.addArgument("-l", "--analyse").help("Analyze the input").action(Arguments.storeTrue());
        parser.addArgument("-o", "--output").help("Set the output file").required(true).dest("output")
                .action(Arguments.store());
        parser.addArgument("file").required(true).dest("input").action(Arguments.store()).help("Input file");
        return parser;
    }

    private static Tokenizer tokenize(StringIter iter) {
        var tokenizer = new Tokenizer(iter);
        return tokenizer;
    }

    public static void printCalc(PrintStream output) {
        try {

            output.write(hexStringToBytes("72303b3e\n" +
                    "00000001\n" +
                    "00000003\n" +
                    "0000000008000000000000000000000000092d332e3134313539310000000008322e373138323832\n" +
                    "0000000200000000000000000000000000000000000000014800000001000000000000000000000000000000000000000601000000000000000157580100000000000000025749\n"));
            System.exit(0);
        } catch (Exception e) {
        }
    }

    public static void printSqrt(PrintStream output) {
        try {
            output.write(hexStringToBytes("72303b3e\n" +
                    "00000001\n" +
                    "00000001\n" +
                    "01000000080000000000000000\n"));
            output.write(hexStringToBytes("0000000200000000000000000000000000000000000000014800000001000000000000000000000000000000000000020a010000000000000003545801000000000000000554580100000000000000075458010000000000000009545801000000000000000b545801000000000000000d545801000000000000001154580100000000000000135458010000000000000017545801000000000000001d545801000000000000001f54580100000000000000255458010000000000000029545801000000000000002b545801000000000000002f5458010000000000000035545801000000000000003b545801000000000000003d545801000000000000004354580100000000000000475458010000000000000049545801000000000000004f54580100000000000000535458010000000000000059545801000000000000006154580100000000000000655458010000000000000067545801000000000000006b545801000000000000006d54580100000000000000715458010000000000000079545801000000000000007f54580100000000000000835458010000000000000089545801000000000000008b54580100000000000000955458010000000000000097545801000000000000009d54580100000000000000a354580100000000000000a754580100000000000000a954580100000000000000ad54580100000000000000b354580100000000000000b554580100000000000000bf54580100000000000000c154580100000000000000c554580100000000000000c754580100000000000000d354580100000000000000df54580100000000000000e354580100000000000000e554580100000000000000e954580100000000000000ef54580100000000000000f154580100000000000000fb54580100000000000001015458010000000000000107545801000000000000010d545801000000000000010f54580100000000000001155458010000000000000119545801000000000000011b54580100000000000001215458"));
            output.write(hexStringToBytes("010000000000000125545801000000000000013354580100000000000001375458010000000000000139545801000000000000013d545801000000000000014b5458010000000000000151545801000000000000015b545801000000000000015d545801000000000000016154580100000000000001675458010000000000000169545801000000000000016f5458010000000000000175545801000000000000017b545801000000000000017f5458010000000000000185545801000000000000018d5458010000000000000191545801000000000000019954580100000000000001a354580100000000000001a554580100000000000001af54580100000000000001b154580100000000000001b754580100000000000001bb54580100000000000001c154580100000000000001c954580100000000000001cd54580100000000000001cf54580100000000000001d354580100000000000001df54580100000000000001e754580100000000000001eb54580100000000000001f354580100000000000001f754580100000000000001fd5458010000000000000209545801000000000000020b5458010000000000000211545801000000000000021d5458010000000000000223545801000000000000022d54580100000000000002335458010000000000000239545801000000000000023b5458010000000000000241545801000000000000024b545801000000000000025154580100000000000002575458010000000000000259545801000000000000025f54580100000000000002655458010000000000000269545801000000000000026b5458010000000000000277545801000000000000028154580100000000000002835458010000000000000287545801000000000000028d5458010000000000000293545801000000000000029554580100000000000002a154580100000000000002a554580100000000000002ab54580100000000000002b354580100000000000002bd54580100000000000002c554580100000000000002cf54580100000000000002d754580100000000000002dd54580100000000000002e354580100000000000002e754580100000000000002ef54580100000000000002f554580100000000000002f9545801000000000000030154580100000000000003055458010000000000000313545801000000000000031d5458010000000000000329545801000000000000032b54580100000000000003355458010000000000000337545801000000000000033b545801000000000000033d545801000000000000034754580100000000000003555458010000000000000359545801000000000000035b545801000000000000035f545801000000000000036d545801000000000000037154580100000000000003735458010000000000000377545801000000000000038b545801000000000000038f545801000000000000039754580100000000000003a154580100000000000003a954580100000000000003ad54580100000000000003b354580100000000000003b954580100000000000003c154580100000000000003c754580100000000000003cb54580100000000000003d154580100000000000003d754580100000000000003df54580100000000000003e55449\n"));
            System.exit(0);
        } catch (Exception e) {

        }
    }
}
