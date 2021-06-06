package com.modules;

import android.util.Log;

public class CRCChecker {
    private static final String TAG = "CRCChecker";
    //should use string, since 'int' can only be 32bits.
    private static String[]  array_divider = {"11001","111010101","1100000001111",
            "10001000000100001",
            "100000100110000010001110110110111"};
    private static int[] array_bits_divider = {5,8,12,16,32};
    private static String divider;
    private static int bits_divider;

    //We cannot choose the divider according to the length of input, since the input and output are divided.
    //default: 8 bit divider.
    public CRCChecker(){
        divider = array_divider[1];
        bits_divider = array_bits_divider[1];
    }

    public CRCChecker(int input_bit){
        if(input_bit<=5){
            divider = array_divider[0];
            bits_divider = array_bits_divider[0];
        }else if(input_bit <= 8){
            divider = array_divider[1];
            bits_divider = array_bits_divider[1];
        }else if(input_bit <= 12){
            divider = array_divider[2];
            bits_divider = array_bits_divider[2];
        }else if(input_bit <= 24){
            divider = array_divider[3];
            bits_divider = array_bits_divider[3];
        }else {
            divider = array_divider[4];
            bits_divider = array_bits_divider[4];
        }
    }

    private StringBuilder CRCRemainer(StringBuilder initial){
        StringBuilder tmpinit_b = new StringBuilder(initial);
        tmpinit_b = clearZero(tmpinit_b);
        Log.d(TAG,"Func: CRCRemainer, after specialized tmpinit_b: "+tmpinit_b.toString());
        int bits_tmpinit_b = tmpinit_b.length();

        while(bits_tmpinit_b >= bits_divider){
            StringBuilder high = new StringBuilder(tmpinit_b.substring(0,bits_divider));
            high = xorOfString(high);
            high.append(tmpinit_b.substring(bits_divider,bits_tmpinit_b));
            tmpinit_b = clearZero(high);
            bits_tmpinit_b = tmpinit_b.length();
            Log.d(TAG,"Func: CRCRemainer, in-while bits_tmpinit_b: "+tmpinit_b.toString());
        }
        return tmpinit_b;
    }

    //hope the string to be '0''1'
    public String AddCRCCode(String initial){
        if(!isInputValid(initial)){
            //TODO
            return "";
        }

        StringBuilder tmpinit_b = new StringBuilder(initial);
        for(int i = 0;i < bits_divider-1;++i){
            tmpinit_b.append('0');
        }
        tmpinit_b = CRCRemainer(tmpinit_b);
        tmpinit_b = addBitsToRemainer(tmpinit_b);
        StringBuilder final_string = new StringBuilder(initial);
        final_string.append(tmpinit_b);
        Log.d(TAG,"Func: AddCRCCode, final stringbuilder: "+final_string.toString());
        return final_string.toString();
    }

    public Boolean CheckCRCCode(String initial){
        if(!isInputValid(initial)){
            //TODO
            return false;
        }

        StringBuilder tmpinit_b = new StringBuilder(initial);
        tmpinit_b = CRCRemainer(tmpinit_b);
        if(tmpinit_b.length() != 0){
            Log.d(TAG,"Func: CheckCRCCode, Wrong result.");
            return false;
        }
        Log.d(TAG,"Func: CheckCRCCode, Pass CRC test.");
        return true;
    }

    //add the remainer to bit_divider-1 bits
    private StringBuilder addBitsToRemainer(StringBuilder input){
        StringBuilder tmp = new StringBuilder();
        for (int i = 0;i < (bits_divider - 1 - input.length());++i){
            tmp.append('0');
        }
        tmp.append(input);
        return tmp;
    }
    //clear all zeros of the string
    // TODO: here how to use the enpty value of StringBuilder? Is the new safe enough?
    private StringBuilder clearZero(StringBuilder input){
        int index_non_zero = -1;
        for(int i = 0;i < input.length();++i){
            if(input.charAt(i) == '1'){
                index_non_zero = i;
                break;
            }
        }
        if(index_non_zero == -1){
            Log.d(TAG,"Func: clearZero, string with all '0': "+input);
            return new StringBuilder("");
        }
        StringBuilder res = new StringBuilder(input.substring(index_non_zero,input.length()));
        return res;
    }
    //do xor
    private StringBuilder xorOfString(StringBuilder input){
        StringBuilder res = new StringBuilder();
        int len = input.length();
        if(len != bits_divider){
            Log.d(TAG,"Func: xorOfString, invalid string: "+input);
            return res;
        }
        for(int i = 0;i < len;++i){
            if(input.charAt(i)==divider.charAt(i)) res.append('0');
            else res.append('1');
        }
        return res;
    }
    //check the string only contains 0,1
    private boolean isInputValid(String input){
        int len = input.length();
        for (int i = 0;i < len;++i){
            char tmp = input.charAt(i);
            if(tmp != '0'&& tmp != '1'){
                Log.d(TAG,"Func: isInputValid, invalid string: "+input);
                return false;
            }
        }
        return true;
    }
}
